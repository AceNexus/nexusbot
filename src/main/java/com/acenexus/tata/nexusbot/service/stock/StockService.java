package com.acenexus.tata.nexusbot.service.stock;

import com.acenexus.tata.nexusbot.dto.StockInfo;
import com.acenexus.tata.nexusbot.enums.StockMarket;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 股票查詢服務介面
 */
public interface StockService {

    /**
     * 查詢股票資訊
     *
     * @param symbol 股票代號
     * @param market 市場類型
     * @return 股票資訊，若查詢失敗則返回 empty
     */
    CompletableFuture<Optional<StockInfo>> getStockInfo(String symbol, StockMarket market);

    /**
     * 檢查此服務是否支援指定的市場
     *
     * @param market 市場類型
     * @return true 表示支援
     */
    boolean supports(StockMarket market);
}
