package com.example.capitalgains.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Tax owed by a trade. Its own nominal type (instead of a bare {@link Money}) so
 * that "money" and "tax due" are not interchangeable in signatures.
 */
public record Tax(Money amount) {

    public static final Tax ZERO = new Tax(Money.ZERO);

    public Tax {
        Objects.requireNonNull(amount, "amount");
    }

    public static Tax zero() {
        return ZERO;
    }

    public static Tax of(Money amount) {
        return amount.isPositive() ? new Tax(amount) : ZERO;
    }

    public BigDecimal toBigDecimal() {
        return amount.amount();
    }
}
