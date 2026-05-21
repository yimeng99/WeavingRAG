package com.weaving.llm.common.utils.converter;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PDF 文档转换器
 * 将 PDF 文档转换为 Markdown 格式，支持图片提取和位置记录
 */
@Component
public class PdfConverter implements DocumentConverter {
    
    private static final String[] SUPPORTED_EXTENSIONS = {".pdf"};
    
    @Override
    public String getSupportedType() {
        return "pdf";
    }
    
    @Override
    public boolean supports(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lowerName = fileName.toLowerCase();
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (lowerName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public ConversionResult convert(File file) {
        try {
            return convertWithFile(file);
        } catch (Exception e) {
            return ConversionResult.failure(file.getName(), "转换 PDF 失败: " + e.getMessage());
        }
    }
    
    @Override
    public ConversionResult convert(InputStream inputStream, String fileName) {
        try {
            File tempFile = File.createTempFile("pdf_convert_", ".pdf");
            try {
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
                return convertWithFile(tempFile);
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete();
                }
            }
        } catch (Exception e) {
            return ConversionResult.failure(fileName, "转换 PDF 失败: " + e.getMessage());
        }
    }
    
    private ConversionResult convertWithFile(File file) throws Exception {
        try (PDDocument document = Loader.loadPDF(file)) {
            StringBuilder markdown = new StringBuilder();
            List<ImageInfo> extractedImages = new ArrayList<>();
            
            PDDocumentInformation info = document.getDocumentInformation();
            String title = info != null ? info.getTitle() : null;
            String author = info != null ? info.getAuthor() : null;
            String subject = info != null ? info.getSubject() : null;
            
            int totalPages = document.getNumberOfPages();
            
            // 先提取所有页面的文本
            List<String> pageTexts = new ArrayList<>();
            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setSortByPosition(true);
            
            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                textStripper.setStartPage(pageNum);
                textStripper.setEndPage(pageNum);
                pageTexts.add(textStripper.getText(document));
            }
            
            // 提取图片，附带位置信息
            extractImagesWithPosition(document, extractedImages, pageTexts);
            
            // 按页处理文本
            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                String pageText = pageTexts.get(pageNum - 1);
                
                if (totalPages > 1) {
                    markdown.append("\n---\n\n");
                    markdown.append("## 第 ").append(pageNum).append(" 页\n\n");
                }
                
                String markdownContent = convertToMarkdown(pageText);
                markdown.append(markdownContent);
                
                // 添加该页的图片引用
                List<ImageInfo> pageImages = getImagesForPage(extractedImages, pageNum);
                for (ImageInfo imageInfo : pageImages) {
                    markdown.append("\n[IMAGE_REF:").append(imageInfo.getIndex()).append("]\n");
                    markdown.append("![图片").append(imageInfo.getIndex()).append("](images/")
                           .append(imageInfo.getFileName()).append(")\n\n");
                    
                    // 如果有位置信息，添加注释
                    if (imageInfo.getPrecedingText() != null && !imageInfo.getPrecedingText().isEmpty()) {
                        markdown.append("<!-- 图片位置：位于 \"")
                               .append(escapeHtml(imageInfo.getPrecedingText().trim()))
                               .append("\" 之后 -->\n\n");
                    }
                }
            }
            
            String content = markdown.toString().trim();
            ConversionResult result = ConversionResult.success(content, file.getName(), "pdf");
            result.setTotalPages(totalPages);
            
            if (title != null && !title.isEmpty()) {
                result.addMetadata("title", title);
            }
            if (author != null && !author.isEmpty()) {
                result.addMetadata("author", author);
            }
            if (subject != null && !subject.isEmpty()) {
                result.addMetadata("subject", subject);
            }
            result.addMetadata("totalPages", totalPages);
            
            result.setImages(extractedImages);
            result.addMetadata("imageCount", extractedImages.size());
            
            return result;
        }
    }
    
    /**
     * 提取图片并记录位置信息
     */
    private void extractImagesWithPosition(PDDocument document, List<ImageInfo> images, 
                                            List<String> pageTexts) throws Exception {
        AtomicInteger imageCounter = new AtomicInteger(0);
        
        for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {
            PDPage page = document.getPage(pageNum);
            String pageText = pageTexts.get(pageNum);
            
            // 获取前后页文本
            String precedingPageText = pageNum > 0 ? pageTexts.get(pageNum - 1) : "";
            String followingPageText = pageNum < pageTexts.size() - 1 ? pageTexts.get(pageNum + 1) : "";
            
            ImageExtractorWithPosition extractor = new ImageExtractorWithPosition(
                    imageCounter, images, pageNum + 1, pageText, precedingPageText, followingPageText);
            extractor.processPage(page);
        }
    }
    
    /**
     * 带位置信息的图片提取器
     */
    private static class ImageExtractorWithPosition extends PDFStreamEngine {
        private final AtomicInteger counter;
        private final List<ImageInfo> images;
        private final int pageNumber;
        private final String pageText;
        private final String precedingPageText;
        private final String followingPageText;
        
        public ImageExtractorWithPosition(AtomicInteger counter, List<ImageInfo> images, 
                                          int pageNumber, String pageText,
                                          String precedingPageText, String followingPageText) {
            this.counter = counter;
            this.images = images;
            this.pageNumber = pageNumber;
            this.pageText = pageText;
            this.precedingPageText = precedingPageText;
            this.followingPageText = followingPageText;
        }
        
        @Override
        protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
            String operation = operator.getName();
            
            if ("Do".equals(operation)) {
                COSBase object = operands.get(0);
                if (object instanceof COSName) {
                    COSName cosName = (COSName) object;
                    PDXObject xobject = getResources().getXObject(cosName);
                    
                    if (xobject instanceof PDImageXObject) {
                        PDImageXObject image = (PDImageXObject) xobject;
                        try {
                            BufferedImage bufferedImage = image.getImage();
                            
                            int index = counter.incrementAndGet();
                            String format = "png";
                            String fileName = "image_" + index + "." + format;
                            
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ImageIO.write(bufferedImage, format, baos);
                            byte[] data = baos.toByteArray();
                            
                            // 获取图片前的文本（当前页前200字符，或前页末尾）
                            String precedingText = getPrecedingText();
                            String followingText = getFollowingText();
                            
                            ImageInfo imageInfo = ImageInfo.builder()
                                    .fileName(fileName)
                                    .data(data)
                                    .format(format)
                                    .mimeType("image/png")
                                    .index(index)
                                    .pageNumber(pageNumber)
                                    .width(bufferedImage.getWidth())
                                    .height(bufferedImage.getHeight())
                                    .precedingText(precedingText)
                                    .followingText(followingText)
                                    .paragraphText(pageText.length() > 500 ? 
                                            pageText.substring(0, 500) + "..." : pageText)
                                    .build();
                            
                            images.add(imageInfo);
                        } catch (Exception e) {
                            // 单个图片提取失败不影响整体处理
                        }
                    }
                }
            }
            
            super.processOperator(operator, operands);
        }
        
        private String getPrecedingText() {
            // 优先取前页末尾内容
            if (precedingPageText != null && !precedingPageText.isEmpty()) {
                String trimmed = precedingPageText.trim();
                if (trimmed.length() > 200) {
                    return "... " + trimmed.substring(trimmed.length() - 200);
                }
                return trimmed;
            }
            return "";
        }
        
        private String getFollowingText() {
            if (followingPageText != null && !followingPageText.isEmpty()) {
                String trimmed = followingPageText.trim();
                if (trimmed.length() > 200) {
                    return trimmed.substring(0, 200) + " ...";
                }
                return trimmed;
            }
            return "";
        }
    }
    
    private List<ImageInfo> getImagesForPage(List<ImageInfo> images, int pageNumber) {
        List<ImageInfo> pageImages = new ArrayList<>();
        for (ImageInfo image : images) {
            if (image.getPageNumber() != null && image.getPageNumber() == pageNumber) {
                pageImages.add(image);
            }
        }
        return pageImages;
    }
    
    private String convertToMarkdown(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        StringBuilder markdown = new StringBuilder();
        String[] lines = text.split("\n");
        
        StringBuilder currentParagraph = new StringBuilder();
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            if (trimmedLine.isEmpty()) {
                if (currentParagraph.length() > 0) {
                    String paragraph = currentParagraph.toString().trim();
                    if (!paragraph.isEmpty()) {
                        String formatted = formatLine(paragraph);
                        markdown.append(formatted).append("\n\n");
                    }
                    currentParagraph = new StringBuilder();
                }
                continue;
            }
            
            if (currentParagraph.length() > 0) {
                currentParagraph.append(" ");
            }
            currentParagraph.append(trimmedLine);
        }
        
        if (currentParagraph.length() > 0) {
            String paragraph = currentParagraph.toString().trim();
            if (!paragraph.isEmpty()) {
                String formatted = formatLine(paragraph);
                markdown.append(formatted).append("\n");
            }
        }
        
        return markdown.toString();
    }
    
    private String formatLine(String line) {
        if (line == null || line.isEmpty()) {
            return "";
        }
        
        // 检测数字编号标题
        if (line.matches("^[0-9]+\\.[0-9]*\\s+.{2,50}$") || 
            line.matches("^[0-9]+\\s+.{2,50}$") ||
            line.matches("^第[一二三四五六七八九十]+[章节篇].{0,50}$")) {
            
            if (line.matches("^[0-9]+\\s+.{2,50}$") || 
                line.matches("^第[一二三四五六七八九十]+[章节篇].{0,50}$")) {
                return "## " + line;
            } else if (line.matches("^[0-9]+\\.[0-9]+\\s+.{2,50}$")) {
                return "### " + line;
            }
        }
        
        // 短行全大写可能是标题
        if (line.length() < 50 && line.toUpperCase().equals(line) && line.matches(".*[A-Z].*")) {
            return "## " + line;
        }
        
        return line;
    }
    
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("\n", " ");
    }
    
    @Override
    public String extractText(File file) {
        try {
            return extractTextFromFile(file);
        } catch (Exception e) {
            return "";
        }
    }
    
    @Override
    public String extractText(InputStream inputStream, String fileName) {
        try {
            File tempFile = File.createTempFile("pdf_extract_", ".pdf");
            try {
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
                return extractTextFromFile(tempFile);
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete();
                }
            }
        } catch (Exception e) {
            return "";
        }
    }
    
    private String extractTextFromFile(File file) throws Exception {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }
}
