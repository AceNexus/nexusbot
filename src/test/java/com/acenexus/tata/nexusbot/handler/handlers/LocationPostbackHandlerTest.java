package com.acenexus.tata.nexusbot.handler.handlers;

import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.facade.LocationFacade;
import com.acenexus.tata.nexusbot.postback.handlers.LocationPostbackHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TextMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.acenexus.tata.nexusbot.constants.Actions.FIND_TOILETS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocationPostbackHandler 測試（重構版）")
class LocationPostbackHandlerTest {

    @Mock
    private LocationFacade locationFacade;

    @Mock
    private JsonNode event;

    @InjectMocks
    private LocationPostbackHandler handler;

    private static final String ROOM_ID = "room123";
    private static final String ROOM_TYPE = "USER";
    private static final String REPLY_TOKEN = "replyToken123";

    @Test
    @DisplayName("canHandle - 應該處理 FIND_TOILETS 動作")
    void canHandle_shouldReturnTrue_forFindToilets() {
        // when
        boolean result = handler.canHandle(FIND_TOILETS);

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
    @DisplayName("handle - FIND_TOILETS 應該設定等待位置狀態並返回指示訊息")
    void handle_shouldSetWaitingStateAndReturnInstruction_whenFindToilets() {
        // given
        Message expectedMessage = new TextMessage("請分享您的位置");
        when(locationFacade.startToiletSearch(ROOM_ID, ChatRoom.RoomType.USER)).thenReturn(expectedMessage);

        // when
        Message result = handler.handle(FIND_TOILETS, ROOM_ID, ROOM_TYPE, REPLY_TOKEN, event);

        // then
        assertThat(result).isEqualTo(expectedMessage);
        verify(locationFacade, times(1)).startToiletSearch(ROOM_ID, ChatRoom.RoomType.USER);
    }

    @Test
    @DisplayName("handle - 未知動作應返回 null")
    void handle_shouldReturnNull_whenUnknownAction() {
        // when
        Message result = handler.handle("UNKNOWN_ACTION", ROOM_ID, ROOM_TYPE, REPLY_TOKEN, event);

        // then
        assertThat(result).isNull();
        verifyNoInteractions(locationFacade);
    }

    @Test
    @DisplayName("getPriority - 應該返回優先順序 4")
    void getPriority_shouldReturn4() {
        // when
        int priority = handler.getPriority();

        // then
        assertThat(priority).isEqualTo(4);
    }
}
