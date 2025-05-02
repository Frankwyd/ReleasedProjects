package com.trading.pnl.model;

import lombok.Data;

@Data
public class OreMarketDataItem {
    private String oreTicker;
    private String value;
    private String date;
    private String bbgTicker; // 用于追踪原始数据
}