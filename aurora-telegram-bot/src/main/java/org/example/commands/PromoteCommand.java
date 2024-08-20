package org.example.commands;

import org.example.AuroraBot;
import org.example.interfaces.BotCommandHandler;
import org.example.enums.DialogMode;
import org.example.models.UserInfo;
import org.example.services.UserInfoService;

import java.util.Optional;

public class PromoteCommand implements BotCommandHandler {
    private final AuroraBot bot;
    private final UserInfoService userInfoService;

    public PromoteCommand(AuroraBot bot, UserInfoService userInfoService) {
        this.bot = bot;
        this.userInfoService = userInfoService;
    }

    @Override
    public void handle(Long userId) {
        Optional<UserInfo> userInfoOptional = userInfoService.getUserInfoByUserId(userId);
        if (userInfoOptional.isEmpty() || userInfoOptional.get().getRole() != UserInfo.Role.ADMIN) {
            bot.sendTextMessage(userId, "У вас нет прав для выполнения этой команды.");
            return;
        }

        bot.sendTextMessage(userId, "Пожалуйста, отправьте алиас пользователя в формате @username.");
        bot.getUserModes().put(userId, DialogMode.PROMOTE);
    }
}
