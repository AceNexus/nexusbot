package com.acenexus.tata.nexusbot.template;

import com.acenexus.tata.nexusbot.constants.AiModel;
import com.acenexus.tata.nexusbot.constants.AiProvider;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.flex.component.Button;
import com.linecorp.bot.model.message.flex.container.Bubble;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.acenexus.tata.nexusbot.constants.Actions.CLEAR_HISTORY;
import static com.acenexus.tata.nexusbot.constants.Actions.CONFIRM_CLEAR_HISTORY;
import static com.acenexus.tata.nexusbot.constants.Actions.DISABLE_AI;
import static com.acenexus.tata.nexusbot.constants.Actions.ENABLE_AI;
import static com.acenexus.tata.nexusbot.constants.Actions.MAIN_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_GEMINI_25_FLASH;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_LLAMA_3_1_8B;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_LLAMA_3_3_70B;
import static com.acenexus.tata.nexusbot.constants.Actions.SELECT_MODEL;
import static com.acenexus.tata.nexusbot.constants.Actions.TOGGLE_AI;

/**
 * AI 設定相關的訊息範本建構器
 */
@Component
public class AISettingsTemplateBuilder extends FlexMessageTemplateBuilder {

    /**
     * AI 設定選單
     */
    public Message aiSettingsMenu(boolean currentStatus) {
        String title = currentStatus ? "AI 智能對話已啟用" : "AI 智能對話已停用";
        String description = currentStatus
                ? "系統將自動回應您的訊息並提供智能助手服務。您可以隨時調整設定或選擇不同的 AI 模型。"
                : "目前為手動模式，系統不會自動回應訊息。您可以啟用 AI 功能來獲得智能助手服務。";

        List<Button> buttons = new ArrayList<>();
        if (currentStatus) {
            buttons.add(createWarningButton("停用 AI", DISABLE_AI));
            buttons.add(createNeutralButton("選擇模型", SELECT_MODEL));
            buttons.add(createDangerButton("清除歷史", CLEAR_HISTORY));
        } else {
            buttons.add(createPrimaryButton("啟用 AI", ENABLE_AI));
            buttons.add(createNeutralButton("選擇模型", SELECT_MODEL));
        }
        buttons.add(createNavigateButton("返回主選單", MAIN_MENU));

        return createCard(title, description, buttons);
    }

    /**
     * AI 模型選擇選單（輪播：Groq｜Gemini）
     */
    public Message aiModelSelectionMenu(String currentModel) {
        AiProvider currentProvider = AiModel.fromId(currentModel).provider;
        String modelDisplayName = AiModel.fromId(currentModel).displayName;

        String groqDesc = currentProvider == AiProvider.GROQ ? "目前：" + modelDisplayName : "高速推理，免費額度充足";
        String geminiDesc = currentProvider == AiProvider.GEMINI_PROXY ? "目前：" + modelDisplayName : "Google 最新模型，支援長文本";

        Bubble groqBubble = createBubble(
                "Groq 模型",
                groqDesc,
                Arrays.asList(
                        createSelectionButton("Llama 3.1 8B｜快速", MODEL_LLAMA_3_1_8B, currentModel.equals(AiModel.LLAMA_3_1_8B.id)),
                        createSelectionButton("Llama 3.3 70B｜精準", MODEL_LLAMA_3_3_70B, currentModel.equals(AiModel.LLAMA_3_3_70B.id)),
                        createNavigateButton("返回 AI 設定", TOGGLE_AI)
                )
        );

        Bubble geminiBubble = createBubble(
                "Gemini 模型",
                geminiDesc,
                Arrays.asList(
                        createSelectionButton("Gemini 2.5 Flash", MODEL_GEMINI_25_FLASH, currentModel.equals(AiModel.GEMINI_25_FLASH.id)),
                        createNavigateButton("返回 AI 設定", TOGGLE_AI)
                )
        );

        return createCarousel("選擇 AI 模型", Arrays.asList(groqBubble, geminiBubble));
    }

    /**
     * 清除對話歷史確認訊息
     */
    public Message clearHistoryConfirmation() {
        return createCard(
                "清除對話歷史",
                "確認要清除所有 AI 對話記錄嗎？\n\n此操作將永久刪除所有對話內容、AI 學習記錄及聊天上下文。\n\n請注意：此操作無法復原。",
                Arrays.asList(
                        createDangerButton("確認清除", CONFIRM_CLEAR_HISTORY),
                        createPrimaryButton("取消操作", TOGGLE_AI)
                )
        );
    }
}
