package com.acenexus.tata.nexusbot.event.handler;

import com.acenexus.tata.nexusbot.dto.TickData;
import com.acenexus.tata.nexusbot.dto.TickStats;
import com.acenexus.tata.nexusbot.enums.StockMarket;
import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.acenexus.tata.nexusbot.service.RealtimeTickService;
import com.acenexus.tata.nexusbot.service.StockSymbolService;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TextMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 即時成交明細命令處理器
 * 支援命令：
 * - /ticks 股票代號 - 查詢成交統計
 * - /明細 股票代號 - 查詢成交統計
 * - /大單 股票代號 - 查詢大單成交
 * - /monitor 股票代號 - 開始監控
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TickCommandHandler implements LineBotEventHandler {

    private final RealtimeTickService realtimeTickService;
    private final StockSymbolService stockSymbolService;

    @Override
    public boolean canHandle(LineBotEvent event) {
        String text = event.getPayloadString("text");
        if (text == null) {
            return false;
        }

        return text.startsWith("/ticks") ||
                text.startsWith("/明細") ||
                text.startsWith("/大單") ||
                text.startsWith("/tick-monitor");
    }

    @Override
    public Message handle(LineBotEvent event) {
        String text = event.getPayloadString("text");
        String[] parts = text.split("\\s+");

        if (parts.length < 2) {
            return new TextMessage(getHelpMessage());
        }

        String command = parts[0];
        String symbolInput = parts[1];

        // 解析股票代號
        String symbol;
        try {
            symbol = stockSymbolService.resolveSymbol(symbolInput, StockMarket.TW);
        } catch (Exception e) {
            return new TextMessage("找不到股票: " + symbolInput);
        }

        try {
            return switch (command) {
                case "/ticks", "/明細" -> buildTickStatsMessage(symbol);
                case "/大單" -> buildBigTradesMessage(symbol);
                case "/tick-monitor" -> handleMonitorCommand(symbol, parts);
                default -> new TextMessage("未知命令");
            };
        } catch (Exception e) {
            log.error("處理成交明細命令失敗: {} {}", command, symbol, e);
            return new TextMessage("查詢失敗，請稍後再試");
        }
    }

    /**
     * 建立成交統計訊息
     */
    private Message buildTickStatsMessage(String symbol) {
        TickStats stats = realtimeTickService.getRealtimeStats(symbol);

        if (stats.getTotalTicks() == null || stats.getTotalTicks() == 0) {
            // 嘗試自動開始監控
            String result = realtimeTickService.startMonitor(symbol);

            return switch (result) {
                case "NO_API_KEY" -> new TextMessage(
                        "即時成交功能需要 Fugle API Key\n" +
                                "免費註冊：https://developer.fugle.tw/\n" +
                                "設定環境變數：FUGLE_API_KEY"
                );
                case "LIMIT_EXCEEDED" -> new TextMessage(String.format(
                        "Fugle 訂閱額度已滿 (5檔上限)\n" +
                                "目前狀態: %s\n" +
                                "請先停止其他股票的監控",
                        realtimeTickService.getSubscriptionSummary()
                ));
                case "FUGLE" -> new TextMessage(String.format(
                        "%s 即時監控已啟動\n" +
                                "數據將透過 WebSocket 即時推送\n" +
                                "請稍後再查詢即可取得成交統計",
                        symbol
                ));
                default -> new TextMessage(symbol + " 監控啟動失敗，請稍後再試");
            };
        }

        String name = stats.getName() != null ? stats.getName() : symbol;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s %s 即時成交\n", symbol, name));
        sb.append("══════════════\n");

        // 價格資訊
        if (stats.getLastPrice() != null) {
            sb.append(String.format("現價: %.2f", stats.getLastPrice()));
            if (stats.getChange() != null && stats.getChangePercent() != null) {
                String sign = stats.getChange().doubleValue() >= 0 ? "+" : "";
                sb.append(String.format(" (%s%.2f, %s%.2f%%)\n",
                        sign, stats.getChange(),
                        sign, stats.getChangePercent()));
            } else {
                sb.append("\n");
            }
        }

        if (stats.getAvgPrice() != null) {
            sb.append(String.format("均價: %.2f\n", stats.getAvgPrice()));
        }

        if (stats.getHighPrice() != null && stats.getLowPrice() != null) {
            sb.append(String.format("最高: %.2f | 最低: %.2f\n",
                    stats.getHighPrice(), stats.getLowPrice()));
        }

        sb.append("──────────────\n");

        // 成交量資訊
        sb.append(String.format("總筆數: %,d 筆\n", stats.getTotalTicks()));
        sb.append(String.format("總成交: %,d 張\n", stats.getTotalVolumeLots()));

        sb.append("──────────────\n");

        // 內外盤
        sb.append(String.format("外盤: %,d 張 (%.1f%%)\n",
                stats.getBuyVolumeLots(), stats.getBuyRatio()));
        sb.append(String.format("內盤: %,d 張 (%.1f%%)\n",
                stats.getSellVolumeLots(), 100 - stats.getBuyRatio()));

        sb.append("──────────────\n");

        // 資料來源與時間
        if (stats.getUpdateTime() != null) {
            sb.append(String.format("更新: %s (%s)",
                    stats.getUpdateTime().toLocalTime().toString().substring(0, 8),
                    stats.getSource() != null ? stats.getSource() : "FUGLE"));
        }

        return new TextMessage(sb.toString());
    }

    /**
     * 建立大單成交訊息
     */
    private Message buildBigTradesMessage(String symbol) {
        List<TickData> bigTrades = realtimeTickService.getBigTrades(symbol, 100);

        if (bigTrades.isEmpty()) {
            TickStats stats = realtimeTickService.getRealtimeStats(symbol);
            if (stats.getTotalTicks() == null || stats.getTotalTicks() == 0) {
                String result = realtimeTickService.startMonitor(symbol);

                return switch (result) {
                    case "NO_API_KEY" -> new TextMessage(
                            "即時成交功能需要 Fugle API Key\n" +
                                    "免費註冊：https://developer.fugle.tw/"
                    );
                    case "LIMIT_EXCEEDED" -> new TextMessage(
                            "Fugle 訂閱額度已滿 (5檔上限)"
                    );
                    case "FUGLE" -> new TextMessage(String.format(
                            "%s 即時監控已啟動\n請稍後再查詢大單成交",
                            symbol
                    ));
                    default -> new TextMessage(symbol + " 監控啟動失敗");
                };
            }
            return new TextMessage(symbol + " 今日無 100 張以上大單");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(symbol).append(" 大單成交 (>=100張)\n");
        sb.append("══════════════════\n");

        // 最多顯示 10 筆（最新的）
        int count = Math.min(bigTrades.size(), 10);
        for (int i = bigTrades.size() - 1; i >= bigTrades.size() - count; i--) {
            TickData tick = bigTrades.get(i);
            int lots = tick.getVolumeLots();
            String type = tick.getTickType().getDisplayName();
            String time = tick.getTime() != null
                    ? tick.getTime().toLocalTime().toString().substring(0, 8)
                    : "-";

            sb.append(String.format("%s %s %d張 @ %.2f\n",
                    time, type, lots, tick.getPrice()));
        }

        sb.append("──────────────────\n");
        sb.append(String.format("共 %d 筆大單", bigTrades.size()));

        return new TextMessage(sb.toString());
    }

    /**
     * 處理監控命令
     */
    private Message handleMonitorCommand(String symbol, String[] parts) {
        String action = parts.length > 2 ? parts[2] : "start";

        if ("stop".equals(action)) {
            realtimeTickService.stopMonitor(symbol);
            return new TextMessage(symbol + " 已停止監控");
        }

        String result = realtimeTickService.startMonitor(symbol);
        String summary = realtimeTickService.getSubscriptionSummary();

        return switch (result) {
            case "NO_API_KEY" -> new TextMessage(
                    "即時成交功能需要設定 Fugle API Key\n\n" +
                            "1. 免費註冊: https://developer.fugle.tw/\n" +
                            "2. 設定環境變數: FUGLE_API_KEY=xxx"
            );
            case "LIMIT_EXCEEDED" -> new TextMessage(String.format(
                    "Fugle 訂閱額度已滿\n" +
                            "目前狀態: %s\n\n" +
                            "請先用 /tick-monitor 股票代號 stop 停止其他監控",
                    summary
            ));
            case "FUGLE" -> new TextMessage(String.format(
                    "%s 即時監控已啟動\n" +
                            "══════════════\n" +
                            "數據源: Fugle WebSocket\n" +
                            "延遲: 毫秒級\n" +
                            "訂閱狀態: %s",
                    symbol, summary
            ));
            default -> new TextMessage(symbol + " 監控啟動失敗，請稍後再試");
        };
    }

    /**
     * 取得說明訊息
     */
    private String getHelpMessage() {
        return """
                即時成交明細查詢
                ══════════════
                /ticks 股票代號 - 成交統計
                /明細 股票代號 - 成交統計
                /大單 股票代號 - 大單成交

                範例:
                /ticks 2330
                /大單 台積電
                """;
    }

    @Override
    public int getPriority() {
        return 25;
    }
}
