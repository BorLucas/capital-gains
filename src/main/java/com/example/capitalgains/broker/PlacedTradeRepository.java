package com.example.capitalgains.broker;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlacedTradeRepository extends JpaRepository<PlacedTrade, Long> {

    /** In execution order: the tax replay depends on it. */
    List<PlacedTrade> findByUserIdOrderByIdAsc(Long userId);

    List<PlacedTrade> findByUserIdAndTickerOrderByIdAsc(Long userId, String ticker);
}
