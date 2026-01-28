package com.acenexus.tata.nexusbot.client;

import com.acenexus.tata.nexusbot.config.FugleConfig;
import com.acenexus.tata.nexusbot.dto.TickData;
import com.acenexus.tata.nexusbot.dto.TickType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Fugle WebSocket 客戶端
 * 提供即時成交明細推送
 * 免費限制：最多訂閱 5 檔股票
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FugleWebSocketClient {

    private final FugleConfig fugleConfig;
    private final ObjectMapper objectMapper;

    private WebSocketSession session;
    private final Map<String, Consumer<TickData>> subscribers = new ConcurrentHashMap<>();
    private final Set<String> subscribedSymbols = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private volatile boolean connecting = false;

    @PostConstruct
    public void init() {
        if (!fugleConfig.isEnabled()) {
            log.warn("Fugle API Key 未設定，WebSocket 功能停用");
            return;
        }
        connect();
    }

    @PreDestroy
    public void cleanup() {
        scheduler.shutdown();
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (Exception e) {
                log.warn("關閉 Fugle WebSocket 連線失敗", e);
            }
        }
    }

    /**
     * 建立 WebSocket 連線
     */
    public void connect() {
        if (!fugleConfig.isEnabled()) {
            return;
        }

        if (connecting) {
            log.debug("Fugle WebSocket 正在連線中...");
            return;
        }

        connecting = true;
        StandardWebSocketClient client = new StandardWebSocketClient();

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("X-API-KEY", fugleConfig.getApiKey());

        try {
            client.execute(new WebSocketHandler() {
                @Override
                public void afterConnectionEstablished(WebSocketSession sess) {
                    session = sess;
                    connecting = false;
                    log.info("Fugle WebSocket 連線成功");
                    resubscribeAll();
                }

                @Override
                public void handleMessage(WebSocketSession sess, WebSocketMessage<?> message) {
                    handleIncomingMessage(message.getPayload().toString());
                }

                @Override
                public void handleTransportError(WebSocketSession sess, Throwable exception) {
                    log.error("Fugle WebSocket 傳輸錯誤: {}", exception.getMessage());
                    connecting = false;
                }

                @Override
                public void afterConnectionClosed(WebSocketSession sess, CloseStatus status) {
                    log.warn("Fugle WebSocket 連線關閉: {}，準備重連...", status);
                    connecting = false;
                    scheduleReconnect();
                }

                @Override
                public boolean supportsPartialMessages() {
                    return false;
                }
            }, headers, URI.create(fugleConfig.getWebsocketUrl())).get(10, TimeUnit.SECONDS);

        } catch (Exception e) {
            log.error("Fugle WebSocket 連線失敗: {}", e.getMessage());
            connecting = false;
            scheduleReconnect();
        }
    }

    /**
     * 訂閱股票即時成交
     *
     * @param symbol   股票代號
     * @param callback 成交回調
     * @return 是否訂閱成功
     */
    public boolean subscribe(String symbol, Consumer<TickData> callback) {
        if (!fugleConfig.isEnabled()) {
            log.warn("Fugle 未啟用，無法訂閱 {}", symbol);
            return false;
        }

        if (subscribedSymbols.size() >= fugleConfig.getMaxSubscriptions()) {
            log.warn("已達 Fugle 訂閱上限 ({})，無法訂閱 {}", fugleConfig.getMaxSubscriptions(), symbol);
            return false;
        }

        subscribers.put(symbol, callback);
        subscribedSymbols.add(symbol);

        if (isConnected()) {
            sendSubscribe(symbol);
        }

        return true;
    }

    /**
     * 取消訂閱
     */
    public void unsubscribe(String symbol) {
        subscribers.remove(symbol);
        subscribedSymbols.remove(symbol);

        if (isConnected()) {
            try {
                String msg = String.format("{\"event\":\"unsubscribe\",\"data\":{\"channel\":\"trades\",\"symbol\":\"%s\"}}", symbol);
                session.sendMessage(new TextMessage(msg));
                log.info("已取消訂閱 {} 即時成交", symbol);
            } catch (Exception e) {
                log.error("取消訂閱失敗: {}", symbol, e);
            }
        }
    }

    /**
     * 檢查是否已連線
     */
    public boolean isConnected() {
        return session != null && session.isOpen();
    }

    /**
     * 檢查是否已訂閱
     */
    public boolean isSubscribed(String symbol) {
        return subscribedSymbols.contains(symbol);
    }

    /**
     * 取得已訂閱數量
     */
    public int getSubscribedCount() {
        return subscribedSymbols.size();
    }

    /**
     * 取得剩餘可訂閱數量
     */
    public int getRemainingSlots() {
        return fugleConfig.getMaxSubscriptions() - subscribedSymbols.size();
    }

    /**
     * 發送訂閱訊息
     */
    private void sendSubscribe(String symbol) {
        try {
            String msg = String.format("{\"event\":\"subscribe\",\"data\":{\"channel\":\"trades\",\"symbol\":\"%s\"}}", symbol);
            session.sendMessage(new TextMessage(msg));
            log.info("已訂閱 {} 即時成交 (Fugle)", symbol);
        } catch (Exception e) {
            log.error("訂閱失敗: {}", symbol, e);
        }
    }

    /**
     * 重新訂閱所有股票
     */
    private void resubscribeAll() {
        for (String symbol : subscribedSymbols) {
            sendSubscribe(symbol);
        }
        log.info("重新訂閱 {} 檔股票", subscribedSymbols.size());
    }

    /**
     * 處理收到的訊息
     */
    private void handleIncomingMessage(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String event = node.path("event").asText();

            if ("data".equals(event)) {
                String channel = node.path("channel").asText();

                if ("trades".equals(channel)) {
                    String symbol = node.path("symbol").asText();
                    JsonNode data = node.path("data");

                    TickData tick = parseTickData(symbol, data);

                    Consumer<TickData> callback = subscribers.get(symbol);
                    if (callback != null) {
                        callback.accept(tick);
                    }
                }
            } else if ("error".equals(event)) {
                log.error("Fugle WebSocket 錯誤: {}", node.path("data").asText());
            }
        } catch (Exception e) {
            log.error("解析 Fugle 訊息失敗: {}", payload, e);
        }
    }

    /**
     * 解析成交資料
     */
    private TickData parseTickData(String symbol, JsonNode data) {
        BigDecimal price = new BigDecimal(data.path("price").asText("0"));
        BigDecimal bid = new BigDecimal(data.path("bid").asText("0"));
        BigDecimal ask = new BigDecimal(data.path("ask").asText("0"));

        // 判斷內外盤
        TickType tickType;
        if (ask.compareTo(BigDecimal.ZERO) > 0 && price.compareTo(ask) >= 0) {
            tickType = TickType.BUY;
        } else if (bid.compareTo(BigDecimal.ZERO) > 0 && price.compareTo(bid) <= 0) {
            tickType = TickType.SELL;
        } else {
            tickType = TickType.NEUTRAL;
        }

        String timeStr = data.path("at").asText();
        LocalDateTime time = LocalDateTime.now();
        try {
            if (timeStr != null && !timeStr.isEmpty()) {
                time = LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            }
        } catch (Exception e) {
            log.debug("解析時間失敗: {}", timeStr);
        }

        return TickData.builder()
                .symbol(symbol)
                .time(time)
                .price(price)
                .volume(data.path("volume").asInt(0))
                .serial(data.path("serial").asLong(0))
                .bidPrice(bid)
                .askPrice(ask)
                .tickType(tickType)
                .source("FUGLE")
                .build();
    }

    /**
     * 排程重連
     */
    private void scheduleReconnect() {
        scheduler.schedule(this::connect, fugleConfig.getReconnectDelay(), TimeUnit.MILLISECONDS);
    }
}
