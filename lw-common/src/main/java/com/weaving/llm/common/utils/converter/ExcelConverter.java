package com.weaving.llm.common.utils.converter;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookType;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Excel 文档转换器
 * 将 Excel 文档转换为 Markdown 表格格式
 */
@Component
public class ExcelConverter implements DocumentConverter {
    
    private static final String[] SUPPORTED_EXTENSIONS = {".xlsx", ".xls"};
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#.##");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    
    @Override
    public String getSupportedType() {
        return "excel";
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
            int totalSheets = 0;
            int totalRows = 0;
            
            // 根据文件扩展名选择合适的 Workbook 类型
            Workbook workbook = createWorkbook(inputStream, fileName);
            
            if (workbook == null) {
                return ConversionResult.failure(fileName, "无法识别的 Excel 格式");
            }
            
            try {
                int numberOfSheets = workbook.getNumberOfSheets();
                totalSheets = numberOfSheets;
                
                for (int i = 0; i < numberOfSheets; i++) {
                    Sheet sheet = workbook.getSheetAt(i);
                    String sheetName = sheet.getSheetName();
                    
                    // 添加工作表标题
                    if (numberOfSheets > 1) {
                        markdown.append("## ").append(sheetName).append("\n\n");
                    }
                    
                    int sheetRows = processSheet(sheet, markdown);
                    totalRows += sheetRows;
                    
                    if (i < numberOfSheets - 1) {
                        markdown.append("\n---\n\n");
                    }
                }
            } finally {
                workbook.close();
            }
            
            String content = markdown.toString().trim();
            ConversionResult result = ConversionResult.success(content, fileName, 
                    fileName.toLowerCase().endsWith(".xlsx") ? "xlsx" : "xls");
            
            result.addMetadata("totalSheets", totalSheets);
            result.addMetadata("totalRows", totalRows);
            
            return result;
        } catch (Exception e) {
            return ConversionResult.failure(fileName, "转换 Excel 失败: " + e.getMessage());
        }
    }
    
    private Workbook createWorkbook(InputStream inputStream, String fileName) throws IOException {
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".xlsx")) {
            return new XSSFWorkbook(inputStream);
        } else if (lowerName.endsWith(".xls")) {
            return new HSSFWorkbook(inputStream);
        }
        return null;
    }
    
    private int processSheet(Sheet sheet, StringBuilder markdown) {
        int rowCount = 0;
        int firstRowNum = sheet.getFirstRowNum();
        int lastRowNum = sheet.getLastRowNum();
        
        if (lastRowNum < 0) {
            return 0;
        }
        
        // 获取最大列数
        int maxColNum = 0;
        for (int i = firstRowNum; i <= lastRowNum; i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                int lastCell = row.getLastCellNum();
                if (lastCell > maxColNum) {
                    maxColNum = lastCell;
                }
            }
        }
        
        if (maxColNum == 0) {
            return 0;
        }
        
        // 处理表头（第一行）
        Row headerRow = sheet.getRow(firstRowNum);
        if (headerRow != null) {
            markdown.append("|");
            for (int j = 0; j < maxColNum; j++) {
                Cell cell = headerRow.getCell(j);
                markdown.append(" ").append(getCellValue(cell)).append(" |");
            }
            markdown.append("\n");
            
            // 添加分隔线
            markdown.append("|");
            for (int j = 0; j < maxColNum; j++) {
                markdown.append(" --- |");
            }
            markdown.append("\n");
            
            rowCount++;
        } else {
            // 如果没有表头行，创建默认表头
            markdown.append("|");
            for (int j = 0; j < maxColNum; j++) {
                markdown.append(" 列").append(j + 1).append(" |");
            }
            markdown.append("\n");
            
            markdown.append("|");
            for (int j = 0; j < maxColNum; j++) {
                markdown.append(" --- |");
            }
            markdown.append("\n");
        }
        
        // 处理数据行
        for (int i = firstRowNum + 1; i <= lastRowNum; i++) {
            Row row = sheet.getRow(i);
            markdown.append("|");
            for (int j = 0; j < maxColNum; j++) {
                Cell cell = row != null ? row.getCell(j) : null;
                markdown.append(" ").append(getCellValue(cell)).append(" |");
            }
            markdown.append("\n");
            rowCount++;
        }
        
        markdown.append("\n");
        return rowCount;
    }
    
    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        try {
            CellType cellType = cell.getCellType();
            
            switch (cellType) {
                case STRING:
                    return cell.getStringCellValue().replace("\n", " ").replace("|", "\\|");
                    
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        Date date = cell.getDateCellValue();
                        return DATE_FORMAT.format(date);
                    } else {
                        double numValue = cell.getNumericCellValue();
                        return NUMBER_FORMAT.format(numValue);
                    }
                    
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                    
                case FORMULA:
                    // 尝试获取公式计算结果
                    try {
                        CellType resultType = cell.getCachedFormulaResultType();
                        if (resultType == CellType.NUMERIC) {
                            return NUMBER_FORMAT.format(cell.getNumericCellValue());
                        } else if (resultType == CellType.STRING) {
                            return cell.getStringCellValue();
                        } else if (resultType == CellType.BOOLEAN) {
                            return String.valueOf(cell.getBooleanCellValue());
                        }
                    } catch (Exception e) {
                        // 如果无法获取计算结果，返回公式本身
                        return cell.getCellFormula();
                    }
                    return "";
                    
                case BLANK:
                    return "";
                    
                default:
                    return "";
            }
        } catch (Exception e) {
            return "";
        }
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
            
            Workbook workbook = createWorkbook(inputStream, fileName);
            if (workbook == null) {
                return "";
            }
            
            try {
                int numberOfSheets = workbook.getNumberOfSheets();
                
                for (int i = 0; i < numberOfSheets; i++) {
                    Sheet sheet = workbook.getSheetAt(i);
                    
                    if (numberOfSheets > 1) {
                        text.append("【").append(sheet.getSheetName()).append("】\n");
                    }
                    
                    for (Row row : sheet) {
                        StringBuilder rowText = new StringBuilder();
                        for (Cell cell : row) {
                            String cellValue = getCellValue(cell);
                            if (!cellValue.isEmpty()) {
                                rowText.append(cellValue).append("\t");
                            }
                        }
                        if (rowText.length() > 0) {
                            text.append(rowText.toString().trim()).append("\n");
                        }
                    }
                    
                    if (i < numberOfSheets - 1) {
                        text.append("\n");
                    }
                }
            } finally {
                workbook.close();
            }
            
            return text.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }
}
