package com.example.capitalgains.broker;

import com.example.capitalgains.domain.FeeSchedule;
import com.example.capitalgains.domain.FeeType;
import com.example.capitalgains.domain.Money;
import com.example.capitalgains.domain.TaxRules;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * The broker-wide fee schedule and tax rules, set by an admin. A single row
 * ({@link #ID}); every new trade is charged and taxed with what is here at the
 * moment it is placed.
 */
@Entity
@Table(name = "broker_config")
public class BrokerConfig {

    public static final long ID = 1L;

    @Id
    private Long id = ID;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false, length = 10)
    private FeeType feeType;

    @Column(name = "fee_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal feeValue;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal taxRate;

    @Column(name = "exemption_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal exemptionLimit;

    protected BrokerConfig() {
        // required by JPA
    }

    public BrokerConfig(FeeSchedule fees, TaxRules rules) {
        apply(fees, rules);
    }

    /** Both arguments are already-validated domain objects, so the row is always consistent. */
    public void apply(FeeSchedule fees, TaxRules rules) {
        this.feeType = fees.type();
        this.feeValue = fees.value();
        this.taxRate = rules.rate();
        this.exemptionLimit = rules.exemptionLimit().amount();
    }

    public FeeSchedule fees() {
        return new FeeSchedule(feeType, feeValue);
    }

    public TaxRules rules() {
        return new TaxRules(taxRate, Money.of(exemptionLimit));
    }
}
