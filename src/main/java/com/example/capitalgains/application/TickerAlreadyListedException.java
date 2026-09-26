package com.example.capitalgains.application;

import com.example.capitalgains.domain.error.CapitalGainsException;

public class TickerAlreadyListedException extends CapitalGainsException {

    public TickerAlreadyListedException(String symbol) {
        super("ticker already listed: " + symbol);
    }
}
