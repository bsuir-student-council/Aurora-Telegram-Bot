package org.example.commands;

import org.example.AuroraBot;
import org.example.interfaces.BotCommandHandler;
import org.example.MultiSessionTelegramBot;

public class StartCommand implements BotCommandHandler {

    private final AuroraBot bot;

    public StartCommand(AuroraBot bot) {
        this.bot = bot;
    }

    @Override
    public void handle(Long userId) {
        String startMessage = MultiSessionTelegramBot.loadMessage("start");
        bot.sendTextButtonsMessage(userId, startMessage,
                "Поехали🚀", "start");
    }
}
