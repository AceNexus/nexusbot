package com.acenexus.tata.nexusbot.repository;

import com.acenexus.tata.nexusbot.entity.MarginTradingStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MarginTradingStatsRepository extends JpaRepository<MarginTradingStats, Long> {

    List<MarginTradingStats> findByStockSymbolAndTradeDateBetweenOrderByTradeDateAsc(String stockSymbol, LocalDate startDate, LocalDate endDate);

    boolean existsByStockSymbolAndTradeDate(String stockSymbol, LocalDate tradeDate);
}
