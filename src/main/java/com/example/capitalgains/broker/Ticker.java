package com.example.capitalgains.broker;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * A stock the broker lists. Trades can only name a listed ticker; an inactive
 * one can no longer be bought, but holders can still sell it. Never deleted,
 * because past trades point at it.
 */
@Entity
@Table(name = "ticker")
public class Ticker {

    @Id
    @Column(length = 10)
    private String symbol;

    @Column(nullable = false, length = 60)
    private String name;

    /** Simulated quote the order ticket pre-fills; the user can type any price. */
    @Column(name = "reference_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal referencePrice;

    @Column(nullable = false)
    private boolean active = true;

    protected Ticker() {
        // required by JPA
    }

    public Ticker(String symbol, String name, BigDecimal referencePrice) {
        this.symbol = symbol;
        this.name = name;
        this.referencePrice = referencePrice;
    }

    public void update(String name, BigDecimal referencePrice, boolean active) {
        this.name = name;
        this.referencePrice = referencePrice;
        this.active = active;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getReferencePrice() {
        return referencePrice;
    }

    public boolean isActive() {
        return active;
    }
}
