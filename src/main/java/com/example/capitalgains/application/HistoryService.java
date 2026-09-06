package com.example.capitalgains.application;

import com.example.capitalgains.domain.TradeResult;
import com.example.capitalgains.history.Simulation;
import com.example.capitalgains.history.SimulationDetail;
import com.example.capitalgains.history.SimulationNotFoundException;
import com.example.capitalgains.history.SimulationRepository;
import com.example.capitalgains.history.SimulationSummary;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Stores and queries already-calculated simulations. The web edge receives read
 * models ({@link SimulationSummary} / {@link SimulationDetail}), never the
 * entity.
 */
@Service
public class HistoryService {

    private final SimulationRepository repository;
    private final Clock clock;

    public HistoryService(SimulationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public Simulation record(List<TradeResult> results) {
        return repository.save(Simulation.of(results, Instant.now(clock)));
    }

    /** Stores several simulations in a single transaction (all or nothing). */
    @Transactional
    public List<Long> recordAll(List<List<TradeResult>> simulations) {
        return simulations.stream()
                .map(results -> record(results).getId())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SimulationSummary> list(Pageable page) {
        return repository.findAllByOrderByIdDesc(page);
    }

    @Transactional(readOnly = true)
    public SimulationDetail findDetail(Long id) {
        return repository.findById(id)
                .map(SimulationDetail::of)
                .orElseThrow(() -> new SimulationNotFoundException(id));
    }
}
