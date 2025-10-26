package com.acenexus.tata.nexusbot.command;

import com.acenexus.tata.nexusbot.entity.ChatRoom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CommandDispatcher 單元測試
 */
@ExtendWith(MockitoExtension.class)
class CommandDispatcherTest {

    @Mock
    private CommandHandler handler1;

    @Mock
    private CommandHandler handler2;

    private CommandDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        when(handler1.getPriority()).thenReturn(1);
        when(handler2.getPriority()).thenReturn(2);

        // 亂序輸入測試排序功能
        dispatcher = new CommandDispatcher(List.of(handler2, handler1));
    }

    @Test
    void shouldDispatchToFirstMatchingHandler() {
        // Given
        CommandContext context = createContext("test");
        when(handler1.canHandle(context)).thenReturn(true);
        when(handler1.handle(context)).thenReturn(CommandResult.handled());

        // When
        CommandResult result = dispatcher.dispatch(context);

        // Then
        assertThat(result.isHandled()).isTrue();
        verify(handler1).canHandle(context);
        verify(handler1).handle(context);
        verify(handler2, never()).canHandle(any()); // handler2 不應被調用
    }

    @Test
    void shouldRespectPriorityOrder() {
        // Given
        CommandContext context = createContext("test");
        when(handler1.canHandle(context)).thenReturn(false);
        when(handler2.canHandle(context)).thenReturn(true);
        when(handler2.handle(context)).thenReturn(CommandResult.handled());

        // When
        CommandResult result = dispatcher.dispatch(context);

        // Then
        assertThat(result.isHandled()).isTrue();
        verify(handler1).canHandle(context); // handler1 先被檢查（優先級高）
        verify(handler2).canHandle(context); // handler1 無法處理，輪到 handler2
        verify(handler2).handle(context);
    }

    @Test
    void shouldReturnNotHandledWhenNoHandlerMatches() {
        // Given
        CommandContext context = createContext("unknown");
        when(handler1.canHandle(context)).thenReturn(false);
        when(handler2.canHandle(context)).thenReturn(false);

        // When
        CommandResult result = dispatcher.dispatch(context);

        // Then
        assertThat(result.isHandled()).isFalse();
        assertThat(result.getMessage()).isNull();
        assertThat(result.getTextResponse()).isNull();
    }

    @Test
    void shouldSortHandlersByPriority() {
        // Given
        CommandContext context = createContext("test");
        when(handler1.canHandle(context)).thenReturn(true);
        when(handler1.handle(context)).thenReturn(CommandResult.withText("response"));

        // When
        CommandResult result = dispatcher.dispatch(context);

        // Then
        // 驗證優先級 1 的 handler1 先被調用
        assertThat(result.isHandled()).isTrue();
        verify(handler1).handle(context);
        verify(handler2, never()).canHandle(any());
    }

    private CommandContext createContext(String message) {
        return CommandContext.builder()
                .roomId("test-room")
                .roomType(ChatRoom.RoomType.USER)
                .userId("user-123")
                .messageText(message)
                .normalizedText(message.toLowerCase())
                .replyToken("reply-token")
                .build();
    }
}
