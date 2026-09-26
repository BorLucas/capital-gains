package com.example.capitalgains.domain;

import com.example.capitalgains.domain.error.InvalidRuleException;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * The capital-gains tax regime a sell is assessed under: the tax rate on the
 * taxable profit, and the gross sale value up to which a sell is exempt.
 *
 * @param rate            fraction of the taxable profit owed as tax, 0 to 1 (0.20 = 20%)
 * @param exemptionLimit  a sell whose gross value is at most this pays no tax
 */
public record TaxRules(BigDecimal rate, Money exemptionLimit) {

    /** The "Capital Gains" challenge's rules: 20% tax, exempt up to 20,000. */
    public static final TaxRules CHALLENGE = new TaxRules(new BigDecimal("0.20"), Money.of("20000"));

    public TaxRules {
        Objects.requireNonNull(rate, "rate");
        Objects.requireNonNull(exemptionLimit, "exemptionLimit");
        if (rate.signum() < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new InvalidRuleException("tax rate must be between 0% and 100%");
        }
        if (exemptionLimit.isNegative()) {
            throw new InvalidRuleException("exemption limit cannot be negative");
        }
    }
}
