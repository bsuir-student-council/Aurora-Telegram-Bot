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
    public void execute(Long userId) {
        bot.sendTextButtonsMessage(userId, MultiSessionTelegramBot.loadMessage("start"), "Поехали🚀", "start");
    }
}
