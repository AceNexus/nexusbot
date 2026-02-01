package com.acenexus.tata.nexusbot.repository;

import com.acenexus.tata.nexusbot.entity.SecuritiesLendingStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SecuritiesLendingStatsRepository extends JpaRepository<SecuritiesLendingStats, Long> {

    List<SecuritiesLendingStats> findByStockSymbolAndTradeDateBetweenOrderByTradeDateAsc(String stockSymbol, LocalDate startDate, LocalDate endDate);

    boolean existsByStockSymbolAndTradeDate(String stockSymbol, LocalDate tradeDate);
}
