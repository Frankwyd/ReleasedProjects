package com.trading.pnl.controller;

import com.trading.pnl.service.TradeMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // 在生产环境中应该限制具体的源
public class TradeController {

    private final TradeMonitorService tradeMonitorService;

    @GetMapping("/trades")
    public ResponseEntity<Map<String, Object>> getTrades() {
        return getTradesInternal(false);
    }

    @GetMapping("/trades/refresh")
    public ResponseEntity<Map<String, Object>> refreshTrades() {
        return getTradesInternal(true);
    }

    private ResponseEntity<Map<String, Object>> getTradesInternal(boolean forceRefresh) {
        try {
            log.info("Receiving request for trades data, forceRefresh: {}", forceRefresh);
            Map<String, Object> response = tradeMonitorService.getLatestTrades(forceRefresh);
            log.debug("Trade response: {}", response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing trades request", e);
            Map<String, Object> errorResponse = Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "details", e.getClass().getName(),
                    "data", new ArrayList<>());
            return ResponseEntity.ok(errorResponse);
        }
    }
}