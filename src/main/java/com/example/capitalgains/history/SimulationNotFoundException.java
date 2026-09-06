package com.example.capitalgains.history;

import com.example.capitalgains.domain.error.CapitalGainsException;

public class SimulationNotFoundException extends CapitalGainsException {

    public SimulationNotFoundException(Long id) {
        super("simulation not found: " + id);
    }
}
