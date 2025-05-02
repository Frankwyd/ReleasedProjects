package com.trading.pnl.controller;

import com.trading.pnl.model.OreMarketDataItem;
import com.trading.pnl.service.OreConversionService;
import com.trading.pnl.service.OreXmlService;
import com.trading.pnl.util.TickerMappingUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.concurrent.*;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.zip.GZIPOutputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.io.FileNotFoundException;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/market-data")
@CrossOrigin(origins = "*")
public class MarketDataController {
    private static final Logger logger = LoggerFactory.getLogger(MarketDataController.class);
    private static final String ERROR_MARK = "ERR";
    private static final String NA_MARK = "N/A";
    private static final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private static final Map<String, CachedData> dataCache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = 5 * 60 * 1000; // 5分钟缓存
    private static final int MAX_CACHE_SIZE = 2000; // 调整为2000条记录
    private static final AtomicLong totalCacheHits = new AtomicLong(0); // 总缓存命中次数
    private static final AtomicLong totalCacheMisses = new AtomicLong(0); // 总缓存未命中次数

    @Autowired
    private OreConversionService oreConversionService;

    @Autowired
    private OreXmlService oreXmlService;

    @Data
    public static class LoadDataRequest {
        private String basePath;
        private String tm1File;
        private String currentFile;
        private String date;
        private String outputDir;
    }

    @Data
    public static class MarketDataItem {
        private String ticker;
        private String tm1Value;
        private String currentValue;
        private String diff;
    }

    @Data
    private static class CachedData {
        private final List<MarketDataItem> data;
        private final long timestamp;
        private final long tm1FileLastModified;
        private final long currentFileLastModified;
        private final AtomicLong hitCount; // 使用AtomicLong确保线程安全
        private volatile long lastAccessTime; // 使用volatile确保可见性

        public CachedData(List<MarketDataItem> data, long tm1FileLastModified, long currentFileLastModified) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
            this.tm1FileLastModified = tm1FileLastModified;
            this.currentFileLastModified = currentFileLastModified;
            this.hitCount = new AtomicLong(0);
            this.lastAccessTime = System.currentTimeMillis();
        }

        public void incrementHitCount() {
            hitCount.incrementAndGet();
            lastAccessTime = System.currentTimeMillis();
            totalCacheHits.incrementAndGet();
        }

        public boolean isValid(long newTm1LastModified, long newCurrentLastModified) {
            return tm1FileLastModified == newTm1LastModified &&
                    currentFileLastModified == newCurrentLastModified;
        }

