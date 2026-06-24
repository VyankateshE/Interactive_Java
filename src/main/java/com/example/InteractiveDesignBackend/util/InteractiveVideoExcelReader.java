package com.example.InteractiveDesignBackend.util;

import org.apache.poi.ss.usermodel.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class InteractiveVideoExcelReader {

    public static Map<String, List<Map<String, Object>>> readFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new RuntimeException("File name is missing.");
        }

        if (filename.endsWith(".xlsx")) {
            return readExcel(file);
        } else if (filename.endsWith(".csv")) {
            return readCSV(file);
        } else {
            throw new RuntimeException("Unsupported file type. Only .xlsx and .csv are allowed.");
        }
    }

    public static Map<String, List<Map<String, Object>>> readCSV(MultipartFile file) {
        Map<String, List<Map<String, Object>>> data = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            List<String> headers = new ArrayList<>();
            List<Map<String, Object>> rows = new ArrayList<>();

            boolean isFirstRow = true;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");

              
                boolean isBlank = Arrays.stream(values).allMatch(String::isBlank);
                if (isBlank || values.length == 0) continue;

                if (isFirstRow) {
                    headers.addAll(Arrays.asList(values));
                    isFirstRow = false;
                } else {
                    Map<String, Object> rowData = new LinkedHashMap<>();

                    for (int i = 0; i < headers.size(); i++) {
                        String columnName = headers.get(i).trim();
                        String value = (i < values.length) ? values[i].trim() : "";
                        Object parsedValue = handleDateTime(columnName, value);
                        rowData.put(columnName, parsedValue);
                    }

                    rows.add(rowData);
                }
            }

            String tableName = file.getOriginalFilename().substring(0, file.getOriginalFilename().lastIndexOf('.')).toLowerCase();
            data.put(tableName, rows);

        } catch (Exception e) {
            throw new RuntimeException("Error reading CSV file: " + e.getMessage());
        }

        return data;
    }

    public static Map<String, List<Map<String, Object>>> readExcel(MultipartFile file) {
        Map<String, List<Map<String, Object>>> data = new HashMap<>();

        try (InputStream is = file.getInputStream()) {
            Workbook workbook = WorkbookFactory.create(is);
            Sheet sheet = workbook.getSheetAt(0);

            Iterator<Row> rowIterator = sheet.iterator();

            List<String> headers = new ArrayList<>();
            List<Map<String, Object>> rows = new ArrayList<>();

            if (rowIterator.hasNext()) {
                Row headerRow = rowIterator.next();
                headerRow.forEach(cell -> headers.add(cell.getStringCellValue().trim()));
            }

            while (rowIterator.hasNext()) {
                Row currentRow = rowIterator.next();

                // ✅ Skip blank Excel row
                if (isRowCompletelyEmpty(currentRow)) continue;

                Map<String, Object> rowData = new LinkedHashMap<>();

                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = currentRow.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    String columnName = headers.get(i);

                    Object parsedValue;
                    switch (cell.getCellType()) {
                        case STRING:
                            parsedValue = handleDateTime(columnName, cell.getStringCellValue().trim());
                            break;
                        case NUMERIC:
                            if (DateUtil.isCellDateFormatted(cell)) {
                                Date date = cell.getDateCellValue();
                                parsedValue = new java.sql.Timestamp(date.getTime());
                            } else {
                                parsedValue = cell.getNumericCellValue();
                            }
                            break;
                        case BOOLEAN:
                            parsedValue = cell.getBooleanCellValue();
                            break;
                        case BLANK:
                            parsedValue = null;
                            break;
                        default:
                            parsedValue = cell.toString().trim();
                    }

                    rowData.put(columnName, parsedValue);
                }

                rows.add(rowData);
            }

            String tableName = file.getOriginalFilename().substring(0, file.getOriginalFilename().lastIndexOf('.')).toLowerCase();
            data.put(tableName, rows);

        } catch (Exception e) {
            throw new RuntimeException("Error reading Excel file: " + e.getMessage());
        }

        return data;
    }

    // ✅ Helper to check if a row is completely empty (for Excel)
    private static boolean isRowCompletelyEmpty(Row row) {
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK && !cell.toString().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static Object handleDateTime(String columnName, String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            if (value.contains("T")) {
                value = value.replace("Z", "");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                Date date = sdf.parse(value);
                return new java.sql.Timestamp(date.getTime());
            }

            if (value.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = sdf.parse(value);
                return new java.sql.Timestamp(date.getTime());
            }

            if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date date = sdf.parse(value);
                return new java.sql.Date(date.getTime());
            }

        } catch (ParseException e) {
            return value;
        }

        return value;
    }
}