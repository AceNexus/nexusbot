package com.acenexus.tata.nexusbot.facade;

import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.linecorp.bot.model.message.Message;

/**
 * 位置功能 Facade - 協調位置服務流程
 */
public interface LocationFacade {

    Message handleLocationMessage(String roomId, String title, String address, double latitude, double longitude, String replyToken);

    /**
     * 開始廁所搜尋流程
     */
    Message startToiletSearch(String roomId, ChatRoom.RoomType roomType);
}
