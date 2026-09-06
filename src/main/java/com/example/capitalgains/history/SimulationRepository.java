package com.example.capitalgains.history;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SimulationRepository extends JpaRepository<Simulation, Long> {

    /** Paginated listing using the summary projection (does not load the items). */
    List<SimulationSummary> findAllByOrderByIdDesc(Pageable pageable);
}
