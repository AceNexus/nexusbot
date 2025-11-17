package com.acenexus.tata.nexusbot.handler.handlers;

import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.facade.AIFacade;
import com.acenexus.tata.nexusbot.postback.handlers.AIPostbackHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.acenexus.tata.nexusbot.constants.Actions.CLEAR_HISTORY;
import static com.acenexus.tata.nexusbot.constants.Actions.CONFIRM_CLEAR_HISTORY;
import static com.acenexus.tata.nexusbot.constants.Actions.DISABLE_AI;
import static com.acenexus.tata.nexusbot.constants.Actions.ENABLE_AI;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_DEEPSEEK_R1;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_GEMMA2_9B;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_LLAMA3_70B;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_LLAMA_3_1_8B;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_LLAMA_3_3_70B;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_QWEN3_32B;
import static com.acenexus.tata.nexusbot.constants.Actions.SELECT_MODEL;
import static com.acenexus.tata.nexusbot.constants.Actions.TOGGLE_AI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AIPostbackHandler 測試（重構版）")
class AIPostbackHandlerTest {

    @Mock
    private AIFacade aiFacade;

    @Mock
    private JsonNode event;

    @InjectMocks
    private AIPostbackHandler handler;

    private static final String ROOM_ID = "room123";
    private static final String ROOM_TYPE = "USER";
    private static final String REPLY_TOKEN = "replyToken123";

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("canHandle - 應該處理所有 AI 相關動作")
    void canHandle_shouldReturnTrue_forAllAIActions() {
        assertThat(handler.canHandle(TOGGLE_AI)).isTrue();
        assertThat(handler.canHandle(ENABLE_AI)).isTrue();
        assertThat(handler.canHandle(DISABLE_AI)).isTrue();
        assertThat(handler.canHandle(SELECT_MODEL)).isTrue();
        assertThat(handler.canHandle(MODEL_LLAMA_3_1_8B)).isTrue();
        assertThat(handler.canHandle(MODEL_LLAMA_3_3_70B)).isTrue();
        assertThat(handler.canHandle(MODEL_LLAMA3_70B)).isTrue();
        assertThat(handler.canHandle(MODEL_GEMMA2_9B)).isTrue();
        assertThat(handler.canHandle(MODEL_DEEPSEEK_R1)).isTrue();
        assertThat(handler.canHandle(MODEL_QWEN3_32B)).isTrue();
        assertThat(handler.canHandle(CLEAR_HISTORY)).isTrue();
        assertThat(handler.canHandle(CONFIRM_CLEAR_HISTORY)).isTrue();
    }

    @Test
    @DisplayName("canHandle - 不應該處理其他動作")
    void canHandle_shouldReturnFalse_forOtherActions() {
        assertThat(handler.canHandle("UNKNOWN_ACTION")).isFalse();
    }

    @Test
    @DisplayName("handle - TOGGLE_AI 應該顯示 AI 設定選單")
    void handle_shouldShowAiSettingsMenu_whenToggleAI() {
        // given
        Message expectedMessage = new TextMessage("AI 設定選單");
        when(aiFacade.showSettingsMenu(ROOM_ID, ChatRoom.RoomType.USER)).thenReturn(expectedMessage);

        // when
        Message result = handler.handle(TOGGLE_AI, ROOM_ID, ROOM_TYPE, REPLY_TOKEN, event);

        // then
        assertThat(result).isEqualTo(expectedMessage);
        verify(aiFacade, times(1)).showSettingsMenu(ROOM_ID, ChatRoom.RoomType.USER);
    }

    @Test
    @DisplayName("handle - ENABLE_AI 成功應該返回成功訊息")
    void handle_shouldReturnSuccess_whenEnableAISucceeds() {
        // given
        Message successMessage = new TextMessage("AI 回應功能已啟用！");
        when(aiFacade.enableAI(ROOM_ID, ChatRoom.RoomType.USER)).thenReturn(successMessage);

        // when
        Message result = handler.handle(ENABLE_AI, ROOM_ID, ROOM_TYPE, REPLY_TOKEN, event);

        // then
        assertThat(result).isEqualTo(successMessage);
        verify(aiFacade, times(1)).enableAI(ROOM_ID, ChatRoom.RoomType.USER);
    }

    @Test
    @DisplayName("handle - ENABLE_AI 失敗應該返回錯誤訊息")
    void handle_shouldReturnError_whenEnableAIFails() {
        // given
        Message errorMessage = new TextMessage("啟用 AI 功能時發生錯誤");
        when(aiFacade.enableAI(ROOM_ID, ChatRoom.RoomType.USER)).thenReturn(errorMessage);

        // when
        Message result = handler.handle(ENABLE_AI, ROOM_ID, ROOM_TYPE, REPLY_TOKEN, event);

        // then
        assertThat(result).isEqualTo(errorMessage);
        verify(aiFacade, times(1)).enableAI(ROOM_ID, ChatRoom.RoomType.USER);
    }

    @Test
    @DisplayName("handle - DISABLE_AI 成功應該返回成功訊息")
    void handle_shouldReturnSuccess_whenDisableAISucceeds() {
        // given
        Message successMessage = new TextMessage("AI 回應功能已關閉。");
        when(aiFacade.disableAI(ROOM_ID, ChatRoom.RoomType.USER)).thenReturn(successMessage);

        // when
        Message result = handler.handle(DISABLE_AI, ROOM_ID, ROOM_TYPE, REPLY_TOKEN, event);

        // then
        assertThat(result).isEqualTo(successMessage);
        verify(aiFacade, times(1)).disableAI(ROOM_ID, ChatRoom.RoomType.USER);
    }

