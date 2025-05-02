package com.trading.pnl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import jakarta.annotation.PostConstruct;

@Slf4j
@Service
public class TradeMonitorService {
    @Value("${trading.file.path}")
    private String tradeCsvPath;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Long lastModifiedTime = null;
    private final Map<String, Object> cache = new HashMap<>();
    private final SimpMessagingTemplate messagingTemplate;

    public TradeMonitorService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @PostConstruct
    public void init() {
        // 检查并创建目录
        Path directory = Paths.get(tradeCsvPath).getParent();
        try {
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
                log.info("Created directory: {}", directory);
            }
        } catch (IOException e) {
            log.error("Error creating directory: {}", directory, e);
        }
    }

    public Map<String, Object> getLatestTrades(boolean forceRefresh) {
        Map<String, Object> response = new HashMap<>();
        Path filePath = Paths.get(tradeCsvPath);

        try {
            // 检查文件是否存在
            if (!Files.exists(filePath)) {
                log.warn("Trade file not found at: {}", tradeCsvPath);
                response.put("status", "error");
                response.put("message", "Trade file not found");
                response.put("path", tradeCsvPath);
                // 返回空数据而不是错误状态
                response.put("data", new ArrayList<>());
                return response;
            }

            // 检查文件是否可读
            if (!Files.isReadable(filePath)) {
                log.error("Cannot read trade file at: {}", tradeCsvPath);
                response.put("status", "error");
                response.put("message", "Cannot read trade file");
                return response;
            }

            // 检查文件是否被修改或强制刷新
            long currentModified = Files.getLastModifiedTime(filePath).toMillis();
            if (!forceRefresh && lastModifiedTime != null && currentModified <= lastModifiedTime) {
                log.debug("No file updates since last check");
                response.put("status", "no_update");
                response.put("data", cache.get("trades"));
                broadcastTrades((List<Map<String, Object>>) cache.get("trades"));
                return response;
            }

            // 以只读模式读取文件
            List<Map<String, Object>> trades = readCsvFile(filePath);
            lastModifiedTime = currentModified;
            cache.put("trades", trades);

            response.put("status", "success");
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("data", trades);
            broadcastTrades(trades);

            return response;

        } catch (Exception e) {
            log.error("Error processing trade file: ", e);
            response.put("status", "error");
            response.put("message", e.getMessage());
            response.put("details", e.getClass().getName());
            return response;
        }
    }

    // 为了兼容现有代码，添加无参数的方法
    public Map<String, Object> getLatestTrades() {
        return getLatestTrades(false);
    }

    private List<Map<String, Object>> readCsvFile(Path filePath) throws IOException {
        List<Map<String, Object>> trades = new ArrayList<>();

        // 使用 Files.newBufferedReader 以只读模式打开文件
        try (BufferedReader br = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            String[] headers = null;

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] values = line.split(",");
                if (headers == null) {
                    headers = values;
                    log.debug("CSV headers: {}", String.join(", ", headers));
                    continue;
                }

                Map<String, Object> trade = new HashMap<>();
                for (int i = 0; i < headers.length && i < values.length; i++) {
                    String header = headers[i].trim();
                    String value = values[i].trim();

                    // 处理数值类型
                    switch (header) {
                        case "Notional":
                            try {
                                trade.put(header, Long.parseLong(value));
                            } catch (NumberFormatException e) {
                                trade.put(header, value);
                            }
                            break;
                        case "Cutoff":
                            try {
                                trade.put(header, Double.parseDouble(value));
                            } catch (NumberFormatException e) {
                                trade.put(header, value);
                            }
                            break;
                        case "OptMult":
                            try {
                                trade.put(header, Integer.parseInt(value));
                            } catch (NumberFormatException e) {
                                trade.put(header, value);
                            }
                            break;
                        default:
                            trade.put(header, value.equals("########") ? "" : value);
                    }
                }
                trades.add(trade);
            }

            log.debug("Read {} trades from file", trades.size());
            return trades;
        }
    }

    private void broadcastTrades(List<Map<String, Object>> trades) {
        if (trades != null && !trades.isEmpty()) {
            Map<String, Object> message = new HashMap<>();
            message.put("status", "success");
            message.put("data", trades);
            message.put("timestamp", LocalDateTime.now().toString());
            messagingTemplate.convertAndSend("/topic/trades", message);
        }
    }
}