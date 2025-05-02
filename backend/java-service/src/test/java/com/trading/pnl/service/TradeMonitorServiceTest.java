package com.trading.pnl.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.io.File;
import java.util.Map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.*;

class TradeMonitorServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private TradeMonitorService tradeMonitorService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tradeMonitorService = new TradeMonitorService(messagingTemplate);
    }

    @Test
    void testGetLatestTrades_Success() {
        // 准备测试数据
        String testFilePath = "src/test/resources/test_trades.csv";

        // 执行测试
        Map<String, Object> result = tradeMonitorService.getLatestTrades();

        // 验证结果
        assertNotNull(result);
        assertEquals("success", result.get("status"));
        assertNotNull(result.get("data"));

        // 验证WebSocket消息发送，指定具体的destination
        verify(messagingTemplate).convertAndSend(eq("/topic/trades"), any(Map.class));
    }

    @Test
    void testGetLatestTrades_FileNotFound() {
        // 设置不存在的文件路径
        String nonExistentPath = "non_existent_file.csv";

        // 执行测试
        Map<String, Object> result = tradeMonitorService.getLatestTrades();

        // 验证结果
        assertEquals("error", result.get("status"));
        assertEquals("Trade file not found", result.get("message"));
    }
}