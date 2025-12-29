package com.acenexus.tata.nexusbot.controller;

import com.acenexus.tata.nexusbot.dto.StockInfo;
import com.acenexus.tata.nexusbot.enums.StockMarket;
import com.acenexus.tata.nexusbot.service.stock.StockService;
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

}
