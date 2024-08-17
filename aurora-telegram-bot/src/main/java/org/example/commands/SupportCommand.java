package org.example.commands;

import org.example.AuroraBot;
import org.example.interfaces.BotCommandHandler;
import org.example.enums.DialogMode;

public class SupportCommand implements BotCommandHandler {
    private final AuroraBot bot;

    public SupportCommand(AuroraBot bot) {
        this.bot = bot;
    }

    @Override
    public void execute(Long userId) {
        if (bot.isRequestTooFrequent(userId)) {
            return;
        }

        bot.getUserModes().put(userId, DialogMode.SUPPORT);
        bot.sendTextMessage(userId,
                "Пожалуйста, опишите вашу проблему. Максимальная длина сообщения - 2000 символов. " +
                        "Вы можете отправить не более одного сообщения раз в 15 минут. Если вы передумали писать, нажмите /profile.");
    }
}
