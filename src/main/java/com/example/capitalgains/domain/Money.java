package com.example.capitalgains.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * A monetary amount in the local currency. Centralizes the project's rounding
 * rule: <b>2 decimal places, HALF_UP</b>. Every {@code Money} is normalized to
 * that scale on construction, so two "equal" amounts are {@code equals}.
 */
public record Money(BigDecimal amount) implements Comparable<Money> {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        Objects.requireNonNull(amount, "amount");
        amount = amount.setScale(SCALE, ROUNDING);
    }

    public static Money of(String amount) {
        return new Money(new BigDecimal(amount));
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    public Money plus(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money minus(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    public Money times(long quantity) {
        return new Money(amount.multiply(BigDecimal.valueOf(quantity)));
    }

    /** Applies a rate (e.g. {@code 0.20}) and rounds the result. */
    public Money applyRate(BigDecimal rate) {
        return new Money(amount.multiply(rate));
    }

    /** Divides by a whole number of shares, rounding to 2 decimal places. */
    public Money dividedBy(long divisor) {
        return new Money(amount.divide(BigDecimal.valueOf(divisor), SCALE, ROUNDING));
    }

    public Money abs() {
        return new Money(amount.abs());
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isZeroOrNegative() {
        return amount.signum() <= 0;
    }

    public boolean isLessThanOrEqualTo(Money other) {
        return compareTo(other) <= 0;
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(other.amount);
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }
}
