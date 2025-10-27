package com.acenexus.tata.nexusbot.command;

/**
 * 命令處理器統一接口
 * <p>
 * 設計模式: Strategy Pattern + Chain of Responsibility
 * 每個 Handler 負責處理特定類型的命令，由 CommandDispatcher 按優先級路由。
 *
 * @see CommandDispatcher
 * @see CommandContext
 * @see CommandResult
 */
public interface CommandHandler {

    /**
     * 判斷是否可以處理該命令
     * <p>
     * 設計原則（必須遵守）：
     * - 必須是純函數（Pure Function），無副作用
     * - 只做判斷邏輯，不執行業務邏輯
     * - 不修改任何狀態，只查詢狀態
     * - 可以多次調用而不影響結果
     * - 使用 context.getNormalizedText() 進行文字匹配
     * <p>
     * 常見模式：
     * // 模式 1: 基於命令匹配
     * return COMMAND_SET.contains(context.getNormalizedText());
     * <p>
     * // 模式 2: 基於狀態查詢
     * return stateManager.isInState(context.getRoomId());
     * <p>
     * // 模式 3: 基於權限查詢
     * return permissionService.hasPermission(context.getRoomId());
     * <p>
     * // 模式 4: 組合判斷
     * return context.getNormalizedText().equals("/cmd") || stateManager.isInState(...);
     * <p>
     * 反例（禁止）：
     * <pre>
     * // 錯誤：在 canHandle 中執行業務邏輯
     * Message response = facade.handleInteraction(...);
     * return response != null;
     *
     * // 錯誤：使用 getMessageText() 而非 getNormalizedText()
     * return context.getMessageText().trim().equals("menu");
     * </pre>
     *
     * @param context 命令上下文
     * @return true 如果可以處理
     */
    boolean canHandle(CommandContext context);

    /**
     * 處理命令
     * <p>
     * 注意：此方法應該只被調用一次。
     * CommandDispatcher 會確保只有第一個 canHandle 返回 true 的 Handler 會被執行。
     *
     * @param context 命令上下文
     * @return 命令執行結果
     */
    CommandResult handle(CommandContext context);

    /**
     * 獲取處理器優先級（數字越小優先級越高）
     * <p>
     * 優先級建議：
     * - 1: 認證相關（最高優先級）
     * - 2: 管理員命令
     * - 3: 狀態流程處理（Reminder, Email 輸入等）
     * - 4: 特定功能命令
     * - 5: 通用命令（如選單）
     *
     * @return 優先級
     */
    int getPriority();
}
