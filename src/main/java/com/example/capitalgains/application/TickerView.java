package com.example.capitalgains.application;

import com.example.capitalgains.broker.Ticker;

import java.math.BigDecimal;

public record TickerView(String symbol, String name, BigDecimal referencePrice, boolean active) {

    static TickerView of(Ticker ticker) {
        return new TickerView(ticker.getSymbol(), ticker.getName(), ticker.getReferencePrice(), ticker.isActive());
    }
}
