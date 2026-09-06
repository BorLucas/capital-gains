package com.example.capitalgains.format;

import com.example.capitalgains.domain.Money;
import com.example.capitalgains.domain.Trade;
import com.example.capitalgains.domain.TradeType;
import com.example.capitalgains.domain.error.InvalidTradeException;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

/**
 * A trade in the challenge's JSON format, shared by the API and the CLI:
 * <pre>{"operation":"buy", "unit-cost":10.00, "quantity": 100}</pre>
 *
 * <p>The bean-validation annotations produce friendly 400 messages on the API.
 * {@link #toDomain()} does NOT rely on them: it checks its own invariants
 * (fail-fast) so the CLI, which does not go through the validator, is protected
 * too.</p>
 */
public record TradeJson(

        @NotBlank(message = "operation is required (buy or sell)")
        String operation,

        @NotNull(message = "unit-cost is required")
        @PositiveOrZero(message = "unit-cost cannot be negative")
        @Digits(integer = 15, fraction = 2, message = "unit-cost must have at most 2 decimal places")
        @JsonProperty("unit-cost")
        BigDecimal unitCost,

        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be greater than zero")
        Long quantity
) {

    public Trade toDomain() {
        if (unitCost == null) {
            throw new InvalidTradeException("unit-cost is required");
        }
        if (quantity == null) {
            throw new InvalidTradeException("quantity is required");
        }
        if (unitCost.stripTrailingZeros().scale() > Money.SCALE) {
            throw new InvalidTradeException("unit-cost must have at most 2 decimal places");
        }
        TradeType type = TradeType.fromCode(operation);
        return new Trade(type, Money.of(unitCost), quantity);
    }

    public static List<Trade> toDomain(List<TradeJson> trades) {
        return trades.stream().map(TradeJson::toDomain).toList();
    }
}
