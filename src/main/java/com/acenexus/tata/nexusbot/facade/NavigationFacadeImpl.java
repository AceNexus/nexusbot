package com.acenexus.tata.nexusbot.facade;

import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 導航功能 Facade 實作
 */
@Service
@RequiredArgsConstructor
public class NavigationFacadeImpl implements NavigationFacade {

    private static final Logger logger = LoggerFactory.getLogger(NavigationFacadeImpl.class);

    private final MessageTemplateProvider messageTemplateProvider;

    @Override
    public Message showMainMenu() {
        logger.debug("Showing main menu");
        return messageTemplateProvider.mainMenu();
    }

    @Override
    public Message showHelpMenu() {
        logger.debug("Showing help menu");
        return messageTemplateProvider.helpMenu();
    }

    @Override
    public Message showAbout() {
        logger.debug("Showing about page");
        return messageTemplateProvider.about();
    }
}
