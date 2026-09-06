package com.example.capitalgains.application;

import com.example.capitalgains.domain.TradeResult;

import java.util.List;

/**
 * Result of calculating and recording a simulation: the id generated in the
 * history plus the tax for each trade.
 */
public record CalculatedSimulation(Long id, List<TradeResult> results) {
}
