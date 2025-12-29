package com.acenexus.tata.nexusbot.controller;

import com.acenexus.tata.nexusbot.client.FinMindApiClient;
import com.acenexus.tata.nexusbot.dto.CandlestickData;
import com.acenexus.tata.nexusbot.dto.CandlestickWithMA;
import com.acenexus.tata.nexusbot.dto.StockInfo;
import com.acenexus.tata.nexusbot.dto.TechnicalAnalysisResult;
import com.acenexus.tata.nexusbot.enums.StockMarket;
import com.acenexus.tata.nexusbot.service.TechnicalAnalysisService;
import com.acenexus.tata.nexusbot.service.stock.StockService;
import com.acenexus.tata.nexusbot.util.MovingAverageCalculator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Tag(name = "Stock API", description = "股票資訊查詢接口")
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final List<StockService> stockServices;
    private final FinMindApiClient finMindApiClient;
    private final TechnicalAnalysisService technicalAnalysisService;

    @Operation(
            summary = "查詢股票資訊",
            description = """
                    根據股票代號和市場類型查詢即時股票資訊。

                    **支援市場**:
                    - TW: 台灣股市 (預設)
                    - US: 美國股市

                    **台股範例**:
                    - 2330 (台積電)
                    - 2317 (鴻海)
                    - 2454 (聯發科)

                    **美股範例**:
                    - AAPL (蘋果)
                    - GOOGL (Google)
                    - TSLA (特斯拉)

                    **回應欄位**:
                    - currentPrice: 當前價格
                    - change: 漲跌金額
                    - changePercent: 漲跌幅 (%)
                    - volume: 成交量
                    - limitUp: 漲停價
                    - limitDown: 跌停價
                    - bestBidPrice: 最佳買價
                    - bestAskPrice: 最佳賣價
                    - turnover: 成交金額（百萬）
                    - updateTime: 資料更新時間
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "查詢成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StockInfo.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "symbol": "2330",
                                              "name": "台積電",
                                              "currentPrice": 1530.00,
                                              "openPrice": 1515.00,
                                              "highPrice": 1535.00,
                                              "lowPrice": 1510.00,
                                              "previousClose": 1520.00,
                                              "change": 10.00,
                                              "changePercent": 0.66,
                                              "volume": 25000000,
                                              "updateTime": "2025-12-29T13:30:00",
                                              "limitUp": 1660.00,
                                              "limitDown": 1360.00,
                                              "bestBidPrice": 1525.00,
                                              "bestAskPrice": 1530.00,
                                              "turnover": 2346.00
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "查無此股票代號",
                    content = @Content(mediaType = "application/json")
            )
    })
    @GetMapping("/{symbol}")
    public CompletableFuture<ResponseEntity<StockInfo>> getStockInfo(@Parameter(description = "股票代號 (台股:4位數字, 美股:英文代碼)", example = "2330")
                                                                     @PathVariable String symbol,
                                                                     @Parameter(description = "市場類型 (TW=台灣, US=美國)", example = "TW")
                                                                     @RequestParam(defaultValue = "TW") StockMarket market) {

        log.info("Stock query - symbol={}, market={}", symbol, market);

        // 找到支援此市場的服務
        Optional<StockService> serviceOpt = stockServices.stream()
                .filter(service -> service.supports(market))
                .findFirst();

        if (serviceOpt.isEmpty()) {
            log.warn("Stock query failed - market={} not supported", market);
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().build());
        }

        StockService stockService = serviceOpt.get();
        log.debug("Using stock service - service={}", stockService.getClass().getSimpleName());

        return stockService.getStockInfo(symbol, market)
                .thenApply(stockInfoOpt -> {
                    if (stockInfoOpt.isEmpty()) {
                        log.warn("Stock not found - symbol={}, market={}", symbol, market);
                        return ResponseEntity.notFound().<StockInfo>build();
                    }

                    StockInfo stockInfo = stockInfoOpt.get();
                    log.info("Stock found - name={}, symbol={}, price={}, change={}", stockInfo.getName(), symbol, stockInfo.getCurrentPrice(), stockInfo.getChange());
                    return ResponseEntity.ok(stockInfo);
                })
                .exceptionally(ex -> {
                    log.error("Stock query error - symbol={}, market={}, error={}", symbol, market, ex.getMessage(), ex);
                    return ResponseEntity.internalServerError().build();
                });
    }

    @Operation(
            summary = "查詢台股K線圖數據",
            description = """
                    查詢台灣上市公司的歷史K線數據（OHLCV）。

                    **資料來源**: FinMind API（台灣開源金融資料庫）

                    **數據包含**:
                    - 日期 (date)
                    - 開盤價 (open)
                    - 最高價 (high)
                    - 最低價 (low)
                    - 收盤價 (close)
                    - 成交量 (volume) - 單位：張
                    - 成交金額 (turnover) - 單位：千元
                    - 漲跌金額 (change)
                    - 漲跌幅 (changePercent) - 單位：%

                    **範例股票代號**:
                    - 2330 (台積電)
                    - 2317 (鴻海)
                    - 2454 (聯發科)

                    **注意事項**:
                    - 預設查詢最近 6 個月的數據
                    - 可透過 months 參數調整查詢區間（1-24個月）
                    - 數據為日K線（每日收盤後更新）
                    - 資料會被快取 1 天以提升效能
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "查詢成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CandlestickData.class),
                            examples = @ExampleObject(
                                    value = """
                                            [
                                              {
                                                "symbol": "2330",
                                                "date": "2024-12-29",
                                                "open": 550.00,
                                                "high": 555.00,
                                                "low": 548.00,
                                                "close": 553.00,
                                                "volume": 25000,
                                                "turnover": 13825.00,
                                                "change": 3.00,
                                                "changePercent": 0.55
                                              }
                                            ]
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "查無此股票代號或無歷史數據",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "伺服器錯誤或資料來源無法存取",
                    content = @Content(mediaType = "application/json")
            )
    })
    @GetMapping("/{symbol}/candlestick")
    public ResponseEntity<List<CandlestickData>> getStockCandlestick(@Parameter(description = "台股代號（4位數字）", example = "2330")
                                                                     @PathVariable String symbol,
                                                                     @Parameter(description = "查詢月數（1-24個月，預設1個月）", example = "1")
                                                                     @RequestParam(defaultValue = "6") int months) {

        log.info("K-line query - symbol={}, months={}", symbol, months);

        if (months < 1 || months > 24) {
            log.warn("Invalid months parameter - symbol={}, months={}", symbol, months);
            return ResponseEntity.badRequest().build();
        }

        try {
            List<CandlestickData> candlesticks = finMindApiClient.getRecentMonthsKLine(symbol, months);

            if (candlesticks.isEmpty()) {
                log.warn("No K-line data found - symbol={}", symbol);
                return ResponseEntity.notFound().build();
            }

            log.info("K-line data retrieved - symbol={}, count={}, months={}", symbol, candlesticks.size(), months);

            return ResponseEntity.ok(candlesticks);

        } catch (Exception ex) {
            log.error("K-line query error - symbol={}, months={}, error={}", symbol, months, ex.getMessage(), ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
            summary = "查詢台股K線圖數據（含均線，優化版）",
            description = """
                    查詢台灣上市公司的歷史K線數據，並包含計算好的移動平均線。

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
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "查詢成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CandlestickWithMA.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "參數錯誤",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "查無此股票代號或無歷史數據",
                    content = @Content(mediaType = "application/json")
            )
    })
    @GetMapping("/{symbol}/candlestick-with-ma")
    public ResponseEntity<List<CandlestickWithMA>> getStockCandlestickWithMA(
            @Parameter(description = "台股代號（4位數字）", example = "2330")
            @PathVariable String symbol,
            @Parameter(description = "顯示月數（1-12個月，預設6個月）", example = "6")
            @RequestParam(defaultValue = "6") int displayMonths,
            @Parameter(description = "均線計算基準月數（3-24個月，預設12個月）", example = "12")
            @RequestParam(defaultValue = "12") int maBaseMonths) {

        log.info("K-line with MA query - symbol={}, displayMonths={}, maBaseMonths={}",
                symbol, displayMonths, maBaseMonths);

        // 參數驗證
        if (displayMonths < 1 || displayMonths > 12) {
            log.warn("Invalid displayMonths parameter - symbol={}, displayMonths={}", symbol, displayMonths);
            return ResponseEntity.badRequest().build();
        }

        if (maBaseMonths < 3 || maBaseMonths > 24) {
            log.warn("Invalid maBaseMonths parameter - symbol={}, maBaseMonths={}", symbol, maBaseMonths);
            return ResponseEntity.badRequest().build();
        }

        if (maBaseMonths < displayMonths) {
            log.warn("maBaseMonths must be >= displayMonths - symbol={}, displayMonths={}, maBaseMonths={}",
                    symbol, displayMonths, maBaseMonths);
            return ResponseEntity.badRequest().build();
        }

        try {
            // 1. 查詢完整的歷史數據（用於計算均線）
            List<CandlestickData> fullData = finMindApiClient.getRecentMonthsKLine(symbol, maBaseMonths);

            if (fullData.isEmpty()) {
                log.warn("No K-line data found - symbol={}", symbol);
                return ResponseEntity.notFound().build();
            }

            // 2. 計算需要返回的數據起始索引（只返回最近 displayMonths 的數據）
            int startIndex = Math.max(0, fullData.size() - (displayMonths * 22)); // 假設每月約 22 個交易日

            // 3. 計算均線並構建返回數據
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
                        .build());
            }

            log.info("K-line with MA data retrieved - symbol={}, returned={}, baseData={}, displayMonths={}, maBaseMonths={}", symbol, result.size(), fullData.size(), displayMonths, maBaseMonths);

            return ResponseEntity.ok(result);

        } catch (Exception ex) {
            log.error("K-line with MA query error - symbol={}, displayMonths={}, maBaseMonths={}, error={}", symbol, displayMonths, maBaseMonths, ex.getMessage(), ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
            summary = "技術分析",
            description = """
                    對台股進行完整技術分析，提供買賣建議。

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
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "分析成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TechnicalAnalysisResult.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "數據不足或參數錯誤",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "查無此股票代號",
                    content = @Content(mediaType = "application/json")
            )
    })
    @GetMapping("/{symbol}/analysis")
    public ResponseEntity<TechnicalAnalysisResult> getTechnicalAnalysis(
            @Parameter(description = "台股代號（4位數字）", example = "2330")
            @PathVariable String symbol) {

        log.info("Technical analysis request - symbol={}", symbol);

        try {
            // 查詢至少 3 個月的數據用於技術分析
            List<CandlestickData> data = finMindApiClient.getRecentMonthsKLine(symbol, 3);

            if (data.isEmpty()) {
                log.warn("No data found for analysis - symbol={}", symbol);
                return ResponseEntity.notFound().build();
            }

            if (data.size() < 60) {
                log.warn("Insufficient data for analysis - symbol={}, dataPoints={}", symbol, data.size());
                return ResponseEntity.badRequest().build();
            }

            // 獲取股票中文名稱
            String fetchedName = symbol;  // 預設使用股票代號
            try {
                // 找到支援台股的服務
                Optional<StockService> serviceOpt = stockServices.stream()
                        .filter(service -> service.supports(StockMarket.TW))
                        .findFirst();

                if (serviceOpt.isPresent()) {
                    StockService stockService = serviceOpt.get();
                    Optional<StockInfo> stockInfoOpt = stockService.getStockInfo(symbol, StockMarket.TW).join();
                    if (stockInfoOpt.isPresent()) {
                        fetchedName = stockInfoOpt.get().getName();
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to fetch stock name - symbol={}, using symbol as fallback", symbol);
            }
            final String stockName = fetchedName;

            // 將股票名稱設定到所有K線數據中
            data.forEach(candle -> candle.setName(stockName));

            // 執行技術分析
            TechnicalAnalysisResult result = technicalAnalysisService.analyze(symbol, data);

            log.info("Technical analysis completed - symbol={}, recommendation={}, confidence={}", symbol, result.getRecommendation(), result.getConfidence());

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.warn("Analysis validation error - symbol={}, error={}", symbol, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception ex) {
            log.error("Technical analysis error - symbol={}, error={}", symbol, ex.getMessage(), ex);
            return ResponseEntity.internalServerError().build();
        }
    }

}
