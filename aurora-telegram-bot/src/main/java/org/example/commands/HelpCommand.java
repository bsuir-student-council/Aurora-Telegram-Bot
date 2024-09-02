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
    public void handle(Long userId) {
        String helpMessage = """
                /start - Заполнить анкету заново 🔄

                /support️ - Предложить улучшения или сообщить об ошибках ️🛠""";
        bot.sendTextMessage(userId, helpMessage);
    }
}
