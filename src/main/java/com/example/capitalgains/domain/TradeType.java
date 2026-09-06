package com.example.capitalgains.domain;

import com.example.capitalgains.domain.error.InvalidTradeException;

/**
 * Type of stock-market trade. The code ({@code "buy"} / {@code "sell"}) is the
 * format used in the challenge's input and output; conversion both ways lives
 * here rather than being scattered across the DTOs.
 */
public enum TradeType {

    BUY("buy"),
    SELL("sell");

    private final String code;

    TradeType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static TradeType fromCode(String code) {
        if (code == null) {
            throw new InvalidTradeException("operation is required (buy or sell)");
        }
        return switch (code.trim().toLowerCase()) {
            case "buy" -> BUY;
            case "sell" -> SELL;
            default -> throw new InvalidTradeException("invalid operation: " + code);
        };
    }
}
