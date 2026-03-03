package com.acenexus.tata.nexusbot.richmenu.impl;

import com.acenexus.tata.nexusbot.config.properties.LineBotProperties;
import com.acenexus.tata.nexusbot.constants.Actions;
import com.acenexus.tata.nexusbot.richmenu.RichMenuService;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.richmenu.RichMenu;
import com.linecorp.bot.model.richmenu.RichMenuArea;
import com.linecorp.bot.model.richmenu.RichMenuBounds;
import com.linecorp.bot.model.richmenu.RichMenuResponse;
import com.linecorp.bot.model.richmenu.RichMenuSize;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Rich Menu 管理服務實作
 * 每次啟動時重建 Rich Menu：刪除舊的 → 建立新的 → 上傳圖片 → 設為 Default
 */
@Service
@RequiredArgsConstructor
public class RichMenuServiceImpl implements RichMenuService {

    private static final Logger logger = LoggerFactory.getLogger(RichMenuServiceImpl.class);
    private static final String IMAGE_UPLOAD_URL = "https://api-data.line.me/v2/bot/richmenu/{richMenuId}/content";
    private static final String IMAGE_CLASSPATH = "richmenu/menu.jpg";

    private final LineMessagingClient lineMessagingClient;
    private final LineBotProperties lineBotProperties;
    private final WebClient.Builder webClientBuilder;

    @Override
    public void setupRichMenu() {
        try {
            logger.info("[RichMenuServiceImpl] Setting up Rich Menu...");

            deleteExistingRichMenus();

            String richMenuId = createRichMenu();
            logger.info("[RichMenuServiceImpl] Rich Menu created, richMenuId={}", richMenuId);

            uploadRichMenuImage(richMenuId);

            lineMessagingClient.setDefaultRichMenu(richMenuId).get();
            logger.info("[RichMenuServiceImpl] Rich Menu setup complete. richMenuId={}", richMenuId);

        } catch (Exception e) {
            logger.error("[RichMenuServiceImpl] Rich Menu setup failed: {}", e.getMessage(), e);
        }
    }

    private void deleteExistingRichMenus() throws Exception {
        List<RichMenuResponse> richMenus = lineMessagingClient.getRichMenuList().get().getRichMenus();
        if (richMenus.isEmpty()) {
            return;
        }
        logger.info("[RichMenuServiceImpl] Deleting {} existing Rich Menu(s)...", richMenus.size());
        for (RichMenuResponse menu : richMenus) {
            lineMessagingClient.deleteRichMenu(menu.getRichMenuId()).get();
        }
    }

    private String createRichMenu() throws Exception {
        RichMenu richMenu = RichMenu.builder()
                .size(new RichMenuSize(2500, 843))
                .selected(true)
                .name("NexusBot Main Menu")
                .chatBarText("功能選單")
                .areas(buildAreas())
                .build();

        return lineMessagingClient.createRichMenu(richMenu).get().getRichMenuId();
    }

    private List<RichMenuArea> buildAreas() {
        return List.of(
                area(0, 0, 833, 421, "AI 智能對話", Actions.TOGGLE_AI),
                area(833, 0, 833, 421, "提醒管理", Actions.REMINDER_MENU),
                area(1666, 0, 834, 421, "Email 通知", Actions.EMAIL_MENU),
                area(0, 421, 833, 422, "時區設定", Actions.TIMEZONE_SETTINGS),
                area(833, 421, 833, 422, "找附近廁所", Actions.FIND_TOILETS),
                area(1666, 421, 834, 422, "說明與支援", Actions.HELP_MENU)
        );
    }

    private RichMenuArea area(int x, int y, int w, int h, String label, String data) {
        return new RichMenuArea(
                new RichMenuBounds(x, y, w, h),
                new PostbackAction(label, data)
        );
    }

    private void uploadRichMenuImage(String richMenuId) {
        ClassPathResource img = new ClassPathResource(IMAGE_CLASSPATH);
        if (!img.exists()) {
            logger.warn("[RichMenuServiceImpl] Rich Menu image not found at classpath:{} — skipping upload", IMAGE_CLASSPATH);
            return;
        }
        try {
            byte[] imageBytes = img.getInputStream().readAllBytes();
            webClientBuilder.build()
                    .post()
                    .uri(IMAGE_UPLOAD_URL, richMenuId)
                    .header("Authorization", "Bearer " + lineBotProperties.getChannelToken())
                    .contentType(MediaType.IMAGE_JPEG)
                    .bodyValue(imageBytes)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            logger.info("[RichMenuServiceImpl] Rich Menu image uploaded successfully");
        } catch (Exception e) {
            logger.error("[RichMenuServiceImpl] Failed to upload Rich Menu image: {}", e.getMessage(), e);
        }
    }

}
