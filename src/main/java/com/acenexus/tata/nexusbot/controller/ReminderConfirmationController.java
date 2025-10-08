package com.acenexus.tata.nexusbot.controller;

import com.acenexus.tata.nexusbot.entity.ReminderLog;
import com.acenexus.tata.nexusbot.repository.ReminderLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 提醒確認控制器
 * 處理 Email 確認連結的點擊
 * 重要：此控制器只更新資料庫，不會發送 LINE Push Message，以節省 LINE 訊息額度
 */
@Tag(name = "提醒確認", description = "Email 提醒確認端點 (零 Quota 設計)")
@Controller
@RequestMapping("/reminder")
@RequiredArgsConstructor
public class ReminderConfirmationController {

    private static final Logger logger = LoggerFactory.getLogger(ReminderConfirmationController.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss");

    private final ReminderLogRepository reminderLogRepository;

    /**
     * 處理提醒確認
     *
     * @param token 確認 Token
     * @param model 視圖模型
     * @return 確認結果頁面
     */
    @Operation(
            summary = "確認 Email 提醒",
            description = """
                    處理 Email 提醒確認連結的點擊。

                    **設計特色 - 零 Quota 設計**:
                    - 只更新資料庫的 `confirmed_at` 欄位
                    - **不發送 LINE Push Message**，節省 LINE 訊息配額
                    - 使用者可在 LINE Bot 中查看「今日提醒記錄」確認狀態

                    **處理流程**:
                    1. 驗證確認 Token 是否有效
                    2. 檢查是否已確認過
                    3. 更新資料庫 `confirmed_at` 欄位
                    4. 返回 HTML 確認結果頁面

                    **返回頁面**:
                    - 成功: 顯示「提醒確認成功」
                    - 已確認: 顯示「此提醒已確認」+ 確認時間
                    - 失敗: 顯示「無效的確認連結」或「確認失敗」

                    **Thymeleaf 模板**: `reminder-confirmation.html`
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "返回確認結果頁面 (HTML)",
                    content = @Content(
                            mediaType = "text/html",
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(
                                    value = """
                                            <!DOCTYPE html>
                                            <html>
                                            <body>
                                                <h1>提醒確認成功</h1>
                                                <p>您可以返回 LINE 查看提醒狀態</p>
                                            </body>
                                            </html>
                                            """
                            )
                    )
            )
    })
    @GetMapping("/confirm/{token}")
    public String confirmReminder(
            @Parameter(
                    description = "Email 確認 Token (UUID 格式)",
                    example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                    required = true
            )
            @PathVariable String token,
            Model model) {
        try {
            // 查詢確認 Token
            Optional<ReminderLog> optionalLog = reminderLogRepository.findByConfirmationToken(token);

            if (optionalLog.isEmpty()) {
                logger.warn("Invalid confirmation token: {}", token);
                model.addAttribute("success", false);
                model.addAttribute("message", "無效的確認連結");
                model.addAttribute("detail", "此連結可能已過期或不存在");
                return "reminder-confirmation";
            }

            ReminderLog log = optionalLog.get();

            // 檢查是否已確認
            if (log.getConfirmedAt() != null) {
                logger.info("Reminder already confirmed: token={}, confirmedAt={}", token, log.getConfirmedAt());
                model.addAttribute("success", true);
                model.addAttribute("message", "此提醒已確認");
                model.addAttribute("detail", "確認時間: " + log.getConfirmedAt().format(TIME_FORMATTER));
                model.addAttribute("alreadyConfirmed", true);
                return "reminder-confirmation";
            }

            // 更新確認狀態
            log.setConfirmedAt(LocalDateTime.now());
            reminderLogRepository.save(log);

            logger.info("Reminder confirmed successfully: token={}, reminderId={}", token, log.getReminderId());

            model.addAttribute("success", true);
            model.addAttribute("message", "提醒確認成功");
            model.addAttribute("detail", "您可以返回 LINE 查看提醒狀態");
            model.addAttribute("alreadyConfirmed", false);

            return "reminder-confirmation";

        } catch (Exception e) {
            logger.error("Error confirming reminder with token {}: {}", token, e.getMessage(), e);
            model.addAttribute("success", false);
            model.addAttribute("message", "確認失敗");
            model.addAttribute("detail", "系統發生錯誤，請稍後再試");
            return "reminder-confirmation";
        }
    }
}
