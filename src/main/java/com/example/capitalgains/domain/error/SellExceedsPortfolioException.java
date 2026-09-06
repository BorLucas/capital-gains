package com.example.capitalgains.domain.error;

/**
 * Attempt to sell more shares than the portfolio holds. The request is
 * well-formed but breaks a business rule -> mapped to HTTP 422.
 */
public class SellExceedsPortfolioException extends CapitalGainsException {

    public SellExceedsPortfolioException(long quantitySold, long quantityHeld) {
        super("selling %d shares, but only %d are held".formatted(quantitySold, quantityHeld));
    }
}
