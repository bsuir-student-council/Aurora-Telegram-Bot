package org.example.commands;

import org.example.AuroraBot;
import org.example.interfaces.BotCommandHandler;
import org.example.modules.profile_matching.ProfileMatchingTask;

public class MatchCommand implements BotCommandHandler {
    private final AuroraBot bot;
    private final ProfileMatchingTask profileMatchingTask;

    public MatchCommand(AuroraBot bot, ProfileMatchingTask profileMatchingTask) {
        this.bot = bot;
        this.profileMatchingTask = profileMatchingTask;
    }

    @Override
    public void handle(Long userId) {
        try {
            profileMatchingTask.sendMatchedProfiles();
            bot.sendTextMessage(userId, "Процесс подбора профилей запущен.");
        } catch (Exception e) {
            bot.sendTextMessage(userId, "Произошла ошибка при запуске подбора профилей: " + e.getMessage());
        }
    }
}
