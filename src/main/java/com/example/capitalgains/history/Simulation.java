package com.example.capitalgains.history;

import com.example.capitalgains.domain.TradeResult;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A simulation that has been calculated and stored in the history: the sequence
 * of trades that was submitted and the tax resulting from each one.
 *
 * <p>{@code tradeCount} and {@code totalTax} are denormalized and computed once
 * at creation (a simulation is immutable once saved). This lets the listing
 * query just the summary, without loading the collection of items.</p>
 */
@Entity
@Table(name = "simulation")
public class Simulation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "trade_count", nullable = false)
    private int tradeCount;

    @Column(name = "total_tax", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalTax;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "simulation_item", joinColumns = @JoinColumn(name = "simulation_id"))
    @OrderColumn(name = "position")
    private List<SimulationItem> items = new ArrayList<>();

    protected Simulation() {
        // required by JPA
    }

    private Simulation(List<SimulationItem> items, BigDecimal totalTax, Instant createdAt) {
        this.items = new ArrayList<>(items);
        this.tradeCount = items.size();
        this.totalTax = totalTax;
        this.createdAt = createdAt;
    }

    public static Simulation of(List<TradeResult> results, Instant createdAt) {
        List<SimulationItem> items = results.stream().map(SimulationItem::of).toList();
        BigDecimal total = results.stream()
                .map(r -> r.tax().toBigDecimal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Simulation(items, total, createdAt);
    }

    public Long getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public int getTradeCount() {
        return tradeCount;
    }

    public BigDecimal getTotalTax() {
        return totalTax;
    }

    public List<SimulationItem> getItems() {
        return List.copyOf(items);
    }
}
