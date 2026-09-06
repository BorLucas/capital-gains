package com.example.capitalgains.history;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Read model for a simulation's detail: each trade in the challenge format,
 * together with the tax it generated. Built inside the read transaction so the
 * web edge never touches the JPA entity directly.
 */
public record SimulationDetail(
        Long id,
        Instant createdAt,
        BigDecimal totalTax,
        List<Trade> trades
) {

    public record Trade(
            String operation,
            @JsonProperty("unit-cost") BigDecimal unitCost,
            long quantity,
            BigDecimal tax
    ) {}

    public static SimulationDetail of(Simulation simulation) {
        List<Trade> trades = simulation.getItems().stream()
                .map(item -> new Trade(
                        item.getType().code(),
                        item.getUnitCost(),
                        item.getQuantity(),
                        item.getTax()))
                .toList();
        return new SimulationDetail(
                simulation.getId(),
                simulation.getCreatedAt(),
                simulation.getTotalTax(),
                trades);
    }
}
