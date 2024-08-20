package org.example.callbacks;

import org.example.AuroraBot;
import org.example.MultiSessionTelegramBot;
import org.example.interfaces.CallbackQueryHandler;

import java.util.logging.Logger;

public class StartCallbackHandler implements CallbackQueryHandler {
    private static final Logger logger = Logger.getLogger(StartCallbackHandler.class.getName());
    private final AuroraBot bot;

    public StartCallbackHandler(AuroraBot bot) {
        this.bot = bot;
    }

    @Override
    public void handle(Long userId, Integer messageId) {
        try {
            String startMessage = MultiSessionTelegramBot.loadMessage("start");
            String updatedMessage = startMessage + "\n\n➪ Поехали🚀";
            bot.editTextMessageWithButtons(userId, messageId, updatedMessage);

            String infoMessage = MultiSessionTelegramBot.loadMessage("info");
            bot.sendTextButtonsMessage(userId, infoMessage, "Принято 😊", "accepted");

            logger.info("Handled start callback for userId: " + userId);
        } catch (Exception e) {
            logger.severe("Error handling start callback for userId: " + userId + " - " + e.getMessage());
        }
    }
}
