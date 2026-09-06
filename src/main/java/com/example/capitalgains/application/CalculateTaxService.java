package com.example.capitalgains.application;

import com.example.capitalgains.domain.TaxCalculator;
import com.example.capitalgains.domain.Trade;
import com.example.capitalgains.domain.TradeResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Use case: calculate the tax for one or several simulations and record them in
 * the history.
 *
 * <p>Fail-fast on the batch: <b>all</b> simulations are calculated (and may
 * fail) before any write, and the write happens in a single transaction. A
 * half-persisted simulation is never left behind.</p>
 */
@Service
public class CalculateTaxService {

    private static final TaxCalculator CALCULATOR = new TaxCalculator();

    private final HistoryService history;

    public CalculateTaxService(HistoryService history) {
        this.history = history;
    }

    public CalculatedSimulation calculate(List<Trade> trades) {
        List<TradeResult> results = CALCULATOR.calculate(trades);
        Long id = history.record(results).getId();
        return new CalculatedSimulation(id, results);
    }

    public List<CalculatedSimulation> calculateBatch(List<List<Trade>> simulations) {
        List<List<TradeResult>> results = simulations.stream()
                .map(CALCULATOR::calculate)
                .toList();

        List<Long> ids = history.recordAll(results);

        return IntStream.range(0, results.size())
                .mapToObj(i -> new CalculatedSimulation(ids.get(i), results.get(i)))
                .toList();
    }
}
