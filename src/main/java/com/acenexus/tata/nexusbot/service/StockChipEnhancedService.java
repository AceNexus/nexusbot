package com.acenexus.tata.nexusbot.service;

import com.acenexus.tata.nexusbot.client.FinMindApiClient;
import com.acenexus.tata.nexusbot.dto.ForeignShareholdingData;
import com.acenexus.tata.nexusbot.dto.MarginTradingData;
import com.acenexus.tata.nexusbot.dto.SecuritiesLendingData;
import com.acenexus.tata.nexusbot.dto.ShareholdingDistributionData;
import com.acenexus.tata.nexusbot.dto.ShareholdingDistributionSummary;
import com.acenexus.tata.nexusbot.entity.ForeignShareholdingStats;
import com.acenexus.tata.nexusbot.entity.MarginTradingStats;
import com.acenexus.tata.nexusbot.entity.SecuritiesLendingStats;
import com.acenexus.tata.nexusbot.entity.ShareholdingDistribution;
import com.acenexus.tata.nexusbot.repository.ForeignShareholdingStatsRepository;
import com.acenexus.tata.nexusbot.repository.MarginTradingStatsRepository;
import com.acenexus.tata.nexusbot.repository.SecuritiesLendingStatsRepository;
import com.acenexus.tata.nexusbot.repository.ShareholdingDistributionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockChipEnhancedService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final FinMindApiClient finMindApiClient;
    private final MarginTradingStatsRepository marginTradingStatsRepository;
    private final SecuritiesLendingStatsRepository securitiesLendingStatsRepository;
    private final ForeignShareholdingStatsRepository foreignShareholdingStatsRepository;
    private final ShareholdingDistributionRepository shareholdingDistributionRepository;

    // ==================== 融資融券 ====================

    public List<MarginTradingData> getMarginTradingRange(String symbol, LocalDate start, LocalDate end) {
        ensureMarginTradingData(symbol, start, end);
        return marginTradingStatsRepository
                .findByStockSymbolAndTradeDateBetweenOrderByTradeDateAsc(symbol, start, end)
                .stream()
                .map(this::toMarginTradingDto)
                .collect(Collectors.toList());
    }

    private void ensureMarginTradingData(String symbol, LocalDate start, LocalDate end) {
        if (marginTradingStatsRepository.existsByStockSymbolAndTradeDate(symbol, end)
                || marginTradingStatsRepository.existsByStockSymbolAndTradeDate(symbol, start)) {
            return;
        }
        log.info("Fetching margin trading data from FinMind - symbol={}, start={}, end={}", symbol, start, end);
        List<MarginTradingData> apiData = finMindApiClient.getMarginTrading(
                symbol, start.format(DATE_FORMATTER), end.format(DATE_FORMATTER));
        for (MarginTradingData data : apiData) {
            try {
                marginTradingStatsRepository.save(toMarginTradingEntity(data));
            } catch (DataIntegrityViolationException e) {
                log.debug("Duplicate margin trading data skipped - symbol={}, date={}", symbol, data.getDate());
            }
        }
    }

    private MarginTradingData toMarginTradingDto(MarginTradingStats entity) {
        return MarginTradingData.builder()
                .symbol(entity.getStockSymbol())
                .date(entity.getTradeDate())
                .marginPurchaseBuy(entity.getMarginPurchaseBuy())
                .marginPurchaseSell(entity.getMarginPurchaseSell())
                .marginPurchaseCashRepayment(entity.getMarginPurchaseCashRepayment())
                .marginPurchaseYesterdayBalance(entity.getMarginPurchaseYesterdayBalance())
                .marginPurchaseTodayBalance(entity.getMarginPurchaseTodayBalance())
                .marginPurchaseLimit(entity.getMarginPurchaseLimit())
                .marginPurchaseChange(entity.getMarginPurchaseChange())
                .shortSaleBuy(entity.getShortSaleBuy())
                .shortSaleSell(entity.getShortSaleSell())
                .shortSaleCashRepayment(entity.getShortSaleCashRepayment())
                .shortSaleYesterdayBalance(entity.getShortSaleYesterdayBalance())
                .shortSaleTodayBalance(entity.getShortSaleTodayBalance())
                .shortSaleLimit(entity.getShortSaleLimit())
                .shortSaleChange(entity.getShortSaleChange())
                .offsetLoanAndShort(entity.getOffsetLoanAndShort())
                .utilizationRate(entity.getUtilizationRate())
                .note(entity.getNote())
                .build();
    }

    private MarginTradingStats toMarginTradingEntity(MarginTradingData dto) {
        return MarginTradingStats.builder()
                .stockSymbol(dto.getSymbol())
                .tradeDate(dto.getDate())
                .marginPurchaseBuy(dto.getMarginPurchaseBuy())
                .marginPurchaseSell(dto.getMarginPurchaseSell())
                .marginPurchaseCashRepayment(dto.getMarginPurchaseCashRepayment())
                .marginPurchaseYesterdayBalance(dto.getMarginPurchaseYesterdayBalance())
                .marginPurchaseTodayBalance(dto.getMarginPurchaseTodayBalance())
                .marginPurchaseLimit(dto.getMarginPurchaseLimit())
                .marginPurchaseChange(dto.getMarginPurchaseChange())
                .shortSaleBuy(dto.getShortSaleBuy())
                .shortSaleSell(dto.getShortSaleSell())
                .shortSaleCashRepayment(dto.getShortSaleCashRepayment())
                .shortSaleYesterdayBalance(dto.getShortSaleYesterdayBalance())
                .shortSaleTodayBalance(dto.getShortSaleTodayBalance())
                .shortSaleLimit(dto.getShortSaleLimit())
                .shortSaleChange(dto.getShortSaleChange())
                .offsetLoanAndShort(dto.getOffsetLoanAndShort())
                .utilizationRate(dto.getUtilizationRate())
                .note(dto.getNote())
                .build();
    }

    // ==================== 借券 ====================

    public List<SecuritiesLendingData> getSecuritiesLendingRange(String symbol, LocalDate start, LocalDate end) {
        ensureSecuritiesLendingData(symbol, start, end);
        return securitiesLendingStatsRepository
                .findByStockSymbolAndTradeDateBetweenOrderByTradeDateAsc(symbol, start, end)
                .stream()
                .map(this::toSecuritiesLendingDto)
                .collect(Collectors.toList());
    }

    private void ensureSecuritiesLendingData(String symbol, LocalDate start, LocalDate end) {
        if (securitiesLendingStatsRepository.existsByStockSymbolAndTradeDate(symbol, end)
                || securitiesLendingStatsRepository.existsByStockSymbolAndTradeDate(symbol, start)) {
            return;
        }
        log.info("Fetching securities lending data from FinMind - symbol={}, start={}, end={}", symbol, start, end);
        List<SecuritiesLendingData> apiData = finMindApiClient.getSecuritiesLending(
                symbol, start.format(DATE_FORMATTER), end.format(DATE_FORMATTER));
        for (SecuritiesLendingData data : apiData) {
            try {
                securitiesLendingStatsRepository.save(toSecuritiesLendingEntity(data));
            } catch (DataIntegrityViolationException e) {
                log.debug("Duplicate securities lending data skipped - symbol={}, date={}", symbol, data.getDate());
            }
        }
    }

    private SecuritiesLendingData toSecuritiesLendingDto(SecuritiesLendingStats entity) {
        return SecuritiesLendingData.builder()
                .symbol(entity.getStockSymbol())
                .date(entity.getTradeDate())
                .transactionType(entity.getTransactionType())
                .totalVolume(entity.getTotalVolume())
                .totalTransactions(entity.getTotalTransactions())
                .avgFeeRate(entity.getAvgFeeRate())
                .closePrice(entity.getClosePrice())
                .build();
    }

    private SecuritiesLendingStats toSecuritiesLendingEntity(SecuritiesLendingData dto) {
        return SecuritiesLendingStats.builder()
                .stockSymbol(dto.getSymbol())
                .tradeDate(dto.getDate())
                .transactionType(dto.getTransactionType())
                .totalVolume(dto.getTotalVolume())
                .totalTransactions(dto.getTotalTransactions())
                .avgFeeRate(dto.getAvgFeeRate())
                .closePrice(dto.getClosePrice())
                .build();
    }

    // ==================== 外資持股 ====================

    public List<ForeignShareholdingData> getForeignShareholdingRange(String symbol, LocalDate start, LocalDate end) {
        ensureForeignShareholdingData(symbol, start, end);
        return foreignShareholdingStatsRepository
                .findByStockSymbolAndTradeDateBetweenOrderByTradeDateAsc(symbol, start, end)
                .stream()
                .map(this::toForeignShareholdingDto)
                .collect(Collectors.toList());
    }

    private void ensureForeignShareholdingData(String symbol, LocalDate start, LocalDate end) {
        if (foreignShareholdingStatsRepository.existsByStockSymbolAndTradeDate(symbol, end)
                || foreignShareholdingStatsRepository.existsByStockSymbolAndTradeDate(symbol, start)) {
            return;
        }
        log.info("Fetching foreign shareholding data from FinMind - symbol={}, start={}, end={}", symbol, start, end);
        List<ForeignShareholdingData> apiData = finMindApiClient.getForeignShareholding(
                symbol, start.format(DATE_FORMATTER), end.format(DATE_FORMATTER));
        for (ForeignShareholdingData data : apiData) {
            try {
                foreignShareholdingStatsRepository.save(toForeignShareholdingEntity(data));
            } catch (DataIntegrityViolationException e) {
                log.debug("Duplicate foreign shareholding data skipped - symbol={}, date={}", symbol, data.getDate());
            }
        }
    }

    private ForeignShareholdingData toForeignShareholdingDto(ForeignShareholdingStats entity) {
        return ForeignShareholdingData.builder()
                .symbol(entity.getStockSymbol())
                .name(entity.getStockName())
                .date(entity.getTradeDate())
                .foreignInvestmentShares(entity.getForeignInvestmentShares())
                .foreignInvestmentRemainingShares(entity.getForeignInvestmentRemainingShares())
                .foreignInvestmentSharesRatio(entity.getForeignInvestmentSharesRatio())
                .foreignInvestmentRemainRatio(entity.getForeignInvestmentRemainRatio())
                .foreignInvestmentUpperLimitRatio(entity.getForeignInvestmentUpperLimitRatio())
                .numberOfSharesIssued(entity.getNumberOfSharesIssued())
                .build();
    }

    private ForeignShareholdingStats toForeignShareholdingEntity(ForeignShareholdingData dto) {
        return ForeignShareholdingStats.builder()
                .stockSymbol(dto.getSymbol())
                .stockName(dto.getName())
                .tradeDate(dto.getDate())
                .foreignInvestmentShares(dto.getForeignInvestmentShares())
                .foreignInvestmentRemainingShares(dto.getForeignInvestmentRemainingShares())
                .foreignInvestmentSharesRatio(dto.getForeignInvestmentSharesRatio())
                .foreignInvestmentRemainRatio(dto.getForeignInvestmentRemainRatio())
                .foreignInvestmentUpperLimitRatio(dto.getForeignInvestmentUpperLimitRatio())
                .numberOfSharesIssued(dto.getNumberOfSharesIssued())
                .build();
    }

    // ==================== 集保分散表 ====================

    public Map<String, Object> getShareholdingDistribution(String symbol, LocalDate date) {
        LocalDate friday = findNearestFriday(date);
        ensureShareholdingDistributionData(symbol, friday, friday);

        List<ShareholdingDistributionData> levels = shareholdingDistributionRepository
                .findByStockSymbolAndDataDateOrderByHoldingSharesLevel(symbol, friday)
                .stream()
                .map(this::toShareholdingDistributionDto)
                .collect(Collectors.toList());

        ShareholdingDistributionSummary summary = buildDistributionSummary(symbol, friday, levels);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("summary", summary);
        result.put("levels", levels);
        return result;
    }

    public List<ShareholdingDistributionSummary> getShareholdingDistributionTrend(
            String symbol, LocalDate start, LocalDate end) {
        ensureShareholdingDistributionData(symbol, start, end);

        List<ShareholdingDistribution> allData = shareholdingDistributionRepository
                .findByStockSymbolAndDataDateBetweenOrderByDataDateAscHoldingSharesLevelAsc(symbol, start, end);

        // 按日期分組計算摘要
        Map<LocalDate, List<ShareholdingDistribution>> groupedByDate = allData.stream()
                .collect(Collectors.groupingBy(ShareholdingDistribution::getDataDate));

        return groupedByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<ShareholdingDistributionData> levels = entry.getValue().stream()
                            .map(this::toShareholdingDistributionDto)
                            .collect(Collectors.toList());
                    return buildDistributionSummary(symbol, entry.getKey(), levels);
                })
                .collect(Collectors.toList());
    }

    private void ensureShareholdingDistributionData(String symbol, LocalDate start, LocalDate end) {
        LocalDate fridayStart = findNearestFriday(start);
        LocalDate fridayEnd = findNearestFriday(end);

        if (shareholdingDistributionRepository.existsByStockSymbolAndDataDate(symbol, fridayEnd)
                || shareholdingDistributionRepository.existsByStockSymbolAndDataDate(symbol, fridayStart)) {
            return;
        }
        log.info("Fetching shareholding distribution from FinMind - symbol={}, start={}, end={}", symbol, start, end);
        List<ShareholdingDistributionData> apiData = finMindApiClient.getShareholdingDistribution(
                symbol, start.format(DATE_FORMATTER), end.format(DATE_FORMATTER));
        for (ShareholdingDistributionData data : apiData) {
            try {
                shareholdingDistributionRepository.save(toShareholdingDistributionEntity(data));
            } catch (DataIntegrityViolationException e) {
                log.debug("Duplicate shareholding distribution skipped - symbol={}, date={}, level={}",
                        symbol, data.getDate(), data.getHoldingSharesLevel());
            }
        }
    }

    /**
     * 找最近的週五（集保分散表以週五為基準日）
     */
    private LocalDate findNearestFriday(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.FRIDAY) return date;
        if (dow == DayOfWeek.SATURDAY) return date.minusDays(1);
        if (dow == DayOfWeek.SUNDAY) return date.minusDays(2);
        // 週一到週四，找上一個週五
        return date.with(TemporalAdjusters.previous(DayOfWeek.FRIDAY));
    }

    /**
     * 建立集保分散彙總（大戶 = 400張以上）
     * 常見級距: 1-999, 1,000-5,000, 5,001-10,000, 10,001-15,000, 15,001-20,000, ...
     * 400張 = 400,000股
     */
    private ShareholdingDistributionSummary buildDistributionSummary(
            String symbol, LocalDate date, List<ShareholdingDistributionData> levels) {

        long totalPeople = 0;
        long totalUnit = 0;
        BigDecimal majorPercent = BigDecimal.ZERO;
        BigDecimal retailPercent = BigDecimal.ZERO;
        long majorPeople = 0;
        long retailPeople = 0;

        for (ShareholdingDistributionData level : levels) {
            long people = level.getPeople() != null ? level.getPeople() : 0;
            long unit = level.getUnit() != null ? level.getUnit() : 0;
            BigDecimal pct = level.getPercent() != null ? level.getPercent() : BigDecimal.ZERO;

            totalPeople += people;
            totalUnit += unit;

            if (isMajorHolder(level.getHoldingSharesLevel())) {
                majorPercent = majorPercent.add(pct);
                majorPeople += people;
            } else {
                retailPercent = retailPercent.add(pct);
                retailPeople += people;
            }
        }

        return ShareholdingDistributionSummary.builder()
                .symbol(symbol)
                .date(date)
                .totalPeople(totalPeople)
                .totalUnit(totalUnit)
                .majorHolderPercent(majorPercent.setScale(2, RoundingMode.HALF_UP))
                .retailHolderPercent(retailPercent.setScale(2, RoundingMode.HALF_UP))
                .majorHolderPeople(majorPeople)
                .retailHolderPeople(retailPeople)
                .build();
    }

    /**
     * 判斷是否為大戶（持股 400,000 股以上的級距）
     */
    private boolean isMajorHolder(String level) {
        if (level == null) return false;
        try {
            // 取級距的下限數字
            String cleaned = level.replace(",", "").trim();
            String[] parts = cleaned.split("-");
            if (parts.length > 0) {
                long lowerBound = Long.parseLong(parts[0].trim());
                return lowerBound >= 400_000;
            }
        } catch (NumberFormatException e) {
            // 可能是 "more than 1,000,001" 之類的特殊格式，視為大戶
            if (level.toLowerCase().contains("more") || level.contains("以上")) {
                return true;
            }
        }
        return false;
    }

    private ShareholdingDistributionData toShareholdingDistributionDto(ShareholdingDistribution entity) {
        return ShareholdingDistributionData.builder()
                .symbol(entity.getStockSymbol())
                .date(entity.getDataDate())
                .holdingSharesLevel(entity.getHoldingSharesLevel())
                .people(entity.getPeople())
                .percent(entity.getPercent())
                .unit(entity.getUnit())
                .build();
    }

    private ShareholdingDistribution toShareholdingDistributionEntity(ShareholdingDistributionData dto) {
        return ShareholdingDistribution.builder()
                .stockSymbol(dto.getSymbol())
                .dataDate(dto.getDate())
                .holdingSharesLevel(dto.getHoldingSharesLevel())
                .people(dto.getPeople())
                .percent(dto.getPercent())
                .unit(dto.getUnit())
                .build();
    }
}
