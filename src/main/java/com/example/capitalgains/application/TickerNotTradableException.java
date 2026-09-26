package com.example.capitalgains.application;

import com.example.capitalgains.domain.error.CapitalGainsException;

/** The ticker is not listed, or is delisted and the order is a buy. */
public class TickerNotTradableException extends CapitalGainsException {

    public TickerNotTradableException(String message) {
        super(message);
    }
}
