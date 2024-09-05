package org.example.commands;

import org.example.AuroraBot;
import org.example.interfaces.BotCommandHandler;
import org.example.models.UserInfo;
import org.example.modules.profile_matching.ProfileMatchingTask;
import org.example.services.UserInfoService;

public class MatchCommand implements BotCommandHandler {
    private static final String NO_PERMISSION_MESSAGE = "У вас нет прав для выполнения этой команды.";

    private final AuroraBot bot;
    private final ProfileMatchingTask profileMatchingTask;
    private final UserInfoService userInfoService;

    public MatchCommand(AuroraBot bot, ProfileMatchingTask profileMatchingTask, UserInfoService userInfoService) {
        this.bot = bot;
        this.profileMatchingTask = profileMatchingTask;
        this.userInfoService = userInfoService;
    }

    @Override
    public void handle(Long userId) {
        userInfoService.getUserInfoByUserId(userId).ifPresentOrElse(
                userInfo -> checkAdminAndExecuteMatch(userId, userInfo),
                () -> bot.sendTextMessage(userId, NO_PERMISSION_MESSAGE)
        );
    }

    private void checkAdminAndExecuteMatch(Long userId, UserInfo userInfo) {
        if (userInfo.getRole() == UserInfo.Role.ADMIN) {
            try {
                profileMatchingTask.sendMatchedProfiles();
                bot.sendTextMessage(userId, "Процесс подбора профилей запущен.");
            } catch (Exception e) {
                bot.sendTextMessage(userId, "Ошибка при запуске подбора профилей: " + e.getMessage());
            }
        } else {
            bot.sendTextMessage(userId, NO_PERMISSION_MESSAGE);
        }
    }

}
