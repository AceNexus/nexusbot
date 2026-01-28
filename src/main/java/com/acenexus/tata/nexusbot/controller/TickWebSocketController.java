package com.acenexus.tata.nexusbot.controller;

import com.acenexus.tata.nexusbot.dto.TickData;
import com.acenexus.tata.nexusbot.dto.TickStats;
import com.acenexus.tata.nexusbot.enums.StockMarket;
import com.acenexus.tata.nexusbot.service.RealtimeTickService;
import com.acenexus.tata.nexusbot.service.StockSymbolService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 即時成交 WebSocket 控制器
 * 處理訂閱/取消訂閱請求，並廣播成交數據
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class TickWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final RealtimeTickService realtimeTickService;
    private final StockSymbolService stockSymbolService;

    @PostConstruct
    public void init() {
        // 註冊 Tick 廣播監聽器
        realtimeTickService.addTickBroadcastListener(this::broadcastTick);
        // 註冊大單廣播監聽器
        realtimeTickService.addBigTradeListener(this::broadcastBigTrade);
        log.info("TickWebSocketController 已註冊廣播監聽器");
    }

    /**
     * 同步目前監控狀態 (新訪客進入時調用)
     * 前端發送到: /app/tick/sync
     */
    @MessageMapping("/tick/sync")
    public void syncStatus() {
        Set<String> symbols = realtimeTickService.getMonitoredSymbols();

        Map<String, Object> response = new HashMap<>();
        response.put("type", "SYNC");
        response.put("symbols", symbols);
        response.put("subscribed", realtimeTickService.getSubscribedCount());
        response.put("maxSubscriptions", realtimeTickService.getMaxSubscriptions());

        // 回傳給所有人（包括剛連線的）
        messagingTemplate.convertAndSend("/topic/tick-status", response);

        log.info("[Tick] 同步狀態: {} 檔監控中", symbols.size());
    }

    /**
     * 處理訂閱請求 (全局共享模式)
     * 前端發送到: /app/tick/subscribe/{symbol}
     */
    @MessageMapping("/tick/subscribe/{symbol}")
    public void subscribe(@DestinationVariable String symbol) {
        try {
            String resolvedSymbol = stockSymbolService.resolveSymbol(symbol, StockMarket.TW);
            String result = realtimeTickService.startMonitor(resolvedSymbol);

            // 廣播訂閱結果給所有人
            Map<String, Object> response = Map.of(
                    "type", "SUBSCRIBE_RESULT",
                    "symbol", resolvedSymbol,
                    "result", result,
                    "subscribed", realtimeTickService.getSubscribedCount(),
                    "maxSubscriptions", realtimeTickService.getMaxSubscriptions()
            );

            messagingTemplate.convertAndSend("/topic/ticks/" + resolvedSymbol, response);
            // 廣播到全局頻道，讓所有人更新訂閱狀態
            messagingTemplate.convertAndSend("/topic/tick-status", response);

            log.info("[Tick] 訂閱: {} -> {}", symbol, result);

            // 如果訂閱成功，發送當前統計
            if ("FUGLE".equals(result)) {
                TickStats stats = realtimeTickService.getRealtimeStats(resolvedSymbol);
                broadcastStats(resolvedSymbol, stats);
            }
        } catch (Exception e) {
            log.error("[Tick] 訂閱失敗: {}", symbol, e);
        }
    }

    /**
     * 處理取消訂閱請求 (全局共享模式)
     * 前端發送到: /app/tick/unsubscribe/{symbol}
     */
    @MessageMapping("/tick/unsubscribe/{symbol}")
    public void unsubscribe(@DestinationVariable String symbol) {
        try {
            String resolvedSymbol = stockSymbolService.resolveSymbol(symbol, StockMarket.TW);
            realtimeTickService.stopMonitor(resolvedSymbol);

            // 廣播取消訂閱結果給所有人
            Map<String, Object> response = Map.of(
                    "type", "UNSUBSCRIBE_RESULT",
                    "symbol", resolvedSymbol,
                    "subscribed", realtimeTickService.getSubscribedCount(),
                    "maxSubscriptions", realtimeTickService.getMaxSubscriptions()
            );

            messagingTemplate.convertAndSend("/topic/ticks/" + resolvedSymbol, response);
            // 廣播到全局頻道，讓所有人更新訂閱狀態
            messagingTemplate.convertAndSend("/topic/tick-status", response);

            log.info("[Tick] 取消訂閱: {}", symbol);
        } catch (Exception e) {
            log.error("[Tick] 取消訂閱失敗: {}", symbol, e);
        }
    }

    /**
     * 廣播成交數據
     */
    private void broadcastTick(String symbol, TickData tick) {
        Map<String, Object> message = Map.of(
                "type", "TICK",
                "symbol", symbol,
                "data", tick
        );
        messagingTemplate.convertAndSend("/topic/ticks/" + symbol, message);
    }

    /**
     * 廣播統計數據
     */
    private void broadcastStats(String symbol, TickStats stats) {
        Map<String, Object> message = Map.of(
                "type", "STATS",
                "symbol", symbol,
                "data", stats
        );
        messagingTemplate.convertAndSend("/topic/ticks/" + symbol, message);
    }

    /**
     * 廣播大單警示
     */
    private void broadcastBigTrade(String symbol, TickData tick, int lots) {
        Map<String, Object> message = Map.of(
                "type", "BIG_TRADE",
                "symbol", symbol,
                "data", tick,
                "lots", lots
        );
        // 廣播到特定股票頻道
        messagingTemplate.convertAndSend("/topic/ticks/" + symbol, message);
        // 同時廣播到大單專用頻道
        messagingTemplate.convertAndSend("/topic/big-trades", message);
    }

}
