# Fugle 即時成交功能文件

## 目錄

- [1. 概述](#1-概述)
- [2. 架構設計](#2-架構設計)
- [3. 檔案位置](#3-檔案位置)
- [4. 環境設定](#4-環境設定)
- [5. 服務限制](#5-服務限制)
- [6. 使用方式](#6-使用方式)
- [7. 監控與日誌](#7-監控與日誌)
- [8. 常見問題排除](#8-常見問題排除)
- [9. 重啟與恢復](#9-重啟與恢復)
- [10. 定期維護](#10-定期維護)
- [11. 實現細節](#11-實現細節)
- [12. Fugle API 參考](#12-fugle-api-參考)

---

## 1. 概述

本系統使用 Fugle API 提供台股即時成交明細功能，採用純 Fugle WebSocket 架構，讓用戶可在瀏覽器中即時監控多檔股票的成交明細。

**功能特色**：

- 即時成交數據推送（毫秒級延遲）
- 內外盤統計與視覺化
- 大單警示（≥100 張）
- 多檔股票同時監控（最多 5 檔）

**實現日期**：2026-01-28

---

## 2. 架構設計

```
┌─────────────────────────────────────────────────────────────┐
│                    瀏覽器 (React + Tailwind)                  │
│                              │                               │
│                    WebSocket (STOMP over SockJS)             │
└─────────────────────────────────┬───────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────┐
│                     Spring Boot Server                      │
│  ┌─────────────────┐  ┌─────────────────────────────────┐   │
│  │ WebSocketConfig │  │ TickWebSocketController         │   │
│  │ (STOMP 配置)    │  │ (訂閱管理 + 廣播)                  │   │
│  └─────────────────┘  └─────────────────────────────────┘   │
│                                  │                          │
│                                  ▼                          │
│            ┌─────────────────────────────────────┐          │
│            │ RealtimeTickService                 │          │
│            │ (成交數據管理 + 統計計算)               │          │
│            └─────────────────────────────────────┘          │
│                                  │                          │
│                                  ▼                          │
│            ┌─────────────────────────────────────┐          │
│            │ FugleWebSocketClient                │          │
│            │ (Fugle API 連接)                     │          │
│            └─────────────────────────────────────┘          │
└─────────────────────────────────┬───────────────────────────┘
                                  │
                                  ▼ WebSocket
                        ┌─────────────────┐
                        │   Fugle API     │
                        │  (即時數據源)     │
                        └─────────────────┘
```

**Spring Boot 扮演雙重角色**：
| 角色 | 檔案 | 用途 |
|------|------|------|
| WebSocket 客戶端 | `FugleWebSocketClient.java` | 連接 Fugle API 取得即時數據 |
| WebSocket 伺服器 | `WebSocketConfig.java` | 讓瀏覽器連入訂閱數據 |

---

## 3. 檔案位置

```
src/main/java/com/acenexus/tata/nexusbot/
├── config/
│   ├── FugleConfig.java              # Fugle 配置類別
│   └── WebSocketConfig.java          # WebSocket STOMP 配置
├── client/
│   └── FugleWebSocketClient.java     # Fugle WebSocket 客戶端
├── service/
│   └── RealtimeTickService.java      # 即時成交服務
├── controller/
│   ├── TickWebSocketController.java  # WebSocket 控制器
│   └── StockController.java          # REST API（含成交相關）
├── dto/
│   ├── TickData.java                 # 成交數據 DTO
│   ├── TickStats.java                # 成交統計 DTO
│   └── TickType.java                 # 內外盤枚舉
└── event/handler/
    └── TickCommandHandler.java       # LINE Bot 成交命令

src/main/resources/static/
├── index.html                        # 首頁（含導向連結）
└── realtime-tick.html                # 前端監控頁面
```

---

## 4. 環境設定

### 必要設定

```bash
# Fugle API Key（必填）
FUGLE_API_KEY=your_api_key_here
```

### 取得 API Key

1. 前往 Fugle 開發者平台：https://developer.fugle.tw/
2. 註冊/登入帳號
3. 建立應用程式取得 API Key
4. 免費方案即可使用

### 選用設定（application.yml）

```yaml
fugle:
  api-key: ${FUGLE_API_KEY}
  websocket-url: wss://api.fugle.tw/marketdata/v1.0/stock/streaming
  reconnect-delay: 5000      # 重連延遲（毫秒）
  max-subscriptions: 5      # 最大訂閱數量
```

---

## 5. 服務限制

### Fugle 免費方案限制

| 項目    | 限制                |
|-------|-------------------|
| 最大訂閱數 | 5 檔股票             |
| 數據延遲  | 即時（毫秒級）           |
| 交易時間  | 09:00 - 13:30（台股） |
| 連線數   | 1 個 WebSocket 連線  |

### 系統內部設定

| 項目   | 值     | 位置                            |
|------|-------|-------------------------------|
| 大單門檻 | 100 張 | `RealtimeTickService.java:48` |
| 歷史保留 | 當日    | 每日 08:30 清除                   |
| 重連延遲 | 5 秒   | `FugleConfig.java`            |

### 訂閱模式：全局共享

系統採用 **全局共享模式**，所有訪客看到相同的監控列表：

```
訪客A 新增 2330 ──► 所有人都看到 2330
訪客B 新增 2317 ──► 所有人都看到 2330, 2317
訪客C 移除 2330 ──► 所有人都移除 2330
```

**特點**：

- 所有訪客共享同一個監控列表
- 任何人新增/移除股票，所有人同步更新
- 新訪客進入時自動同步現有監控列表
- 最多監控 5 檔股票（Fugle 免費版限制）

**WebSocket 頻道**：
| 頻道 | 用途 |
|------|------|
| `/topic/tick-status` | 全局狀態同步 (SYNC/SUBSCRIBE/UNSUBSCRIBE) |
| `/topic/ticks/{symbol}` | 單一股票成交數據 |
| `/topic/big-trades` | 大單警示 |

---

## 6. 使用方式

### 啟動應用

```bash
./gradlew bootRun
```

### 開啟頁面

```
http://localhost:5001/index.html          # 首頁（有連結）
http://localhost:5001/realtime-tick.html  # 直接訪問
```

### 功能操作

1. 在搜尋框輸入股票代號或名稱
2. 點擊搜尋結果新增監控
3. 即時成交數據將自動推送
4. 點擊股票標籤切換顯示
5. 點擊 × 移除監控
