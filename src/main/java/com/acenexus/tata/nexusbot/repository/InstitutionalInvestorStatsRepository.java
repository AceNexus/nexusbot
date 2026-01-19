package com.acenexus.tata.nexusbot.repository;

import com.acenexus.tata.nexusbot.entity.InstitutionalInvestorStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InstitutionalInvestorStatsRepository extends JpaRepository<InstitutionalInvestorStats, Long> {
    
    /**
     * 查詢指定股票在日期區間內的統計數據
     */
    List<InstitutionalInvestorStats> findByStockSymbolAndTradeDateBetweenOrderByTradeDateAsc(
            String stockSymbol, LocalDate startDate, LocalDate endDate);

    /**
     * 檢查指定日期是否有數據 (用來判斷是否需要呼叫 API)
     * 只需要查全市場是否有任何一筆資料即可，因為我們總是存全市場
     */
    boolean existsByTradeDate(LocalDate tradeDate);
}
