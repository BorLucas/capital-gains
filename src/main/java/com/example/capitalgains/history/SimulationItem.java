package com.example.capitalgains.history;

import com.example.capitalgains.domain.Trade;
import com.example.capitalgains.domain.TradeResult;
import com.example.capitalgains.domain.TradeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;

/**
 * A trade recorded inside a {@link Simulation}, together with the tax it
 * generated.
 */
@Embeddable
public class SimulationItem {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TradeType type;

    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitCost;

    @Column(nullable = false)
    private long quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal tax;

    protected SimulationItem() {
        // required by JPA
    }

    public SimulationItem(TradeType type, BigDecimal unitCost, long quantity, BigDecimal tax) {
        this.type = type;
        this.unitCost = unitCost;
        this.quantity = quantity;
        this.tax = tax;
    }

    static SimulationItem of(TradeResult result) {
        Trade trade = result.trade();
        return new SimulationItem(
                trade.type(),
                trade.unitCost().amount(),
                trade.quantity(),
                result.tax().toBigDecimal());
    }

    public TradeType getType() {
        return type;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public long getQuantity() {
        return quantity;
    }

    public BigDecimal getTax() {
        return tax;
    }
}
