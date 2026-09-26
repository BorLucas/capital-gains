package com.example.capitalgains.broker;

import com.example.capitalgains.domain.Money;
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
 * A buy or sell a user executed on the broker. The tax is not stored: it is
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
        this.executedAt = executedAt;
    }

    public static PlacedTrade of(Long userId, String ticker, Trade trade, Instant executedAt) {
        return new PlacedTrade(userId, ticker, trade, executedAt);
    }

    public Trade toDomain() {
        return new Trade(type, Money.of(unitCost), quantity);
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
