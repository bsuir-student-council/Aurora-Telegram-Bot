package org.example.callbacks;

import org.example.AuroraBot;
import org.example.MultiSessionTelegramBot;
import org.example.interfaces.CallbackQueryHandler;

public class AcceptedCallbackHandler implements CallbackQueryHandler {
    private final AuroraBot bot;

    public AcceptedCallbackHandler(AuroraBot bot) {
        this.bot = bot;
    }

    @Override
    public void handle(Long userId, Integer messageId) {
        String updatedMessage = MultiSessionTelegramBot.loadMessage("info") + "\n\n➪ Принято 🫡";
        bot.editTextMessageWithButtons(userId, messageId, updatedMessage);
        bot.askFullName(userId);
    }
}
