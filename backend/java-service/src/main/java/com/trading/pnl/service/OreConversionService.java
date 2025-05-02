package com.trading.pnl.service;

import com.trading.pnl.model.OreMarketDataItem;
import com.trading.pnl.util.TickerMappingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OreConversionService {
    private static final Logger logger = LoggerFactory.getLogger(OreConversionService.class);
    private static final String TARGET_TICKER = "USSO10 Curncy";

    public List<OreMarketDataItem> convertToOreFormat(Map<String, String> marketData, String date) {
        List<OreMarketDataItem> oreData = new ArrayList<>();

        logger.info("Starting conversion of {} market data items", marketData.size());

        // 检查目标ticker是否存在
        if (marketData.containsKey(TARGET_TICKER)) {
            logger.info("Found target ticker '{}' in input data with value: {}",
                    TARGET_TICKER, marketData.get(TARGET_TICKER));
        } else {
            logger.info("Target ticker '{}' not found in input data", TARGET_TICKER);
            logger.info("Available tickers: {}", marketData.keySet());
        }

        for (Map.Entry<String, String> entry : marketData.entrySet()) {
            String bbgTicker = entry.getKey();
            String value = entry.getValue();

            // 获取ORE Ticker和系数，如果映射不存在则返回null
            Map<String, Object> mapping = TickerMappingUtil.getOreTickerAndCoefficient(bbgTicker);
            if (mapping == null) {
                continue; // 跳过没有映射关系的ticker
            }

            String oreTicker = (String) mapping.get("target");
            Object coefficientObj = mapping.get("coefficient");
            double coefficient;

            // 处理系数可能是Integer或Double的情况
            if (coefficientObj instanceof Integer) {
                coefficient = ((Integer) coefficientObj).doubleValue();
            } else if (coefficientObj instanceof Double) {
                coefficient = (Double) coefficientObj;
            } else {
                logger.warn("Invalid coefficient type for ticker {}: {}", bbgTicker, coefficientObj);
                continue;
            }

            // 应用系数转换
            String convertedValue;
            try {
                double numericValue = Double.parseDouble(value);
                convertedValue = String.format("%.4f", numericValue * coefficient);
            } catch (NumberFormatException e) {
                logger.warn("Invalid numeric value for ticker {}: {}", bbgTicker, value);
                convertedValue = value; // 保持原值不变
            }

            OreMarketDataItem item = new OreMarketDataItem();
            item.setBbgTicker(bbgTicker);
            item.setOreTicker(oreTicker);
            item.setValue(convertedValue);
            item.setDate(date);
            oreData.add(item);
        }

        logger.info("Conversion completed. {} items converted out of {} total items",
                oreData.size(), marketData.size());

        return oreData;
    }

    public void saveToOreFormat(List<OreMarketDataItem> data, File outputFile) throws IOException {
        // 确保输出目录存在
        File outputDir = outputFile.getParentFile();
        if (!outputDir.exists()) {
            outputDir.mkdirs();
            logger.info("Created output directory: {}", outputDir.getAbsolutePath());
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            // 直接写入数据，不写入标题
            for (OreMarketDataItem item : data) {
                writer.write(String.format("%s %s %s\n",
                        item.getDate(),
                        item.getOreTicker(),
                        item.getValue()));
            }
        }
        logger.info("Successfully saved {} ORE format data records to {}",
                data.size(), outputFile.getAbsolutePath());
    }

    public String generateOreFileName(String date) {
        return String.format("ORE_Market_%s.txt", date.replace("-", ""));
    }
}