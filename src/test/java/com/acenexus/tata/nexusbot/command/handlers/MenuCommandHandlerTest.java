package com.acenexus.tata.nexusbot.command.handlers;

import com.acenexus.tata.nexusbot.command.CommandContext;
import com.acenexus.tata.nexusbot.command.CommandResult;
import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import com.linecorp.bot.model.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * MenuCommandHandler 單元測試
 */
@ExtendWith(MockitoExtension.class)
class MenuCommandHandlerTest {

    @Mock
    private MessageTemplateProvider messageTemplateProvider;

    @Mock
    private Message mockMessage;

    private MenuCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MenuCommandHandler(messageTemplateProvider);
    }

    @Test
    void shouldHandleMenuCommand() {
        // Given
        CommandContext context = createContext("menu");

        // When
        boolean canHandle = handler.canHandle(context);

        // Then
        assertThat(canHandle).isTrue();
    }

    @Test
    void shouldHandleChineseMenuCommand() {
        // Given
        CommandContext context = createContext("選單");

        // When
        boolean canHandle = handler.canHandle(context);

        // Then
        assertThat(canHandle).isTrue();
    }

    @Test
    void shouldNotHandleOtherCommands() {
        // Given
        CommandContext context = createContext("other");

        // When
        boolean canHandle = handler.canHandle(context);

        // Then
        assertThat(canHandle).isFalse();
    }

    @Test
    void shouldReturnMenuMessage() {
        // Given
        CommandContext context = createContext("menu");
        when(messageTemplateProvider.mainMenu()).thenReturn(mockMessage);

        // When
        CommandResult result = handler.handle(context);

        // Then
        assertThat(result.isHandled()).isTrue();
        assertThat(result.getMessage()).isEqualTo(mockMessage);
        assertThat(result.getTextResponse()).isNull();
    }

    @Test
    void shouldHavePriority5() {
        // When
        int priority = handler.getPriority();

        // Then
        assertThat(priority).isEqualTo(5);
    }

    @Test
    void shouldBeCaseInsensitive() {
        // Given - 大寫輸入
        CommandContext upperCaseContext = createContext("MENU");
        CommandContext mixedCaseContext = createContext("MeNu");

        // When
        boolean canHandleUpper = handler.canHandle(upperCaseContext);
        boolean canHandleMixed = handler.canHandle(mixedCaseContext);

        // Then
        assertThat(canHandleUpper).isTrue();
        assertThat(canHandleMixed).isTrue();
    }

    private CommandContext createContext(String normalizedText) {
        return CommandContext.builder()
                .roomId("test-room")
                .roomType(ChatRoom.RoomType.USER)
                .userId("user-123")
                .messageText(normalizedText)
                .normalizedText(normalizedText.toLowerCase())
                .replyToken("reply-token")
                .build();
    }
}
