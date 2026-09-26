package com.example.capitalgains.broker;

import com.example.capitalgains.domain.Money;
import com.example.capitalgains.domain.TaxRules;
import com.example.capitalgains.domain.Trade;
import com.example.capitalgains.domain.TradeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A buy or sell a user executed on the broker, with the brokerage fee charged
 * and the tax rules in force at the time: a later change of the broker's fees or
 * rules never rewrites history.
 * The tax is not stored: it is
 * recomputed by replaying the user's trades for the ticker, so the rules stay
 * in one place (the domain).
 */
@Entity
@Table(name = "placed_trade", indexes = @Index(name = "ix_placed_trade_user", columnList = "user_id"))
public class PlacedTrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TradeType type;

    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitCost;

    @Column(nullable = false)
    private long quantity;

    /** Nullable so rows from before fees existed read as zero. */
    @Column(precision = 19, scale = 2)
    private BigDecimal fee;

    /** Tax regime in force when the trade was made; null (older rows) means the challenge's. */
    @Column(name = "tax_rate", precision = 5, scale = 4)
    private BigDecimal taxRate;

    @Column(name = "exemption_limit", precision = 19, scale = 2)
    private BigDecimal exemptionLimit;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    protected PlacedTrade() {
        // required by JPA
    }

    private PlacedTrade(Long userId, String ticker, Trade trade, Instant executedAt) {
        this.userId = userId;
        this.ticker = ticker;
        this.type = trade.type();
        this.unitCost = trade.unitCost().amount();
        this.quantity = trade.quantity();
        this.fee = trade.fee().amount();
        this.taxRate = trade.rules().rate();
        this.exemptionLimit = trade.rules().exemptionLimit().amount();
        this.executedAt = executedAt;
    }

    public static PlacedTrade of(Long userId, String ticker, Trade trade, Instant executedAt) {
        return new PlacedTrade(userId, ticker, trade, executedAt);
    }

    public Trade toDomain() {
        TaxRules rules = taxRate == null || exemptionLimit == null
                ? TaxRules.CHALLENGE
                : new TaxRules(taxRate, Money.of(exemptionLimit));
        return new Trade(type, Money.of(unitCost), quantity, fee == null ? Money.ZERO : Money.of(fee), rules);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTicker() {
        return ticker;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }
}
