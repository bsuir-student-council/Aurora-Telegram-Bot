package org.example.commands;

import org.example.AuroraBot;
import org.example.interfaces.BotCommandHandler;
import org.example.MultiSessionTelegramBot;

public class HelpCommand implements BotCommandHandler {
    private final AuroraBot bot;

    public HelpCommand(AuroraBot bot) {
        this.bot = bot;
    }

    @Override
    public void execute(Long userId) {
        String helpMessage = MultiSessionTelegramBot.loadMessage("help");
        bot.sendTextMessage(userId, helpMessage);
    }
}
