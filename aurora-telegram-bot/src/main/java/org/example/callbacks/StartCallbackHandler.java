package org.example.callbacks;

import org.example.AuroraBot;
import org.example.MultiSessionTelegramBot;
import org.example.interfaces.CallbackQueryHandler;

public class StartCallbackHandler implements CallbackQueryHandler {
    private final AuroraBot bot;

    public StartCallbackHandler(AuroraBot bot) {
        this.bot = bot;
    }

    @Override
    public void handle(Long userId, Integer messageId) {
        String updatedMessage = MultiSessionTelegramBot.loadMessage("start") + "\n\n➪ Поехали🚀";
        bot.editTextMessageWithButtons(userId, messageId, updatedMessage);
        bot.sendTextButtonsMessage(userId, MultiSessionTelegramBot.loadMessage("info"), "Принято 😊", "accepted");
    }
}
