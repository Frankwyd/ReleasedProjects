package com.trading.pnl.config;

import com.trading.pnl.service.TradeMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulingConfig {

    private final TradeMonitorService tradeMonitorService;

    @Scheduled(fixedRate = 300000) // 改为300秒检查一次
    public void monitorTrades() {
        try {
            tradeMonitorService.getLatestTrades();
        } catch (Exception e) {
            log.error("Error monitoring trades: ", e);
        }
    }
}