    @Test
    @DisplayName("handle - DISABLE_AI 失敗應該返回錯誤訊息")
    void handle_shouldReturnError_whenDisableAIFails() {
        // given
        Message errorMessage = new TextMessage("關閉 AI 功能時發生錯誤");
        when(aiFacade.disableAI(ROOM_ID, ChatRoom.RoomType.USER)).thenReturn(errorMessage);

        // when
        Message result = handler.handle(DISABLE_AI, ROOM_ID, ROOM_TYPE, REPLY_TOKEN, event);

        // then
        assertThat(result).isEqualTo(errorMessage);
        verify(aiFacade, times(1)).disableAI(ROOM_ID, ChatRoom.RoomType.USER);
    }

    @Test
    @DisplayName("handle - SELECT_MODEL 應該顯示模型選擇選單")
    void handle_shouldShowModelSelectionMenu_whenSelectModel() {
        // given
        Message expectedMessage = new TextMessage("模型選擇選單");
        when(aiFacade.showModelSelectionMenu(ROOM_ID, ChatRoom.RoomType.USER)).thenReturn(expectedMessage);

        // when
        Message result = handler.handle(SELECT_MODEL, ROOM_ID, ROOM_TYPE, REPLY_TOKEN, event);

        // then
        assertThat(result).isEqualTo(expectedMessage);
        verify(aiFacade, times(1)).showModelSelectionMenu(ROOM_ID, ChatRoom.RoomType.USER);
    }

    @Test
    @DisplayName("handle - MODEL_LLAMA_3_1_8B 成功應該切換模型")
    void handle_shouldChangeModel_whenModelLlama31_8BSucceeds() {
        // given
        Message successMessage = new TextMessage("AI 模型已切換");
        when(aiFacade.selectModel(ROOM_ID, ChatRoom.RoomType.USER, "llama-3.1-8b-instant", "Llama 3.1 8B (快速創意)"))
                .thenReturn(successMessage);

        // when
        Message result = handler.handle(MODEL_LLAMA_3_1_8B, ROOM_ID, ROOM_TYPE, REPLY_TOKEN, event);

        // then
        assertThat(result).isEqualTo(successMessage);
        verify(aiFacade, times(1)).selectModel(ROOM_ID, ChatRoom.RoomType.USER, "llama-3.1-8b-instant", "Llama 3.1 8B (快速創意)");
    }

    @Test
    @DisplayName("handle - 模型切換失敗應該返回錯誤訊息")
    void handle_shouldReturnError_whenModelChangeFails() {
        // given
        Message errorMessage = new TextMessage("切換 AI 模型時發生錯誤");
        when(aiFacade.selectModel(ROOM_ID, ChatRoom.RoomType.USER, "llama-3.1-8b-instant", "Llama 3.1 8B (快速創意)"))
                .thenReturn(errorMessage);

        // when
        Message result = handler.handle(MODEL_LLAMA_3_1_8B, ROOM_ID, ROOM_TYPE, REPLY_TOKEN, event);

        // then
        assertThat(result).isEqualTo(errorMessage);
        verify(aiFacade, times(1)).selectModel(ROOM_ID, ChatRoom.RoomType.USER, "llama-3.1-8b-instant", "Llama 3.1 8B (快速創意)");
    }

    @Test
    @DisplayName("handle - CLEAR_HISTORY 應該顯示確認訊息")
    void handle_shouldShowConfirmation_whenClearHistory() {
        // given
        Message expectedMessage = new TextMessage("確定要清除歷史記錄嗎？");
        when(aiFacade.showClearHistoryConfirmation()).thenReturn(expectedMessage);

        // when
        Message result = handler.handle(CLEAR_HISTORY, ROOM_ID, ROOM_TYPE, REPLY_TOKEN, event);

        // then
        assertThat(result).isEqualTo(expectedMessage);
        verify(aiFacade, times(1)).showClearHistoryConfirmation();
    }

    @Test
    @DisplayName("handle - CONFIRM_CLEAR_HISTORY 應該清除歷史記錄")
    void handle_shouldClearHistory_whenConfirmClearHistory() {
        // given
        Message successMessage = new TextMessage("歷史對話記錄已清除。");
        when(aiFacade.clearHistory(ROOM_ID)).thenReturn(successMessage);

        // when
        Message result = handler.handle(CONFIRM_CLEAR_HISTORY, ROOM_ID, ROOM_TYPE, REPLY_TOKEN, event);

        // then
        assertThat(result).isEqualTo(successMessage);
        verify(aiFacade, times(1)).clearHistory(ROOM_ID);
    }

    @Test
    @DisplayName("handle - 未知動作應返回 null")
    void handle_shouldReturnNull_whenUnknownAction() {
        // when
        Message result = handler.handle("UNKNOWN_ACTION", ROOM_ID, ROOM_TYPE, REPLY_TOKEN, event);

        // then
        assertThat(result).isNull();
        verifyNoInteractions(aiFacade);
    }

    @Test
    @DisplayName("getPriority - 應該返回優先順序 2")
    void getPriority_shouldReturn2() {
        // when
        int priority = handler.getPriority();

        // then
        assertThat(priority).isEqualTo(2);
    }
}
