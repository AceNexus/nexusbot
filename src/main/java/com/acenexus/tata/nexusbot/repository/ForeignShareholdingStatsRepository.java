package com.acenexus.tata.nexusbot.repository;

import com.acenexus.tata.nexusbot.entity.ForeignShareholdingStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ForeignShareholdingStatsRepository extends JpaRepository<ForeignShareholdingStats, Long> {

    List<ForeignShareholdingStats> findByStockSymbolAndTradeDateBetweenOrderByTradeDateAsc(String stockSymbol, LocalDate startDate, LocalDate endDate);

    boolean existsByStockSymbolAndTradeDate(String stockSymbol, LocalDate tradeDate);
}
