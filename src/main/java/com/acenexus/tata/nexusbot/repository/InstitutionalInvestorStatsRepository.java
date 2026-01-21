package com.acenexus.tata.nexusbot.repository;

import com.acenexus.tata.nexusbot.entity.InstitutionalInvestorStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 法人買賣超統計數據存取層
 */
@Repository
public interface InstitutionalInvestorStatsRepository extends JpaRepository<InstitutionalInvestorStats, Long> {

    /**
     * 查詢指定股票在特定日期的籌碼數據
     *
     * @param stockSymbol 股票代號
     * @param tradeDate   交易日期
     * @return 籌碼數據
     */
    Optional<InstitutionalInvestorStats> findByStockSymbolAndTradeDate(String stockSymbol, LocalDate tradeDate);

    /**
     * 查詢指定日期範圍內的所有籌碼數據 (常用於歷史回測或區間統計) - 依照日期遞減排序 (最新在前)
     *
     * @param stockSymbol 股票代號
     * @param startDate   開始日期
     * @param endDate     結束日期
     * @return 籌碼數據列表
     */
    List<InstitutionalInvestorStats> findByStockSymbolAndTradeDateBetweenOrderByTradeDateDesc(String stockSymbol, LocalDate startDate, LocalDate endDate);

    /**
     * 查詢指定日期範圍內的所有籌碼數據 - 依照日期遞增排序 (最舊在前，適用於圖表繪製)
     *
     * @param stockSymbol 股票代號
     * @param startDate   開始日期
     * @param endDate     結束日期
     * @return 籌碼數據列表
     */
    List<InstitutionalInvestorStats> findByStockSymbolAndTradeDateBetweenOrderByTradeDateAsc(String stockSymbol, LocalDate startDate, LocalDate endDate);

    /**
     * 查詢特定日期全市場的籌碼數據
     *
     * @param tradeDate 交易日期
     * @return 全市場籌碼數據
     */
    List<InstitutionalInvestorStats> findByTradeDate(LocalDate tradeDate);

    /**
     * 檢查特定日期是否有資料 (用於判斷是否需要執行排程同步)
     *
     * @param tradeDate 交易日期
     * @return 是否有資料
     */
    boolean existsByTradeDate(LocalDate tradeDate);
}