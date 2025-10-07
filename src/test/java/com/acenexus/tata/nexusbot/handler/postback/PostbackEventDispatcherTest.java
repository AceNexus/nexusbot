package com.acenexus.tata.nexusbot.handler.postback;

import com.acenexus.tata.nexusbot.chatroom.ChatRoomManager;
import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.service.MessageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostbackEventDispatcher 測試")
class PostbackEventDispatcherTest {

    @Mock
    private PostbackHandler handler1;

    @Mock
    private PostbackHandler handler2;

    @Mock
    private MessageService messageService;

    @Mock
    private ChatRoomManager chatRoomManager;

    private PostbackEventDispatcher dispatcher;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        List<PostbackHandler> handlers = Arrays.asList(handler1, handler2);
        dispatcher = new PostbackEventDispatcher(handlers, messageService, chatRoomManager);
    }

    @Test
    @DisplayName("dispatch - 應該分發給第一個可處理的 Handler")
    void dispatch_shouldSendToFirstHandlerThatCanHandle() throws Exception {
        // given
        String json = """
                {
                    "replyToken": "reply123",
                    "postback": {
                        "data": "TEST_ACTION"
                    },
                    "source": {
                        "type": "user",
                        "userId": "user123"
                    }
                }
                """;
        JsonNode event = objectMapper.readTree(json);
        Message response = new TextMessage("Response");

        when(handler1.getPriority()).thenReturn(1);
        when(handler2.getPriority()).thenReturn(2);
        when(handler1.canHandle("TEST_ACTION")).thenReturn(true);
        when(handler1.handle(eq("TEST_ACTION"), eq("user123"), anyString(), eq("reply123"), eq(event)))
                .thenReturn(response);
        when(chatRoomManager.determineRoomType("user")).thenReturn(ChatRoom.RoomType.USER);

        // when
        dispatcher.dispatch(event);

        // then
        verify(handler1, times(1)).canHandle("TEST_ACTION");
        verify(handler1, times(1)).handle(eq("TEST_ACTION"), eq("user123"), eq("USER"), eq("reply123"), eq(event));
        verify(messageService, times(1)).sendMessage("reply123", response);
        verify(handler2, never()).canHandle(anyString());
    }

    @Test
    @DisplayName("dispatch - 應該按優先順序嘗試多個 Handler")
    void dispatch_shouldTryHandlersInPriorityOrder() throws Exception {
        // given
        String json = """
                {
                    "replyToken": "reply123",
                    "postback": {
                        "data": "TEST_ACTION"
                    },
                    "source": {
                        "type": "user",
                        "userId": "user123"
                    }
                }
                """;
        JsonNode event = objectMapper.readTree(json);
        Message response = new TextMessage("Response");

        when(handler1.getPriority()).thenReturn(1);
        when(handler2.getPriority()).thenReturn(2);
        when(handler1.canHandle("TEST_ACTION")).thenReturn(false);
        when(handler2.canHandle("TEST_ACTION")).thenReturn(true);
        when(handler2.handle(eq("TEST_ACTION"), eq("user123"), anyString(), eq("reply123"), eq(event)))
                .thenReturn(response);
        when(chatRoomManager.determineRoomType("user")).thenReturn(ChatRoom.RoomType.USER);

        // when
        dispatcher.dispatch(event);

        // then
        verify(handler1, times(1)).canHandle("TEST_ACTION");
        verify(handler2, times(1)).canHandle("TEST_ACTION");
        verify(handler2, times(1)).handle(eq("TEST_ACTION"), eq("user123"), eq("USER"), eq("reply123"), eq(event));
        verify(messageService, times(1)).sendMessage("reply123", response);
    }

    @Test
    @DisplayName("dispatch - 群組事件應使用 groupId")
    void dispatch_shouldUseGroupId_forGroupEvents() throws Exception {
        // given
        String json = """
                {
                    "replyToken": "reply123",
                    "postback": {
                        "data": "TEST_ACTION"
                    },
                    "source": {
                        "type": "group",
                        "groupId": "group123"
                    }
                }
                """;
        JsonNode event = objectMapper.readTree(json);
        Message response = new TextMessage("Response");

        when(handler1.getPriority()).thenReturn(1);
        when(handler1.canHandle("TEST_ACTION")).thenReturn(true);
        when(handler1.handle(eq("TEST_ACTION"), eq("group123"), anyString(), eq("reply123"), eq(event)))
                .thenReturn(response);
        when(chatRoomManager.determineRoomType("group")).thenReturn(ChatRoom.RoomType.GROUP);

        // when
        dispatcher.dispatch(event);

        // then
        verify(handler1, times(1)).handle(eq("TEST_ACTION"), eq("group123"), eq("GROUP"), eq("reply123"), eq(event));
        verify(messageService, times(1)).sendMessage("reply123", response);
    }

    @Test
    @DisplayName("dispatch - 沒有 Handler 可處理應發送未知動作訊息")
    void dispatch_shouldSendUnknownActionMessage_whenNoHandlerCanHandle() throws Exception {
        // given
        String json = """
                {
                    "replyToken": "reply123",
                    "postback": {
                        "data": "UNKNOWN_ACTION"
                    },
                    "source": {
                        "type": "user",
                        "userId": "user123"
                    }
                }
                """;
        JsonNode event = objectMapper.readTree(json);

        when(handler1.getPriority()).thenReturn(1);
        when(handler2.getPriority()).thenReturn(2);
        when(handler1.canHandle("UNKNOWN_ACTION")).thenReturn(false);
        when(handler2.canHandle("UNKNOWN_ACTION")).thenReturn(false);
        when(chatRoomManager.determineRoomType("user")).thenReturn(ChatRoom.RoomType.USER);

        // when
        dispatcher.dispatch(event);

        // then
        verify(handler1, times(1)).canHandle("UNKNOWN_ACTION");
        verify(handler2, times(1)).canHandle("UNKNOWN_ACTION");
        verify(messageService, times(1)).sendMessage(eq("reply123"), any(TextMessage.class));
    }

    @Test
    @DisplayName("dispatch - Handler 拋出例外應發送錯誤訊息")
    void dispatch_shouldSendErrorMessage_whenHandlerThrowsException() throws Exception {
        // given
        String json = """
                {
                    "replyToken": "reply123",
                    "postback": {
                        "data": "TEST_ACTION"
                    },
                    "source": {
                        "type": "user",
                        "userId": "user123"
                    }
                }
                """;
        JsonNode event = objectMapper.readTree(json);

        when(handler1.getPriority()).thenReturn(1);
        when(handler1.canHandle("TEST_ACTION")).thenReturn(true);
        when(handler1.handle(anyString(), anyString(), anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Test exception"));
        when(chatRoomManager.determineRoomType("user")).thenReturn(ChatRoom.RoomType.USER);

        // when
        dispatcher.dispatch(event);

        // then
        verify(messageService, times(1)).sendMessage(eq("reply123"), any(TextMessage.class));
    }

    @Test
    @DisplayName("dispatch - 缺少 postback 欄位應不處理")
    void dispatch_shouldNotProcess_whenPostbackFieldMissing() throws Exception {
        // given
        String json = """
                {
                    "replyToken": "reply123",
                    "source": {
                        "type": "user",
                        "userId": "user123"
                    }
                }
                """;
        JsonNode event = objectMapper.readTree(json);

        // when
        dispatcher.dispatch(event);

        // then
        verifyNoInteractions(handler1);
        verifyNoInteractions(handler2);
        verifyNoInteractions(messageService);
    }

    @Test
    @DisplayName("dispatch - Handler 返回 null 應繼續嘗試下一個 Handler")
    void dispatch_shouldTryNextHandler_whenHandlerReturnsNull() throws Exception {
        // given
        String json = """
                {
                    "replyToken": "reply123",
                    "postback": {
                        "data": "TEST_ACTION"
                    },
                    "source": {
                        "type": "user",
                        "userId": "user123"
                    }
                }
                """;
        JsonNode event = objectMapper.readTree(json);
        Message response = new TextMessage("Response from handler2");

        when(handler1.getPriority()).thenReturn(1);
        when(handler2.getPriority()).thenReturn(2);
        when(handler1.canHandle("TEST_ACTION")).thenReturn(true);
        when(handler1.handle(eq("TEST_ACTION"), eq("user123"), anyString(), eq("reply123"), eq(event)))
                .thenReturn(null); // Handler1 返回 null
        when(handler2.canHandle("TEST_ACTION")).thenReturn(true);
        when(handler2.handle(eq("TEST_ACTION"), eq("user123"), anyString(), eq("reply123"), eq(event)))
                .thenReturn(response);
        when(chatRoomManager.determineRoomType("user")).thenReturn(ChatRoom.RoomType.USER);

        // when
        dispatcher.dispatch(event);

        // then
        verify(handler1, times(1)).handle(eq("TEST_ACTION"), eq("user123"), eq("USER"), eq("reply123"), eq(event));
        verify(handler2, times(1)).handle(eq("TEST_ACTION"), eq("user123"), eq("USER"), eq("reply123"), eq(event));
        verify(messageService, times(1)).sendMessage("reply123", response);
    }

    @Test
    @DisplayName("dispatch - 空 action 字串應發送未知動作訊息")
    void dispatch_shouldSendUnknownAction_whenActionIsEmpty() throws Exception {
        // given
        String json = """
                {
                    "replyToken": "reply123",
                    "postback": {
                        "data": ""
                    },
                    "source": {
                        "type": "user",
                        "userId": "user123"
                    }
                }
                """;
        JsonNode event = objectMapper.readTree(json);

        when(handler1.getPriority()).thenReturn(1);
        when(handler2.getPriority()).thenReturn(2);
        when(handler1.canHandle("")).thenReturn(false);
        when(handler2.canHandle("")).thenReturn(false);
        when(chatRoomManager.determineRoomType("user")).thenReturn(ChatRoom.RoomType.USER);

        // when
        dispatcher.dispatch(event);

        // then
        verify(handler1, times(1)).canHandle("");
        verify(handler2, times(1)).canHandle("");
        verify(messageService, times(1)).sendMessage(eq("reply123"), any(TextMessage.class));
    }

    @Test
    @DisplayName("dispatch - 多個 Handler 都能處理應選擇優先順序最高的")
    void dispatch_shouldSelectHighestPriorityHandler_whenMultipleCanHandle() throws Exception {
        // given
        String json = """
                {
                    "replyToken": "reply123",
                    "postback": {
                        "data": "TEST_ACTION"
                    },
                    "source": {
                        "type": "user",
                        "userId": "user123"
                    }
                }
                """;
        JsonNode event = objectMapper.readTree(json);
        Message response1 = new TextMessage("Response from handler1");

        when(handler1.getPriority()).thenReturn(1); // 更高優先順序
        when(handler2.getPriority()).thenReturn(5);
        when(handler1.canHandle("TEST_ACTION")).thenReturn(true);
        // 移除 handler2.canHandle 的 stub，因為不會被呼叫
        when(handler1.handle(eq("TEST_ACTION"), eq("user123"), anyString(), eq("reply123"), eq(event)))
                .thenReturn(response1);
        when(chatRoomManager.determineRoomType("user")).thenReturn(ChatRoom.RoomType.USER);

        // when
        dispatcher.dispatch(event);

        // then
        verify(handler1, times(1)).canHandle("TEST_ACTION");
        verify(handler1, times(1)).handle(eq("TEST_ACTION"), eq("user123"), eq("USER"), eq("reply123"), eq(event));
        verify(handler2, never()).canHandle(anyString()); // handler2 不應被呼叫
        verify(messageService, times(1)).sendMessage("reply123", response1);
    }
}
