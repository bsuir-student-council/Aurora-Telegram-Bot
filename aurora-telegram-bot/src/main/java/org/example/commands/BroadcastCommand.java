package org.example.commands;

import org.example.AuroraBot;
import org.example.interfaces.BotCommandHandler;
import org.example.enums.DialogMode;
import org.example.models.UserInfo;
import org.example.services.UserInfoService;

public class BroadcastCommand implements BotCommandHandler {

    private final AuroraBot bot;
    private final UserInfoService userInfoService;

    public BroadcastCommand(AuroraBot bot, UserInfoService userInfoService) {
        this.bot = bot;
        this.userInfoService = userInfoService;
    }

    @Override
    public void handle(Long userId) {
        userInfoService.getUserInfoByUserId(userId).ifPresentOrElse(
                userInfo -> checkAdminAndRequestMessage(userId, userInfo),
                () -> bot.sendTextMessage(userId, "У вас нет прав для выполнения этой команды.")
        );
    }

    private void checkAdminAndRequestMessage(Long userId, UserInfo userInfo) {
        if (userInfo.getRole() == UserInfo.Role.ADMIN) {
            bot.sendTextMessage(userId, "Пожалуйста, отправьте сообщение, которое нужно разослать всем видимым пользователям.\n\n" +
                    "Для отмены отправьте сообщение 'Отмена'.");
            bot.getUserModes().put(userId, DialogMode.BROADCAST);
        } else {
            bot.sendTextMessage(userId, "У вас нет прав для выполнения этой команды.");
        }
    }
}