        public String getStats() {
            return String.format("hits=%d, age=%ds, lastAccess=%ds ago, records=%d",
                    hitCount.get(),
                    (System.currentTimeMillis() - timestamp) / 1000,
                    (System.currentTimeMillis() - lastAccessTime) / 1000,
                    data.size());
        }
    }

    @PostMapping("/load")
    public ResponseEntity<?> loadMarketData(@RequestBody LoadDataRequest request) {
        logger.info("Received request to load market data. Base path: {}, TM1 file: {}, Current file: {}",
                request.getBasePath(), request.getTm1File(), request.getCurrentFile());

        try {
            // 获取文件对象
            File tm1File = new File(request.getBasePath(), request.getTm1File());
            File currentFile = new File(request.getBasePath(), request.getCurrentFile());

            // 检查文件是否存在
            if (!tm1File.exists() || !currentFile.exists()) {
                throw new FileNotFoundException("市场数据文件不存在");
            }

            // 获取文件最后修改时间
            long tm1LastModified = tm1File.lastModified();
            long currentLastModified = currentFile.lastModified();

            // 生成缓存键
            String cacheKey = request.getTm1File() + "|" + request.getCurrentFile();

            // 检查缓存
            CachedData cachedData = dataCache.get(cacheKey);
            boolean isLiveMode = request.getCurrentFile().contains("Live");

            if (cachedData != null) {
                try {
                    if (isLiveMode) {
                        if (currentLastModified <= cachedData.currentFileLastModified) {
                            cachedData.incrementHitCount();
                            logger.info(
                                    "Cache hit for live data [{}]. File not modified since cache. Cache stats: {}, Global hits/misses: {}/{}",
                                    cacheKey, cachedData.getStats(), totalCacheHits.get(), totalCacheMisses.get());
                            return createCompressedResponse(cachedData.getData());
                        } else {
                            totalCacheMisses.incrementAndGet();
                            logger.info(
                                    "Cache miss for live data [{}]. File modified at {} which is newer than cache at {}. Global hits/misses: {}/{}",
                                    cacheKey, new Date(currentLastModified),
                                    new Date(cachedData.currentFileLastModified),
                                    totalCacheHits.get(), totalCacheMisses.get());
                        }
                    } else {
                        if (cachedData.isValid(tm1LastModified, currentLastModified)) {
                            cachedData.incrementHitCount();
                            logger.info("Cache hit for EOD data [{}]. Cache stats: {}, Global hits/misses: {}/{}",
                                    cacheKey, cachedData.getStats(), totalCacheHits.get(), totalCacheMisses.get());
                            return createCompressedResponse(cachedData.getData());
                        } else {
                            totalCacheMisses.incrementAndGet();
                            logger.info(
                                    "Cache miss for EOD data [{}]. Files modified. TM1: {}, Current: {}. Global hits/misses: {}/{}",
                                    cacheKey, new Date(tm1LastModified), new Date(currentLastModified),
                                    totalCacheHits.get(), totalCacheMisses.get());
                        }
                    }
                } catch (Exception e) {
                    totalCacheMisses.incrementAndGet();
                    logger.warn(
                            "Error while processing cached data for [{}], will reload from files. Error: {}. Global hits/misses: {}/{}",
                            cacheKey, e.getMessage(), totalCacheHits.get(), totalCacheMisses.get());
                }
            } else {
                totalCacheMisses.incrementAndGet();
                logger.info("Cache miss for [{}]: No cached data found. Global hits/misses: {}/{}",
                        cacheKey, totalCacheHits.get(), totalCacheMisses.get());
            }

            // 并行读取两个文件
            CompletableFuture<Map<String, String>> tm1Future = CompletableFuture.supplyAsync(
                    () -> {
                        try {
                            return readExcelData(tm1File);
                        } catch (Exception e) {
                            throw new CompletionException(e);
                        }
                    }, executorService);

            CompletableFuture<Map<String, String>> currentFuture = CompletableFuture.supplyAsync(
                    () -> {
                        try {
                            return readExcelData(currentFile);
                        } catch (Exception e) {
                            throw new CompletionException(e);
                        }
                    }, executorService);

            // 等待两个任务完成并合并结果
            Map<String, String> tm1Data = tm1Future.get(30, TimeUnit.SECONDS);
            Map<String, String> currentData = currentFuture.get(30, TimeUnit.SECONDS);

            // 使用Stream API并行处理数据合并
            List<MarketDataItem> result = tm1Data.keySet().parallelStream()
                    .map(ticker -> {
                        MarketDataItem item = new MarketDataItem();
                        item.setTicker(ticker);
                        item.setTm1Value(tm1Data.get(ticker));
                        item.setCurrentValue(currentData.getOrDefault(ticker, NA_MARK));

                        // 计算差值
                        if (isNumeric(item.getTm1Value()) && isNumeric(item.getCurrentValue())) {
                            double tm1 = Double.parseDouble(item.getTm1Value());
                            double current = Double.parseDouble(item.getCurrentValue());
                            item.setDiff(String.format("%.4f", current - tm1));
                        } else {
                            item.setDiff(NA_MARK);
                        }

                        return item;
                    })
                    .collect(Collectors.toList());

            // 更新缓存
            CachedData newCacheData = new CachedData(result, tm1LastModified, currentLastModified);
            dataCache.put(cacheKey, newCacheData);
            logger.info(
                    "Cache updated for [{}]. Cache size: {}/{}, Records in cache: {}, Files: TM1={}, Current={}, Global hits/misses: {}/{}",
                    cacheKey, dataCache.size(), MAX_CACHE_SIZE, result.size(),
                    new Date(tm1LastModified), new Date(currentLastModified),
                    totalCacheHits.get(), totalCacheMisses.get());

            return createCompressedResponse(result);

        } catch (FileNotFoundException e) {
            logger.error("Market data file not found", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.ok(response);
        } catch (TimeoutException e) {
            logger.error("Timeout while loading market data", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "数据加载超时");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error loading market data", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/convert-to-ore")
    public ResponseEntity<?> convertToOreFormat(@RequestBody LoadDataRequest request) {
        logger.info(
                "Received request to convert market data to ORE format. Base path: {}, TM1 file: {}, Date: {}, Output dir: {}",
                request.getBasePath(), request.getTm1File(), request.getDate(), request.getOutputDir());

        try {
            // 读取TM1文件数据
            Map<String, String> tm1Data = readExcelData(new File(request.getBasePath(), request.getTm1File()));

            // 转换数据
            List<OreMarketDataItem> oreData = oreConversionService.convertToOreFormat(tm1Data, request.getDate());

            // 生成ORE格式文件
            String oreFileName = oreConversionService.generateOreFileName(request.getDate());
            File outputFile = new File(request.getOutputDir(), oreFileName);
            oreConversionService.saveToOreFormat(oreData, outputFile);

            // 更新ore.xml文件
            // 获取工作目录（outputDir的父目录）
            File outputDirFile = new File(request.getOutputDir());
            String workingDir = outputDirFile.getParent();
            logger.info("Working directory for ore.xml update: {}", workingDir);

            // 更新ore.xml
            oreXmlService.updateOreXml(workingDir, request.getDate(), oreFileName);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "数据已成功转换为ORE格式",
                    "file", outputFile.getAbsolutePath()));
        } catch (Exception e) {
            logger.error("Error converting to ORE format", e);
            return ResponseEntity.ok(Map.of(
                    "status", "error",
                    "message", e.getMessage()));
        }
    }

    private ResponseEntity<?> createCompressedResponse(List<MarketDataItem> data) throws Exception {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);

        // 将响应数据转换为JSON并压缩
        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(response);

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {
            gzipStream.write(jsonString.getBytes(StandardCharsets.UTF_8));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.CONTENT_ENCODING, "gzip");

        return new ResponseEntity<>(byteStream.toByteArray(), headers, HttpStatus.OK);
    }

    private boolean isNumeric(String str) {
        if (str == null || str.equals(ERROR_MARK) || str.equals(NA_MARK)) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private Map<String, String> readExcelData(File file) throws Exception {
        Map<String, String> data = new HashMap<>();
        int errorCount = 0;

        try (FileInputStream fis = new FileInputStream(file);
                Workbook workbook = new HSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            int startRow = 3;

            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    Cell tickerCell = row.getCell(0);
                    Cell valueCell = row.getCell(1);

                    if (tickerCell != null && valueCell != null) {
                        String ticker = "";

                        // 读取ticker
                        if (tickerCell.getCellType() == CellType.STRING) {
                            ticker = tickerCell.getStringCellValue().trim();
                        } else {
                            errorCount++;
                            if (errorCount <= 5) { // 只记录前5个错误
                                logger.warn("Row {}: Ticker cell is not a string type", i + 1);
                            }
                            continue;
                        }

                        // 读取value
                        String value;
                        try {
                            switch (valueCell.getCellType()) {
                                case NUMERIC:
                                    value = String.format("%.4f", valueCell.getNumericCellValue());
                                    break;
                                case STRING:
                                    try {
                                        double numValue = Double.parseDouble(valueCell.getStringCellValue().trim());
                                        value = String.format("%.4f", numValue);
                                    } catch (NumberFormatException e) {
                                        value = NA_MARK;
                                        errorCount++;
                                        if (errorCount <= 5) {
                                            logger.warn("Row {}: Invalid numeric string for ticker {}", i + 1, ticker);
                                        }
                                    }
                                    break;
                                case FORMULA:
                                    try {
                                        value = String.format("%.4f", valueCell.getNumericCellValue());
                                    } catch (Exception e) {
                                        value = ERROR_MARK;
                                        errorCount++;
                                        if (errorCount <= 5) {
                                            logger.warn("Row {}: Formula evaluation error for ticker {}", i + 1,
                                                    ticker);
                                        }
                                    }
                                    break;
                                case ERROR:
                                    value = ERROR_MARK;
                                    errorCount++;
                                    if (errorCount <= 5) {
                                        logger.warn("Row {}: Error cell for ticker {}", i + 1, ticker);
                                    }
                                    break;
                                case BLANK:
                                    value = NA_MARK;
                                    break;
                                default:
                                    value = NA_MARK;
                                    break;
                            }

                            if (!ticker.isEmpty()) {
                                data.put(ticker, value);
                            }
                        } catch (Exception e) {
                            errorCount++;
                            if (errorCount <= 5) {
                                logger.warn("Row {}: Error reading value for ticker {}: {}",
                                        i + 1, ticker, e.getMessage());
                            }
                            if (!ticker.isEmpty()) {
                                data.put(ticker, ERROR_MARK);
                            }
                        }
                    }
                }
            }
        }

        logger.info("File: {}. Read {} records. Total errors: {}",
                file.getName(), data.size(), errorCount);
        if (errorCount > 5) {
            logger.warn("Additional {} errors were suppressed", errorCount - 5);
        }

        return data;
    }
}