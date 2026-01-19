package com.acenexus.tata.nexusbot.service;

import com.acenexus.tata.nexusbot.client.TwseApiClient;
import com.acenexus.tata.nexusbot.dto.InstitutionalInvestorsData;
import com.acenexus.tata.nexusbot.entity.InstitutionalInvestorStats;
import com.acenexus.tata.nexusbot.repository.InstitutionalInvestorStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 股票籌碼服務
 * 負責處理法人進出數據的查詢與儲存
 * 實作「資料庫優先，API 補漏」的策略
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockChipService {

    private final InstitutionalInvestorStatsRepository statsRepository;
    private final TwseApiClient twseApiClient;

    /**
     * 取得指定股票在日期區間內的法人進出數據
     * 策略：資料庫優先 (DB First)，缺失日期自動補齊 (Auto-fill Missing Data)
     */
    public List<InstitutionalInvestorsData> getInstitutionalInvestorsRange(String symbol, LocalDate startDate, LocalDate endDate) {
        // 1. 計算區間內的有效交易日 (排除週末)
        List<LocalDate> targetDates = getBusinessDaysBetween(startDate, endDate);
        if (targetDates.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 確保這些日期的數據已存在於 DB (若無則自動補抓)
        ensureDataAvailability(targetDates);

        // 3. 從 DB 查詢完整結果並轉換為 DTO
        return statsRepository.findByStockSymbolAndTradeDateBetweenOrderByTradeDateAsc(symbol, startDate, endDate)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 確保指定日期的數據已存在於資料庫
     * 若發現缺失日期，會觸發 API 抓取全市場數據並存入 DB
     */
    private void ensureDataAvailability(List<LocalDate> dates) {
        // 找出 DB 中已存在的日期 (使用全市場檢查，只要當天有任何一筆資料即視為已同步)
        // 注意：這裡假設 statsRepository 能高效檢查日期是否存在
        // 為了效能，我們只對「區間內」的日期做檢查，而不是逐日 query
        
        // 實作策略：
        // 這裡無法簡單用 "findByTradeDateIn" 因為資料量太大
        // 我們改用 "找出缺少的日期" 的反向思維 -> 逐日檢查 (雖然 N+1 但 N 不大且有快取/索引)
        // 或者優化：先查該股票在區間內有的日期，缺的就是可能沒同步的
        // 但考慮到可能是 "該股票當天沒交易" vs "DB沒資料"，我們還是保守檢查全市場狀態
        
        List<LocalDate> missingDates = new ArrayList<>();
        for (LocalDate date : dates) {
            if (!statsRepository.existsByTradeDate(date)) {
                missingDates.add(date);
            }
        }

        if (missingDates.isEmpty()) {
            return;
        }

        log.info("Found {} missing dates in DB, starting auto-fill process...", missingDates.size());
        
        for (LocalDate date : missingDates) {
            fetchAndSaveDailyStats(date);
            
            // 避免觸發 API Rate Limit
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 取得兩個日期之間的平日 (排除週六、週日)
     */
    private List<LocalDate> getBusinessDaysBetween(LocalDate start, LocalDate end) {
        List<LocalDate> businessDays = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            if (current.getDayOfWeek().getValue() < 6) { // 1=Mon, ..., 5=Fri
                businessDays.add(current);
            }
            current = current.plusDays(1);
        }
        return businessDays;
    }

    /**
     * 從 API 抓取指定日期的全市場數據並存入 DB
     */
    @Transactional
    public void fetchAndSaveDailyStats(LocalDate date) {
        try {
            List<InstitutionalInvestorStats> entities = new ArrayList<>();

            // 1. 呼叫 TWSE API 取得上市全市場數據
            Map<String, InstitutionalInvestorsData> twseDataMap = twseApiClient.getAllInstitutionalInvestorsByDate(date);
            if (!twseDataMap.isEmpty()) {
                for (InstitutionalInvestorsData data : twseDataMap.values()) {
                    entities.add(toEntity(data));
                }
                log.debug("Fetched {} TWSE records for date {}", twseDataMap.size(), date);
            }

            // 2. 呼叫 TPEx API 取得上櫃全市場數據
            Map<String, InstitutionalInvestorsData> tpexDataMap = twseApiClient.getTpexAllInstitutionalInvestorsByDate(date);
            if (!tpexDataMap.isEmpty()) {
                for (InstitutionalInvestorsData data : tpexDataMap.values()) {
                    // 避免重複 (理論上代號不會重複，但保險起見)
                    if (!twseDataMap.containsKey(data.getSymbol())) {
                        entities.add(toEntity(data));
                    }
                }
                log.debug("Fetched {} TPEx records for date {}", tpexDataMap.size(), date);
            }
            
            if (entities.isEmpty()) {
                log.info("No data found from API (TWSE & TPEx) for date {} (Market might be closed)", date);
                // 可以考慮在 DB 記錄 "休市" 狀態
                return;
            }

            // 3. 批次儲存 (上市 + 上櫃)
            statsRepository.saveAll(entities);
            log.info("Saved total {} records (TWSE + TPEx) for date {}", entities.size(), date);
            
        } catch (Exception e) {
            log.error("Error fetching/saving daily stats for date {}", date, e);
        }
    }

    private InstitutionalInvestorsData toDto(InstitutionalInvestorStats entity) {
        return InstitutionalInvestorsData.builder()
                .symbol(entity.getStockSymbol())
                .name(entity.getStockName())
                .date(entity.getTradeDate())
                .foreignInvestorBuy(entity.getForeignInvestorBuy())
                .foreignInvestorSell(entity.getForeignInvestorSell())
                .foreignInvestorBuySell(entity.getForeignInvestorBuySell())
                .investmentTrustBuy(entity.getInvestmentTrustBuy())
                .investmentTrustSell(entity.getInvestmentTrustSell())
                .investmentTrustBuySell(entity.getInvestmentTrustBuySell())
                .dealerBuy(entity.getDealerBuy())
                .dealerSell(entity.getDealerSell())
                .dealerBuySell(entity.getDealerBuySell())
                .totalBuy(entity.getTotalBuy())
                .totalSell(entity.getTotalSell())
                .totalBuySell(entity.getTotalBuySell())
                .build();
    }

    private InstitutionalInvestorStats toEntity(InstitutionalInvestorsData dto) {
        return InstitutionalInvestorStats.builder()
                .stockSymbol(dto.getSymbol())
                .stockName(dto.getName())
                .tradeDate(dto.getDate())
                .foreignInvestorBuy(dto.getForeignInvestorBuy())
                .foreignInvestorSell(dto.getForeignInvestorSell())
                .foreignInvestorBuySell(dto.getForeignInvestorBuySell())
                .investmentTrustBuy(dto.getInvestmentTrustBuy())
                .investmentTrustSell(dto.getInvestmentTrustSell())
                .investmentTrustBuySell(dto.getInvestmentTrustBuySell())
                .dealerBuy(dto.getDealerBuy())
                .dealerSell(dto.getDealerSell())
                .dealerBuySell(dto.getDealerBuySell())
                .totalBuy(dto.getTotalBuy()) // 注意：DTO 裡目前可能沒有計算總買進賣出，只算淨額，視 API 回傳而定
                .totalSell(dto.getTotalSell())
                .totalBuySell(dto.getTotalBuySell())
                .build();
    }
}
