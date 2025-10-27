package com.acenexus.tata.nexusbot.handler.handlers;

import com.acenexus.tata.nexusbot.chatroom.ChatRoomManager;
import com.acenexus.tata.nexusbot.postback.handlers.NavigationPostbackHandler;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
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

import static com.acenexus.tata.nexusbot.constants.Actions.ABOUT;
import static com.acenexus.tata.nexusbot.constants.Actions.HELP_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.MAIN_MENU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NavigationPostbackHandler 測試")
class NavigationPostbackHandlerTest {

    @Mock
    private MessageTemplateProvider messageTemplateProvider;

    @Mock
    private ChatRoomManager chatRoomManager;

    @Mock
    private JsonNode event;

    @InjectMocks
    private NavigationPostbackHandler handler;

    private Message mainMenuMessage;
    private Message helpMenuMessage;
    private Message aboutMessage;

    @BeforeEach
    void setUp() {
        mainMenuMessage = new TextMessage("主選單");
        helpMenuMessage = new TextMessage("說明選單");
        aboutMessage = new TextMessage("關於");
    }

    @Test
    @DisplayName("canHandle - 應該處理 MAIN_MENU 動作")
    void canHandle_shouldReturnTrue_forMainMenu() {
        // when
        boolean result = handler.canHandle(MAIN_MENU);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("canHandle - 應該處理 HELP_MENU 動作")
    void canHandle_shouldReturnTrue_forHelpMenu() {
        // when
        boolean result = handler.canHandle(HELP_MENU);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("canHandle - 應該處理 ABOUT 動作")
    void canHandle_shouldReturnTrue_forAbout() {
        // when
        boolean result = handler.canHandle(ABOUT);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("canHandle - 不應該處理其他動作")
    void canHandle_shouldReturnFalse_forOtherActions() {
        // when
        boolean result = handler.canHandle("UNKNOWN_ACTION");

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("handle - 應該返回主選單訊息")
    void handle_shouldReturnMainMenu_whenMainMenuAction() {
        // given
        when(messageTemplateProvider.mainMenu()).thenReturn(mainMenuMessage);

        // when
        Message result = handler.handle(MAIN_MENU, "room123", "user", "replyToken123", event);

        // then
        assertThat(result).isEqualTo(mainMenuMessage);
        verify(messageTemplateProvider, times(1)).mainMenu();
        verifyNoInteractions(chatRoomManager);
    }

    @Test
    @DisplayName("handle - 應該返回說明選單訊息")
    void handle_shouldReturnHelpMenu_whenHelpMenuAction() {
        // given
        when(messageTemplateProvider.helpMenu()).thenReturn(helpMenuMessage);

        // when
        Message result = handler.handle(HELP_MENU, "room123", "user", "replyToken123", event);

        // then
        assertThat(result).isEqualTo(helpMenuMessage);
        verify(messageTemplateProvider, times(1)).helpMenu();
        verifyNoInteractions(chatRoomManager);
    }

    @Test
    @DisplayName("handle - 應該返回關於訊息")
    void handle_shouldReturnAbout_whenAboutAction() {
        // given
        when(messageTemplateProvider.about()).thenReturn(aboutMessage);

        // when
        Message result = handler.handle(ABOUT, "room123", "user", "replyToken123", event);

        // then
        assertThat(result).isEqualTo(aboutMessage);
        verify(messageTemplateProvider, times(1)).about();
        verifyNoInteractions(chatRoomManager);
    }

    @Test
    @DisplayName("handle - 未知動作應返回 null")
    void handle_shouldReturnNull_whenUnknownAction() {
        // when
        Message result = handler.handle("UNKNOWN_ACTION", "room123", "user", "replyToken123", event);

        // then
        assertThat(result).isNull();
        verifyNoInteractions(messageTemplateProvider);
        verifyNoInteractions(chatRoomManager);
    }

    @Test
    @DisplayName("getPriority - 應該返回低優先順序 10")
    void getPriority_shouldReturn10() {
        // when
        int priority = handler.getPriority();

        // then
        assertThat(priority).isEqualTo(10);
    }
}
