package org.example.commands;

import org.example.AuroraBot;
import org.example.interfaces.BotCommandHandler;
import org.example.services.UserInfoService;

public class ProfileCommand implements BotCommandHandler {
    private final AuroraBot bot;
    private final UserInfoService userInfoService;

    public ProfileCommand(AuroraBot bot, UserInfoService userInfoService) {
        this.bot = bot;
        this.userInfoService = userInfoService;
    }

    @Override
    public void execute(Long userId) {
        userInfoService.getUserInfoByUserId(userId).ifPresentOrElse(
                userInfo -> bot.sendUserProfile(userId, userInfo),
                () -> bot.sendTextMessage(userId, "Анкета не найдена. Пожалуйста, заполните анкету командой /start.")
        );
    }
}
