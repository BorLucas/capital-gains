package com.example.capitalgains.broker;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TickerRepository extends JpaRepository<Ticker, String> {

    List<Ticker> findAllByOrderBySymbolAsc();

    List<Ticker> findByActiveTrueOrderBySymbolAsc();
}
