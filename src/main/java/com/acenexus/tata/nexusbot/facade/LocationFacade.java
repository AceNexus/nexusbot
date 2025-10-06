package com.acenexus.tata.nexusbot.facade;

import com.linecorp.bot.model.message.Message;

/**
 * 位置功能 Facade - 協調位置服務流程
 */
public interface LocationFacade {

    Message handleLocationMessage(String roomId, String title, String address, double latitude, double longitude, String replyToken);
}
