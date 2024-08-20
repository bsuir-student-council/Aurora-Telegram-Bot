package org.example.commands;

import org.example.AuroraBot;
import org.example.interfaces.BotCommandHandler;
import org.example.enums.DialogMode;
import org.example.models.UserInfo;
import org.example.services.UserInfoService;


public class PromoteCommand implements BotCommandHandler {

    private static final String NO_PERMISSION_MESSAGE = "У вас нет прав для выполнения этой команды.";
    private final AuroraBot bot;
    private final UserInfoService userInfoService;

    public PromoteCommand(AuroraBot bot, UserInfoService userInfoService) {
        this.bot = bot;
        this.userInfoService = userInfoService;
    }

    @Override
    public void handle(Long userId) {
        userInfoService.getUserInfoByUserId(userId).ifPresentOrElse(
                userInfo -> checkAdminAndRequestUsername(userId, userInfo),
                () -> bot.sendTextMessage(userId, NO_PERMISSION_MESSAGE)
        );
    }

    private void checkAdminAndRequestUsername(Long userId, UserInfo userInfo) {
        if (userInfo.getRole() == UserInfo.Role.ADMIN) {
            bot.sendTextMessage(userId, "Пожалуйста, отправьте алиас пользователя в формате @username.");
            bot.getUserModes().put(userId, DialogMode.PROMOTE);
        } else {
            bot.sendTextMessage(userId, NO_PERMISSION_MESSAGE);
        }
    }
}
