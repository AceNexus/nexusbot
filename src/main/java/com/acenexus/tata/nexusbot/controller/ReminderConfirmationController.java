package com.acenexus.tata.nexusbot.controller;

import com.acenexus.tata.nexusbot.entity.ReminderLog;
import com.acenexus.tata.nexusbot.repository.ReminderLogRepository;
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
    @GetMapping("/confirm/{token}")
    public String confirmReminder(@PathVariable String token, Model model) {
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
