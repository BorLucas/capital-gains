package com.example.capitalgains.format;

import com.example.capitalgains.domain.TradeResult;

import java.math.BigDecimal;
import java.util.List;

/**
 * Output in the challenge format: <pre>{"tax":0.00}</pre>
 */
public record TaxJson(BigDecimal tax) {

    public static List<TaxJson> fromResults(List<TradeResult> results) {
        return results.stream()
                .map(r -> new TaxJson(r.tax().toBigDecimal()))
                .toList();
    }
}
