package org.example.commands;

import org.example.AuroraBot;
import org.example.interfaces.BotCommandHandler;
import org.example.models.UserInfo;
import org.example.services.UserInfoService;

import java.util.List;
import java.util.Optional;

public class AdminsListCommand implements BotCommandHandler {
    private final AuroraBot bot;
    private final UserInfoService userInfoService;

    public AdminsListCommand(AuroraBot bot, UserInfoService userInfoService) {
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

        List<UserInfo> admins = userInfoService.getAllUsers().stream()
                .filter(user -> user.getRole() == UserInfo.Role.ADMIN)
                .toList();

        if (admins.isEmpty()) {
            bot.sendTextMessage(userId, "Администраторы не найдены.");
        } else {
            StringBuilder adminList = new StringBuilder("Список администраторов:\n");
            int counter = 1;
            for (UserInfo admin : admins) {
                String adminAlias = bot.getUserAlias(admin.getUserId());
                String adminName = admin.getName() != null ? admin.getName() : "Имя не указано";
                adminList.append(counter++)
                        .append(". ")
                        .append(adminName)
                        .append(" - ")
                        .append(adminAlias != null ? adminAlias : admin.getUserId())
                        .append("\n");
            }
            bot.sendTextMessage(userId, adminList.toString());
        }
    }
}
