package com.example.capitalgains.orders;

import com.example.capitalgains.domain.Money;
import com.example.capitalgains.domain.Trade;
import com.example.capitalgains.domain.TradeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;

/**
 * A trade as it was submitted on an order, stored so the worker can assess the
 * order later. Already validated at submission time; {@link #toDomain()} just
 * rebuilds the domain object.
 */
@Embeddable
public class RequestedTrade {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TradeType type;

    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitCost;

    @Column(nullable = false)
    private long quantity;

    protected RequestedTrade() {
        // required by JPA
    }

    private RequestedTrade(TradeType type, BigDecimal unitCost, long quantity) {
        this.type = type;
        this.unitCost = unitCost;
        this.quantity = quantity;
    }

    public static RequestedTrade of(Trade trade) {
        return new RequestedTrade(trade.type(), trade.unitCost().amount(), trade.quantity());
    }

    public Trade toDomain() {
        return new Trade(type, Money.of(unitCost), quantity);
    }
}
