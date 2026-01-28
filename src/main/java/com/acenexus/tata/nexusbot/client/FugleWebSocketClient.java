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
    private volatile boolean initialConnection = true;  // 標記是否為首次啟動
    private volatile boolean authenticated = false;     // 標記是否已驗證

    @PostConstruct
    public void init() {
        if (!fugleConfig.isEnabled()) {
            log.warn("[Fugle] API Key 未設定，WebSocket 功能停用");
            return;
        }
        connect();
    }

    @PreDestroy
    public void cleanup() {
        log.info("[Fugle] 服務關閉中，清理訂閱...");

        // 先取消所有訂閱
        if (session != null && session.isOpen() && !subscribedSymbols.isEmpty()) {
            for (String symbol : subscribedSymbols) {
                try {
                    String msg = String.format("{\"event\":\"unsubscribe\",\"data\":{\"channel\":\"trades\",\"symbol\":\"%s\"}}", symbol);
                    session.sendMessage(new TextMessage(msg));
                    log.info("[Fugle] 已取消訂閱 {}", symbol);
                } catch (Exception e) {
                    log.warn("[Fugle] 取消訂閱 {} 失敗: {}", symbol, e.getMessage());
                }
            }
            // 等待取消訂閱完成
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 清空本地狀態
        subscribers.clear();
        subscribedSymbols.clear();

        // 關閉排程器
        scheduler.shutdown();

        // 關閉連線
        if (session != null && session.isOpen()) {
            try {
                session.close();
                log.info("[Fugle] 連線已關閉");
            } catch (Exception e) {
                log.warn("[Fugle] 關閉連線失敗", e);
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
            log.debug("[Fugle] 正在連線中...");
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
                    authenticated = false;

                    // 發送身份驗證請求
                    try {
                        String authMsg = String.format("{\"event\":\"auth\",\"data\":{\"apikey\":\"%s\"}}", fugleConfig.getApiKey());
                        sess.sendMessage(new TextMessage(authMsg));
                        log.info("[Fugle] 連線成功，等待身份驗證...");
                    } catch (Exception e) {
                        log.error("[Fugle] 發送身份驗證失敗: {}", e.getMessage());
                    }
                    // 訂閱會在收到 authenticated 事件後進行
                }

                @Override
                public void handleMessage(WebSocketSession sess, WebSocketMessage<?> message) {
                    handleIncomingMessage(message.getPayload().toString());
                }

                @Override
                public void handleTransportError(WebSocketSession sess, Throwable exception) {
                    String errorType = exception.getClass().getSimpleName();
                    String errorMsg = exception.getMessage();
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = "無詳細訊息";
                    }
                    log.error("[Fugle] 傳輸錯誤 ({}: {})", errorType, errorMsg);
                    connecting = false;
                }

                @Override
                public void afterConnectionClosed(WebSocketSession sess, CloseStatus status) {
                    log.warn("[Fugle] 連線關閉 (狀態碼: {}, 原因: {})，{}秒後重連",
                            status.getCode(),
                            status.getReason() != null ? status.getReason() : "無",
                            fugleConfig.getReconnectDelay() / 1000);
                    connecting = false;
                    scheduleReconnect();
                }

                @Override
                public boolean supportsPartialMessages() {
                    return false;
                }
            }, headers, URI.create(fugleConfig.getWebsocketUrl())).get(10, TimeUnit.SECONDS);

        } catch (Exception e) {
            log.error("[Fugle] 連線失敗 ({}: {})，{}秒後重連",
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    fugleConfig.getReconnectDelay() / 1000);
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
            log.warn("[Fugle] 未啟用，無法訂閱 {}", symbol);
            return false;
        }

        if (subscribedSymbols.size() >= fugleConfig.getMaxSubscriptions()) {
            log.warn("[Fugle] 已達訂閱上限 ({}/{}), 無法訂閱 {}", subscribedSymbols.size(), fugleConfig.getMaxSubscriptions(), symbol);
            return false;
        }

        subscribers.put(symbol, callback);
        subscribedSymbols.add(symbol);

        if (isConnected() && authenticated) {
            sendSubscribe(symbol);
        } else {
            log.info("[Fugle] 等待連線/驗證完成後訂閱 {}", symbol);
        }

        return true;
    }

    /**
     * 取消訂閱
     */
    public void unsubscribe(String symbol) {
        subscribers.remove(symbol);
        subscribedSymbols.remove(symbol);

        if (isConnected() && authenticated) {
            try {
                String msg = String.format("{\"event\":\"unsubscribe\",\"data\":{\"channel\":\"trades\",\"symbol\":\"%s\"}}", symbol);
                session.sendMessage(new TextMessage(msg));
                log.info("[Fugle] 已取消訂閱 {} (剩餘: {})", symbol, subscribedSymbols.size());
            } catch (Exception e) {
                log.error("[Fugle] 取消訂閱失敗: {}", symbol, e);
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
            log.info("[Fugle] 已訂閱 {} ({}/{})", symbol, subscribedSymbols.size(), fugleConfig.getMaxSubscriptions());
        } catch (Exception e) {
            log.error("[Fugle] 訂閱失敗: {}", symbol, e);
        }
    }

    /**
     * 重新訂閱所有股票
     */
    private void resubscribeAll() {
        for (String symbol : subscribedSymbols) {
            sendSubscribe(symbol);
        }
        log.info("[Fugle] 重新訂閱完成 ({}/{})", subscribedSymbols.size(), fugleConfig.getMaxSubscriptions());
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
            } else if ("authenticated".equals(event)) {
                // 身份驗證成功
                authenticated = true;
                log.info("[Fugle] 身份驗證成功");

                // 根據是否首次連線決定行為
                if (initialConnection) {
                    subscribers.clear();
                    subscribedSymbols.clear();
                    initialConnection = false;
                    log.info("[Fugle] 首次啟動，訂閱數: 0");
                } else {
                    // 斷線重連：重新訂閱現有股票
                    if (!subscribedSymbols.isEmpty()) {
                        log.info("[Fugle] 重連後重新訂閱 {} 檔", subscribedSymbols.size());
                        resubscribeAll();
                    }
                }
            } else if ("error".equals(event)) {
                String errorMsg = node.path("data").path("message").asText();
                if (errorMsg.isEmpty()) {
                    errorMsg = node.path("data").asText();
                }
                if (errorMsg.isEmpty()) {
                    errorMsg = payload;  // 顯示完整原始訊息
                }
                log.error("[Fugle] API 錯誤: {}", errorMsg);
            } else if (!"pong".equals(event) && !"subscribed".equals(event)) {
                // 記錄未知事件類型（排除 pong 和 subscribed）
                log.debug("[Fugle] 未處理事件: {}", payload);
            }
        } catch (Exception e) {
            log.error("[Fugle] 解析訊息失敗: {}", payload, e);
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
