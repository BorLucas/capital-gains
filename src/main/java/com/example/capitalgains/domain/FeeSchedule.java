package com.example.capitalgains.domain;

import com.example.capitalgains.domain.error.InvalidRuleException;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * How much brokerage an order pays.
 *
 * @param type  fixed amount per order, or a percentage of the order's gross value
 * @param value the amount (FIXED) or the percentage, 0 to 100 (PERCENT)
 */
public record FeeSchedule(FeeType type, BigDecimal value) {

    public FeeSchedule {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(value, "value");
        if (value.signum() < 0) {
            throw new InvalidRuleException("fee cannot be negative");
        }
        if (type == FeeType.PERCENT && value.compareTo(new BigDecimal("100")) > 0) {
            throw new InvalidRuleException("a percentage fee cannot exceed 100%");
        }
    }

    public static FeeSchedule fixed(String amount) {
        return new FeeSchedule(FeeType.FIXED, new BigDecimal(amount));
    }

    public static FeeSchedule percent(String percentage) {
        return new FeeSchedule(FeeType.PERCENT, new BigDecimal(percentage));
    }

    /** The fee for an order of the given gross value, rounded like any {@link Money}. */
    public Money feeFor(Money orderValue) {
        return switch (type) {
            case FIXED -> Money.of(value);
            case PERCENT -> orderValue.applyRate(value.movePointLeft(2));
        };
    }
}
