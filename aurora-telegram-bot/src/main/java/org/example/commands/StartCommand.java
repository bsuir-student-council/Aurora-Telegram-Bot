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
        String startMessage = """
                Привет! Я Аврора, твой бот для Random Coffee! ☕️

                Я помогу тебе найти новых друзей и интересных собеседников.
                Каждую неделю я буду подбирать для тебя нового интересного человека на основе твоих интересов.

                Готов к неожиданным знакомствам?""";
        bot.sendTextButtonsMessage(userId, startMessage,
                "Поехали🚀", "start");
    }
}
