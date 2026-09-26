package com.example.capitalgains.application;

import com.example.capitalgains.domain.FeeSchedule;
import com.example.capitalgains.domain.FeeType;
import com.example.capitalgains.domain.Money;
import com.example.capitalgains.domain.TaxRules;

import java.math.BigDecimal;

/**
 * The broker's fees and tax rules as people read them: the tax rate is a
 * percentage (20, not 0.20) and so is a PERCENT fee.
 */
public record RulesView(FeeType feeType, BigDecimal feeValue, BigDecimal taxRatePercent, BigDecimal exemptionLimit) {

    static RulesView of(FeeSchedule fees, TaxRules rules) {
        return new RulesView(fees.type(), fees.value().stripTrailingZeros(),
                rules.rate().movePointRight(2).stripTrailingZeros(), rules.exemptionLimit().amount());
    }

    FeeSchedule fees() {
        return new FeeSchedule(feeType, feeValue);
    }

    TaxRules rules() {
        return new TaxRules(taxRatePercent.movePointLeft(2), Money.of(exemptionLimit));
    }
}
