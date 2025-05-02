package com.trading.pnl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TradingPnlApplication {
    public static void main(String[] args) {
        SpringApplication.run(TradingPnlApplication.class, args);
    }
}