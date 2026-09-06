package com.example.capitalgains.domain.error;

/**
 * A trade's data is missing or outside the allowed domain (e.g. null quantity,
 * negative cost, unknown type).
 */
public class InvalidTradeException extends CapitalGainsException {

    public InvalidTradeException(String message) {
        super(message);
    }
}
