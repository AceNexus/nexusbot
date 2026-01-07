package com.acenexus.tata.nexusbot.controller;

import com.acenexus.tata.nexusbot.client.FinMindApiClient;
import com.acenexus.tata.nexusbot.dto.CandlestickData;
import com.acenexus.tata.nexusbot.dto.CandlestickWithMA;
import com.acenexus.tata.nexusbot.dto.InstitutionalInvestorsData;
import com.acenexus.tata.nexusbot.dto.StockInfo;
import com.acenexus.tata.nexusbot.dto.StockSymbolDto;
import com.acenexus.tata.nexusbot.dto.TechnicalAnalysisResult;
import com.acenexus.tata.nexusbot.enums.StockMarket;
import com.acenexus.tata.nexusbot.service.StockSymbolService;
import com.acenexus.tata.nexusbot.service.TechnicalAnalysisService;
import com.acenexus.tata.nexusbot.service.stock.StockService;
import com.acenexus.tata.nexusbot.util.MovingAverageCalculator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Tag(name = "Stock API", description = "股票資訊查詢接口")
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final List<StockService> stockServices;
    private final FinMindApiClient finMindApiClient;
    private final TechnicalAnalysisService technicalAnalysisService;
    private final StockSymbolService stockSymbolService;

    @Operation(summary = "查詢台股K線圖數據（含均線，優化版）", description = """
            查詢台灣上市公司的歷史K線數據，並包含計算好的移動平均線。

            **支援輸入格式**:
            - 股票代號（如 "2330"）
            - 股票中文名稱（如 "台積電"）
            - 股票英文名稱（如 "TSMC"）

            **優化特點**:
            - 基於更長的歷史數據計算均線，確保準確性
            - 只返回需要顯示的月數，減少數據傳輸
            - 均線已在後端計算完成，前端無需處理

            **均線說明**:
            - MA5: 5日移動平均線
            - MA10: 10日移動平均線
            - MA20: 20日移動平均線
            - MA60: 60日移動平均線

            **參數說明**:
            - displayMonths: 顯示月數（前端顯示的數據範圍）
            - maBaseMonths: 均線計算基準月數（用於計算均線的歷史數據長度）

            **注意事項**:
            - maBaseMonths 必須大於等於 displayMonths
            - 建議 maBaseMonths 至少為 3 個月以確保 MA60 準確
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查詢成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CandlestickWithMA.class))),
            @ApiResponse(responseCode = "400", description = "參數錯誤", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "查無此股票代號或無歷史數據", content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{symbol}/candlestick-with-ma")
    public ResponseEntity<List<CandlestickWithMA>> getStockCandlestickWithMA(
            @Parameter(description = "台股代號或名稱（代號如2330或名稱如台積電）", example = "2330") @PathVariable String symbol,
            @Parameter(description = "顯示月數（1-12個月，預設6個月）", example = "6") @RequestParam(defaultValue = "6") int displayMonths,
            @Parameter(description = "均線計算基準月數（3-24個月，預設12個月）", example = "12") @RequestParam(defaultValue = "12") int maBaseMonths) {

        log.info("K-line with MA query - input={}, displayMonths={}, maBaseMonths={}",
                symbol, displayMonths, maBaseMonths);

        // 解析輸入：可能是股票代號或名稱
        String resolvedSymbol = stockSymbolService.resolveSymbol(symbol, StockMarket.TW);
        log.debug("Resolved symbol - input={}, resolved={}", symbol, resolvedSymbol);

        // 參數驗證
        if (displayMonths < 1 || displayMonths > 12) {
            log.warn("Invalid displayMonths parameter - input={}, displayMonths={}", symbol, displayMonths);
            return ResponseEntity.badRequest().build();
        }

        if (maBaseMonths < 3 || maBaseMonths > 24) {
            log.warn("Invalid maBaseMonths parameter - input={}, maBaseMonths={}", symbol, maBaseMonths);
            return ResponseEntity.badRequest().build();
        }

        if (maBaseMonths < displayMonths) {
            log.warn("maBaseMonths must be >= displayMonths - input={}, displayMonths={}, maBaseMonths={}",
                    symbol, displayMonths, maBaseMonths);
            return ResponseEntity.badRequest().build();
        }

        try {
            // 1. 查詢完整的歷史數據（用於計算均線）
            List<CandlestickData> fullData = finMindApiClient.getRecentMonthsKLine(resolvedSymbol,
                    maBaseMonths);

            if (fullData.isEmpty()) {
                log.warn("No K-line data found - symbol={}", symbol);
                return ResponseEntity.notFound().build();
            }

            // 2. 計算需要返回的數據起始索引（只返回最近 displayMonths 的數據）
            int startIndex = Math.max(0, fullData.size() - (displayMonths * 22)); // 假設每月約 22 個交易日

            // 3. 批量計算 EMA 序列（使用完整數據以確保準確性）
            List<BigDecimal> ema5Series = MovingAverageCalculator.calculateEMASeries(fullData, 5);
            List<BigDecimal> ema10Series = MovingAverageCalculator.calculateEMASeries(fullData, 10);
            List<BigDecimal> ema20Series = MovingAverageCalculator.calculateEMASeries(fullData, 20);
            List<BigDecimal> ema60Series = MovingAverageCalculator.calculateEMASeries(fullData, 60);

            // 4. 計算均線並構建返回數據
            List<CandlestickWithMA> result = new java.util.ArrayList<>();
            for (int i = startIndex; i < fullData.size(); i++) {
                CandlestickData candle = fullData.get(i);

                result.add(CandlestickWithMA.builder()
                        .symbol(candle.getSymbol())
                        .date(candle.getDate())
                        .open(candle.getOpen())
                        .high(candle.getHigh())
                        .low(candle.getLow())
                        .close(candle.getClose())
                        .volume(candle.getVolume())
                        .turnover(candle.getTurnover())
                        .change(candle.getChange())
                        .changePercent(candle.getChangePercent())
                        .ma5(MovingAverageCalculator.calculateMA5(fullData, i))
                        .ma10(MovingAverageCalculator.calculateMA10(fullData, i))
                        .ma20(MovingAverageCalculator.calculateMA20(fullData, i))
                        .ma60(MovingAverageCalculator.calculateMA60(fullData, i))
                        .ema5(ema5Series.get(i))
                        .ema10(ema10Series.get(i))
                        .ema20(ema20Series.get(i))
                        .ema60(ema60Series.get(i))
                        .build());
            }

            log.info("K-line with MA data retrieved - symbol={}, returned={}, baseData={}, displayMonths={}, maBaseMonths={}",
                    symbol, result.size(), fullData.size(), displayMonths, maBaseMonths);

            return ResponseEntity.ok(result);

        } catch (Exception ex) {
            log.error("K-line with MA query error - symbol={}, displayMonths={}, maBaseMonths={}, error={}",
                    symbol, displayMonths, maBaseMonths, ex.getMessage(), ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "技術分析", description = """
            對台股進行完整技術分析，提供買賣建議。

            **支援輸入格式**:
            - 股票代號（如 "2330"）
            - 股票中文名稱（如 "台積電"）
            - 股票英文名稱（如 "TSMC"）

            **分析內容包括**:
            - RSI (相對強弱指標)
            - KD (隨機指標)
            - MACD (指數平滑異同移動平均線)
            - 布林帶 (Bollinger Bands)
            - 移動平均線 (MA5/MA10/MA20/MA60)
            - 買賣訊號分析
            - 支撐壓力位
            - 綜合操作建議

            **注意事項**:
            - 需要至少 3 個月以上的歷史數據
            - 分析結果僅供參考，不構成投資建議
            - 投資有風險，請審慎評估
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "分析成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TechnicalAnalysisResult.class))),
            @ApiResponse(responseCode = "400", description = "數據不足或參數錯誤", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "查無此股票代號", content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{symbol}/analysis")
    public ResponseEntity<TechnicalAnalysisResult> getTechnicalAnalysis(
            @Parameter(description = "台股代號或名稱（代號如2330或名稱如台積電）", example = "2330") @PathVariable String symbol) {

        log.info("Technical analysis request - input={}", symbol);

        // 解析輸入：可能是股票代號或名稱
        String resolvedSymbol = stockSymbolService.resolveSymbol(symbol, StockMarket.TW);
        log.debug("Resolved symbol - input={}, resolved={}", symbol, resolvedSymbol);

        try {
            // 查詢至少 3 個月的數據用於技術分析
            List<CandlestickData> data = finMindApiClient.getRecentMonthsKLine(resolvedSymbol, 3);

            if (data.isEmpty()) {
                log.warn("No data found for analysis - symbol={}", symbol);
                return ResponseEntity.notFound().build();
            }

            if (data.size() < 60) {
                log.warn("Insufficient data for analysis - symbol={}, dataPoints={}", symbol,
                        data.size());
                return ResponseEntity.badRequest().build();
            }

            // 獲取股票中文名稱
            String fetchedName = resolvedSymbol; // 預設使用股票代號
            try {
                // 找到支援台股的服務
                Optional<StockService> serviceOpt = stockServices.stream()
                        .filter(service -> service.supports(StockMarket.TW))
                        .findFirst();

                if (serviceOpt.isPresent()) {
                    StockService stockService = serviceOpt.get();
                    Optional<StockInfo> stockInfoOpt = stockService
                            .getStockInfo(resolvedSymbol, StockMarket.TW).join();
                    if (stockInfoOpt.isPresent()) {
                        fetchedName = stockInfoOpt.get().getName();
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to fetch stock name - symbol={}, using symbol as fallback",
                        resolvedSymbol);
            }
            final String stockName = fetchedName;

            // 將股票名稱設定到所有K線數據中
            data.forEach(candle -> candle.setName(stockName));

            // 執行技術分析
            TechnicalAnalysisResult result = technicalAnalysisService.analyze(symbol, data);

            log.info("Technical analysis completed - symbol={}, recommendation={}, confidence={}", symbol,
                    result.getRecommendation(), result.getConfidence());

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.warn("Analysis validation error - symbol={}, error={}", symbol, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception ex) {
            log.error("Technical analysis error - symbol={}, error={}", symbol, ex.getMessage(), ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "取得所有台股列表", description = """
            取得所有上市上櫃股票代號與名稱列表。
            資料來源：FinMind
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查詢成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StockSymbolDto.class)))
    })
    @GetMapping("/list")
    public ResponseEntity<List<StockSymbolDto>> getStockList() {
        log.info("Stock list query requested");

        List<StockSymbolDto> stocks = stockSymbolService.getAllStocks();

        log.info("Returning {} stocks", stocks.size());
        return ResponseEntity.ok(stocks);
    }

    @Operation(summary = "查詢法人進出", description = """
            查詢台股三大法人（外資、投信、自營商）買賣超數據。

            **支援輸入格式**:
            - 股票代號（如 "2330"）
            - 股票中文名稱（如 "台積電"）
            - 股票英文名稱（如 "TSMC"）

            **參數說明**:
            - days: 查詢最近幾天的數據（預設 30 天，最多 90 天）

            **注意事項**:
            - 數據單位為「張」（1 張 = 1000 股）
            - 正值表示買超，負值表示賣超
            - 數據來源：FinMind 台灣金融資料庫
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查詢成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = InstitutionalInvestorsData.class))),
            @ApiResponse(responseCode = "400", description = "參數錯誤", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "查無此股票代號或無數據", content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{symbol}/institutional-investors")
    public ResponseEntity<List<InstitutionalInvestorsData>> getInstitutionalInvestors(
            @Parameter(description = "台股代號或名稱（代號如2330或名稱如台積電）", example = "2330") @PathVariable String symbol,
            @Parameter(description = "查詢天數（1-90天，預設30天）", example = "30") @RequestParam(defaultValue = "30") int days) {

        log.info("Institutional investors query - input={}, days={}", symbol, days);

        // 解析輸入：可能是股票代號或名稱
        String resolvedSymbol = stockSymbolService.resolveSymbol(symbol, StockMarket.TW);
        log.debug("Resolved symbol - input={}, resolved={}", symbol, resolvedSymbol);

        // 參數驗證
        if (days < 1 || days > 90) {
            log.warn("Invalid days parameter - input={}, days={}", symbol, days);
            return ResponseEntity.badRequest().build();
        }

        try {
            List<InstitutionalInvestorsData> data = finMindApiClient.getRecentDaysInstitutionalInvestors(
                    resolvedSymbol, days);

            if (data.isEmpty()) {
                log.warn("No institutional investors data found - symbol={}", symbol);
                return ResponseEntity.notFound().build();
            }

            log.info("Institutional investors data retrieved - symbol={}, count={}", symbol, data.size());
            return ResponseEntity.ok(data);

        } catch (Exception ex) {
            log.error("Institutional investors query error - symbol={}, days={}, error={}",
                    symbol, days, ex.getMessage(), ex);
            return ResponseEntity.internalServerError().build();
        }
    }

}
