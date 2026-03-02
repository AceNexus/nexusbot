# [AIClient-2-API：Gemini CLI OAuth 設定指南](https://github.com/justlovemaki/AIClient-2-API)

---

## 前置條件

- Google 帳號（免費帳號即可，訂閱 Gemini Pro 可獲得更高配額）
- [Node.js ≥ 20.0.0](https://nodejs.org/)
- Port 3000（Web UI）、8085（Gemini OAuth 回調）未被占用

---

## 一、下載並安裝 AIClient-2-API

本文件使用 [AIClient-2-API](https://github.com/justlovemaki/AIClient-2-API) 作為 Gemini 的本地 API 代理，將 Gemini CLI 的 OAuth 授權轉換為標準 OpenAI 相容介面。

```bash
# 下載專案
git clone https://github.com/justlovemaki/AIClient-2-API.git

# 進入目錄並安裝依賴
cd AIClient-2-API
npm install
```

---

## 二、檔案結構說明

```
configs/
├── config.json                    # 主設定檔（從 config.json.example 複製）
├── config.json.example            # 設定範例
├── provider_pools.json            # 提供商憑證池（授權後自動更新）
├── gemini/
│   └── xxxxxxxx_oauth_creds.json  # Gemini OAuth 憑據（授權後自動生成）
└── pwd                            # Web UI 登入密碼
```

---

## 三、設定 config.json

在 `configs/` 目錄下，將範例檔複製並改名：

```
configs/config.json.example  →  複製並改名為  configs/config.json
```

> `config.json` 預設使用 `gemini-cli-oauth` 作為主要提供商，無需修改即可使用。

**重要設定項說明（`configs/config.json`）：**

```json
{
  "REQUIRED_API_KEY": "123456",        // 客戶端呼叫 API 時需要帶的 Bearer Token，建議改為強密碼
  "SERVER_PORT": 3000,                 // Web UI 與 API 服務的埠號
  "MODEL_PROVIDER": "gemini-cli-oauth" // 使用 Gemini CLI OAuth
}
```

---

## 四、啟動服務

```bash
# 切換到 AIClient-2-API 專案目錄，例如：
cd D:\java\AceNexus\AIClient-2-API
npm start
```

看到以下訊息代表啟動成功：

```
Unified API Server running on http://0.0.0.0:3000
```

---

## 五、透過 Web UI 完成 Gemini OAuth 授權

### 5.1 登入 Web UI

開啟瀏覽器，訪問：

```
http://localhost:3000
```

預設密碼：`admin123`

> 建議登入後至「配置管理」頁面修改密碼。

### 5.2 生成授權

1. 點擊左側邊欄的 **「提供商池管理」**
2. 找到 **Gemini CLI OAuth** 的項目
3. 點擊 **「生成授權」** 按鈕
4. 瀏覽器自動開啟 Google 登入頁面
5. 選擇 Google 帳號，完成授權

授權成功後，系統會自動：

- 將憑證儲存至 `configs/gemini/` 目錄
- 更新 `configs/provider_pools.json`

---

## 六、驗證是否成功

### 6.1 對話測試（PowerShell）

```powershell
curl -Uri "http://localhost:3000/v1/chat/completions" `
     -Method POST `
     -Headers @{"Content-Type"="application/json"; "Authorization"="Bearer 123456"} `
     -Body '{"model": "gemini-2.5-pro", "messages": [{"role": "user", "content": "你好"}]}' `
     -UseBasicParsing
```

### 6.2 查詢可用模型（PowerShell）

```powershell
curl -Uri "http://localhost:3000/v1/models" `
     -Method GET `
     -Headers @{"Authorization"="Bearer 123456"} `
     -UseBasicParsing
```

---
