package com.weaving.llm.common.utils.converter;

import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DOCX 文档转换器
 * 将 Word 文档转换为 Markdown 格式，支持图片提取和位置记录
 */
@Component
public class DocxConverter implements DocumentConverter {
    
    private static final String[] SUPPORTED_EXTENSIONS = {".docx"};
    
    private static final Map<String, String> IMAGE_TYPE_MAP = new HashMap<>();
    static {
        IMAGE_TYPE_MAP.put("image/png", "png");
        IMAGE_TYPE_MAP.put("image/jpeg", "jpg");
        IMAGE_TYPE_MAP.put("image/jpg", "jpg");
        IMAGE_TYPE_MAP.put("image/gif", "gif");
        IMAGE_TYPE_MAP.put("image/bmp", "bmp");
        IMAGE_TYPE_MAP.put("image/tiff", "tiff");
    }
    
    @Override
    public String getSupportedType() {
        return "docx";
    }
    
    @Override
    public boolean supports(String fileName) {
        if (fileName == null) return false;
        String lowerName = fileName.toLowerCase();
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (lowerName.endsWith(ext)) return true;
        }
        return false;
    }
    
    @Override
    public ConversionResult convert(File file) {
        try (InputStream is = new FileInputStream(file)) {
            return convert(is, file.getName());
        } catch (IOException e) {
            return ConversionResult.failure(file.getName(), "读取文件失败: " + e.getMessage());
        }
    }
    
    @Override
    public ConversionResult convert(InputStream inputStream, String fileName) {
        try {
            StringBuilder markdown = new StringBuilder();
            List<ImageInfo> extractedImages = new ArrayList<>();
            
            try (XWPFDocument document = new XWPFDocument(inputStream)) {
                AtomicInteger imageCounter = new AtomicInteger(0);
                AtomicInteger globalCharPosition = new AtomicInteger(0);
                
                // 先收集所有段落文本
                List<String> allParagraphTexts = new ArrayList<>();
                for (IBodyElement element : document.getBodyElements()) {
                    if (element instanceof XWPFParagraph) {
                        allParagraphTexts.add(((XWPFParagraph) element).getText());
                    } else if (element instanceof XWPFTable) {
                        allParagraphTexts.add(getTableText((XWPFTable) element));
                    }
                }
                
                // 遍历处理
                int paragraphIndex = 0;
                for (IBodyElement element : document.getBodyElements()) {
                    if (element instanceof XWPFParagraph) {
                        processParagraph((XWPFParagraph) element, markdown, extractedImages, 
                                imageCounter, paragraphIndex, globalCharPosition, allParagraphTexts);
                        paragraphIndex++;
                    } else if (element instanceof XWPFTable) {
                        processTable((XWPFTable) element, markdown);
                        globalCharPosition.addAndGet(getTableText((XWPFTable) element).length());
                        paragraphIndex++;
                    }
                }
                
                String title = document.getProperties() != null &&
                        document.getProperties().getCoreProperties() != null ?
                        document.getProperties().getCoreProperties().getTitle() : null;
                
                String content = markdown.toString().trim();
                ConversionResult result = ConversionResult.success(content, fileName, "docx");
                
                if (title != null && !title.isEmpty()) {
                    result.addMetadata("title", title);
                }
                
                result.setImages(extractedImages);
                result.addMetadata("imageCount", extractedImages.size());
                
                return result;
            }
        } catch (Exception e) {
            return ConversionResult.failure(fileName, "转换 DOCX 失败: " + e.getMessage());
        }
    }
    
    private void processParagraph(XWPFParagraph paragraph, StringBuilder markdown,
                                   List<ImageInfo> images, AtomicInteger imageCounter,
                                   int paragraphIndex, AtomicInteger globalCharPosition,
                                   List<String> allParagraphTexts) {
        String paraText = paragraph.getText();
        int currentCharPos = globalCharPosition.get();
        
        // 获取前后文本
        String precedingText = paragraphIndex > 0 ? allParagraphTexts.get(paragraphIndex - 1) : "";
        String followingText = paragraphIndex < allParagraphTexts.size() - 1 ? 
                allParagraphTexts.get(paragraphIndex + 1) : "";
        
        // 截取适当长度
        if (precedingText != null && precedingText.length() > 200) {
            precedingText = "..." + precedingText.substring(precedingText.length() - 200);
        }
        if (followingText != null && followingText.length() > 200) {
            followingText = followingText.substring(0, 200) + "...";
        }
        
        // 提取图片
        for (XWPFRun run : paragraph.getRuns()) {
            for (XWPFPicture picture : run.getEmbeddedPictures()) {
                try {
                    XWPFPictureData pictureData = picture.getPictureData();
                    byte[] data = pictureData.getData();
                    String mimeType = pictureData.getPackagePart().getContentType();
                    String format = IMAGE_TYPE_MAP.getOrDefault(mimeType, "png");
                    
                    int index = imageCounter.incrementAndGet();
                    String imageFileName = "image_" + index + "." + format;
                    
                    ImageInfo imageInfo = ImageInfo.builder()
                            .fileName(imageFileName)
                            .data(data)
                            .format(format)
                            .mimeType(mimeType)
                            .index(index)
                            .paragraphIndex(paragraphIndex)
                            .charPosition(currentCharPos)
                            .precedingText(precedingText)
                            .followingText(followingText)
                            .paragraphText(paraText)
                            .build();
                    
                    images.add(imageInfo);
                    
                    // Markdown 中插入图片引用和位置标记
                    markdown.append("\n[IMAGE_REF:").append(index).append("]\n");
                    markdown.append("![图片").append(index).append("](images/").append(imageFileName).append(")\n");
                    if (precedingText != null && !precedingText.trim().isEmpty()) {
                        markdown.append("<!-- 位于: \"").append(escapeText(precedingText.trim())).append("\" 之后 -->\n");
                    }
                    markdown.append("\n");
                    
                } catch (Exception ignored) {}
            }
        }
        
        if (paraText == null || paraText.trim().isEmpty()) {
            return;
        }
        
        String style = paragraph.getStyle();
        
        if (style != null) {
            String headingText = formatHeading(paraText, style);
            if (headingText != null) {
                markdown.append(headingText);
                globalCharPosition.addAndGet(paraText.length());
                return;
            }
        }
        
        if (paragraph.getNumFmt() != null) {
            markdown.append("- ").append(paraText).append("\n\n");
            globalCharPosition.addAndGet(paraText.length() + 2);
            return;
        }
        
        // 处理普通段落
        StringBuilder formatted = new StringBuilder();
        for (XWPFRun run : paragraph.getRuns()) {
            String runText = run.getText(0);
            if (runText == null) continue;
            
            if (run.isBold() && run.isItalic()) {
                formatted.append("***").append(runText).append("***");
            } else if (run.isBold()) {
                formatted.append("**").append(runText).append("**");
            } else if (run.isItalic()) {
                formatted.append("*").append(runText).append("*");
            } else {
                formatted.append(runText);
            }
        }
        
        if (formatted.length() > 0) {
            markdown.append(formatted).append("\n\n");
            globalCharPosition.addAndGet(formatted.length() + 2);
        }
    }
    
    private String formatHeading(String text, String style) {
        if (style.contains("Heading1") || style.equals("1")) return "# " + text + "\n\n";
        if (style.contains("Heading2") || style.equals("2")) return "## " + text + "\n\n";
        if (style.contains("Heading3") || style.equals("3")) return "### " + text + "\n\n";
        if (style.contains("Heading4") || style.equals("4")) return "#### " + text + "\n\n";
        if (style.contains("Heading5") || style.equals("5")) return "##### " + text + "\n\n";
        if (style.contains("Heading6") || style.equals("6")) return "###### " + text + "\n\n";
        return null;
    }
    
    private String getTableText(XWPFTable table) {
        StringBuilder sb = new StringBuilder();
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                sb.append(cell.getText()).append("\t");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
    
    private void processTable(XWPFTable table, StringBuilder markdown) {
        List<XWPFTableRow> rows = table.getRows();
        if (rows.isEmpty()) return;
        
        XWPFTableRow headerRow = rows.get(0);
        List<XWPFTableCell> headerCells = headerRow.getTableCells();
        
        markdown.append("|");
        for (XWPFTableCell cell : headerCells) {
            markdown.append(" ").append(cell.getText().replace("\n", " ")).append(" |");
        }
        markdown.append("\n|");
        for (int i = 0; i < headerCells.size(); i++) {
            markdown.append(" --- |");
        }
        markdown.append("\n");
        
        for (int i = 1; i < rows.size(); i++) {
            markdown.append("|");
            for (XWPFTableCell cell : rows.get(i).getTableCells()) {
                markdown.append(" ").append(cell.getText().replace("\n", " ")).append(" |");
            }
            markdown.append("\n");
        }
        markdown.append("\n");
    }
    
    private String escapeText(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\n", " ");
    }
    
    @Override
    public String extractText(File file) {
        try (InputStream is = new FileInputStream(file)) {
            return extractText(is, file.getName());
        } catch (IOException e) {
            return "";
        }
    }
    
    @Override
    public String extractText(InputStream inputStream, String fileName) {
        try {
            StringBuilder text = new StringBuilder();
            try (XWPFDocument document = new XWPFDocument(inputStream)) {
                for (IBodyElement element : document.getBodyElements()) {
                    if (element instanceof XWPFParagraph) {
                        String t = ((XWPFParagraph) element).getText();
                        if (t != null && !t.trim().isEmpty()) text.append(t).append("\n");
                    } else if (element instanceof XWPFTable) {
                        for (XWPFTableRow row : ((XWPFTable) element).getRows()) {
                            for (XWPFTableCell cell : row.getTableCells()) {
                                text.append(cell.getText()).append("\t");
                            }
                            text.append("\n");
                        }
                    }
                }
            }
            return text.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }
}
