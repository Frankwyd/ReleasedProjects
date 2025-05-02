package com.pms.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/market-data")
@CrossOrigin(origins = "*")
public class MarketDataController {
    private static final Logger logger = LoggerFactory.getLogger(MarketDataController.class);

    @Data
    public static class LoadDataRequest {
        private String basePath;
        private String tm1File;
        private String currentFile;
    }

    @Data
    public static class MarketDataItem {
        private String ticker;
        private double tm1Value;
        private double currentValue;
    }

    @PostMapping("/load")
    public ResponseEntity<?> loadMarketData(@RequestBody LoadDataRequest request) {
        logger.info("Received request to load market data. Base path: {}, TM1 file: {}, Current file: {}",
                request.getBasePath(), request.getTm1File(), request.getCurrentFile());

        try {
            // 读取TM1文件数据
            Map<String, Double> tm1Data = readExcelData(new File(request.getBasePath(), request.getTm1File()));
            // 读取当前文件数据
            Map<String, Double> currentData = readExcelData(new File(request.getBasePath(), request.getCurrentFile()));

            // 合并数据
            List<MarketDataItem> result = new ArrayList<>();
            for (String ticker : tm1Data.keySet()) {
                if (currentData.containsKey(ticker)) {
                    MarketDataItem item = new MarketDataItem();
                    item.setTicker(ticker);
                    item.setTm1Value(tm1Data.get(ticker));
                    item.setCurrentValue(currentData.get(ticker));
                    result.add(item);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", result);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error loading market data", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    private Map<String, Double> readExcelData(File file) throws Exception {
        Map<String, Double> data = new HashMap<>();
        try (FileInputStream fis = new FileInputStream(file);
                Workbook workbook = new HSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0); // 获取第一个sheet
            int startRow = 3; // 从第4行开始读取

            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    Cell tickerCell = row.getCell(0);
                    Cell valueCell = row.getCell(1);

                    if (tickerCell != null && valueCell != null) {
                        String ticker = tickerCell.getStringCellValue();
                        double value = valueCell.getNumericCellValue();
                        if (!ticker.trim().isEmpty()) {
                            data.put(ticker, value);
                        }
                    }
                }
            }
        }
        return data;
    }
}