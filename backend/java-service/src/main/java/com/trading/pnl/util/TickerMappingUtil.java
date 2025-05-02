package com.trading.pnl.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class TickerMappingUtil {
    private static final Logger logger = LoggerFactory.getLogger(TickerMappingUtil.class);
    private static final String MAPPING_FILE_PATH = "config/ticker_mapping.json";
    private static Map<String, Map<String, Object>> bbgToOreMap = new HashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String TARGET_TICKER = "USSO10 Curncy";

    static {
        loadMapping();
    }

    private static void loadMapping() {
        try {
            // 使用ClassPathResource来加载资源文件
            ClassPathResource resource = new ClassPathResource(MAPPING_FILE_PATH);
            logger.info("Loading mapping file from classpath: {}", MAPPING_FILE_PATH);

            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    bbgToOreMap = objectMapper.readValue(is, Map.class);
                    logger.info("Successfully loaded {} ticker mappings", bbgToOreMap.size());

                    // 检查目标ticker的映射
                    Map<String, Object> targetMapping = bbgToOreMap.get(TARGET_TICKER);
                    logger.info("Target ticker '{}' mapping: {}", TARGET_TICKER, targetMapping);

                    // 检查所有ticker的格式
                    logger.info("Checking all ticker formats in mapping file:");
                    for (Map.Entry<String, Map<String, Object>> entry : bbgToOreMap.entrySet()) {
                        logger.info("Ticker: '{}' -> '{}'", entry.getKey(), entry.getValue());
                    }
                }
            } else {
                logger.warn("Ticker mapping file not found in classpath: {}", MAPPING_FILE_PATH);
                // 尝试从文件系统加载
                File mappingFile = new File(MAPPING_FILE_PATH);
                if (mappingFile.exists()) {
                    bbgToOreMap = objectMapper.readValue(mappingFile, Map.class);
                    logger.info("Successfully loaded {} ticker mappings from file system", bbgToOreMap.size());
                } else {
                    logger.warn("Ticker mapping file not found in file system either");
                    // 加载默认映射
                    loadDefaultMapping();
                }
            }
        } catch (IOException e) {
            logger.error("Error loading ticker mapping file", e);
            loadDefaultMapping();
        }
    }

    private static void loadDefaultMapping() {
        logger.info("Loading default mappings");
        // 添加一些默认的映射关系
        Map<String, Object> defaultMapping = new HashMap<>();
        defaultMapping.put("target", "USDCNH=X");
        defaultMapping.put("coefficient", 1.0);
        bbgToOreMap.put("USDCNH", defaultMapping);

        defaultMapping = new HashMap<>();
        defaultMapping.put("target", "EURUSD=X");
        defaultMapping.put("coefficient", 1.0);
        bbgToOreMap.put("EURUSD", defaultMapping);

        defaultMapping = new HashMap<>();
        defaultMapping.put("target", "USDJPY=X");
        defaultMapping.put("coefficient", 1.0);
        bbgToOreMap.put("USDJPY", defaultMapping);

        defaultMapping = new HashMap<>();
        defaultMapping.put("target", "GBPUSD=X");
        defaultMapping.put("coefficient", 1.0);
        bbgToOreMap.put("GBPUSD", defaultMapping);

        logger.info("Loaded {} default ticker mappings", bbgToOreMap.size());
    }

    public static Map<String, Object> getOreTickerAndCoefficient(String bbgTicker) {
        // 如果是目标ticker，记录详细信息
        if (TARGET_TICKER.equals(bbgTicker)) {
            logger.info("Processing target ticker: '{}'", bbgTicker);
            logger.info("Current mappings: {}", bbgToOreMap);

            // 检查ticker的每个字符
            logger.info("Checking target ticker characters:");
            for (int i = 0; i < bbgTicker.length(); i++) {
                char c = bbgTicker.charAt(i);
                logger.info("Character {}: '{}' (ASCII: {})", i, c, (int) c);
            }
        }

        // 检查ticker是否在映射中
        Map<String, Object> mapping = bbgToOreMap.get(bbgTicker);

        // 如果是目标ticker，记录查找结果
        if (TARGET_TICKER.equals(bbgTicker)) {
            if (mapping != null) {
                logger.info("Found mapping for target ticker: '{}' -> '{}'", bbgTicker, mapping);
            } else {
                logger.info("No mapping found for target ticker: '{}'", bbgTicker);
                // 尝试不同的匹配方式
                logger.info("Trying alternative matching methods:");
                for (Map.Entry<String, Map<String, Object>> entry : bbgToOreMap.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(bbgTicker)) {
                        logger.info("Found case-insensitive match: '{}' -> '{}'", entry.getKey(), entry.getValue());
                    }
                    if (entry.getKey().trim().equals(bbgTicker.trim())) {
                        logger.info("Found trimmed match: '{}' -> '{}'", entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        return mapping;
    }

    public static void updateMapping(String bbgTicker, String oreTicker, double coefficient) {
        Map<String, Object> mapping = new HashMap<>();
        mapping.put("target", oreTicker);
        mapping.put("coefficient", coefficient);
        bbgToOreMap.put(bbgTicker, mapping);
        try {
            objectMapper.writeValue(new File(MAPPING_FILE_PATH), bbgToOreMap);
            logger.info("Updated mapping for {} -> {} (coefficient: {})", bbgTicker, oreTicker, coefficient);
        } catch (IOException e) {
            logger.error("Error saving ticker mapping", e);
        }
    }

    public static Map<String, Map<String, Object>> getAllMappings() {
        return new HashMap<>(bbgToOreMap);
    }
}