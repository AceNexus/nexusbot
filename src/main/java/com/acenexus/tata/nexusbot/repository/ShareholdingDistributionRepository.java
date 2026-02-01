package com.acenexus.tata.nexusbot.repository;

import com.acenexus.tata.nexusbot.entity.ShareholdingDistribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ShareholdingDistributionRepository extends JpaRepository<ShareholdingDistribution, Long> {

    List<ShareholdingDistribution> findByStockSymbolAndDataDateOrderByHoldingSharesLevel(String stockSymbol, LocalDate dataDate);

    boolean existsByStockSymbolAndDataDate(String stockSymbol, LocalDate dataDate);

    List<ShareholdingDistribution> findByStockSymbolAndDataDateBetweenOrderByDataDateAscHoldingSharesLevelAsc(String stockSymbol, LocalDate startDate, LocalDate endDate);
}
