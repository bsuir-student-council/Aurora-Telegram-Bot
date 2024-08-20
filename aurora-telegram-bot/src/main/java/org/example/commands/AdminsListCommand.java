package org.example.commands;

import org.example.AuroraBot;
import org.example.interfaces.BotCommandHandler;
import org.example.models.UserInfo;
import org.example.services.UserInfoService;

import java.util.List;

public class AdminsListCommand implements BotCommandHandler {

    private static final String NO_PERMISSION_MESSAGE = "У вас нет прав для выполнения этой команды.";

    private final AuroraBot bot;
    private final UserInfoService userInfoService;

    public AdminsListCommand(AuroraBot bot, UserInfoService userInfoService) {
        this.bot = bot;
        this.userInfoService = userInfoService;
    }

    @Override
    public void handle(Long userId) {
        userInfoService.getUserInfoByUserId(userId).ifPresentOrElse(
                userInfo -> checkAdminAndListAdmins(userId, userInfo),
                () -> bot.sendTextMessage(userId, NO_PERMISSION_MESSAGE)
        );
    }

    private void checkAdminAndListAdmins(Long userId, UserInfo userInfo) {
        if (userInfo.getRole() == UserInfo.Role.ADMIN) {
            sendAdminsList(userId);
        } else {
            bot.sendTextMessage(userId, NO_PERMISSION_MESSAGE);
        }
    }

    private void sendAdminsList(Long userId) {
        List<UserInfo> admins = userInfoService.getAllUsers().stream()
                .filter(user -> user.getRole() == UserInfo.Role.ADMIN)
                .toList();

        if (admins.isEmpty()) {
            bot.sendTextMessage(userId, "Администраторы не найдены.");
        } else {
            String adminList = buildAdminList(admins);
            bot.sendTextMessage(userId, adminList);
        }
    }

    private String buildAdminList(List<UserInfo> admins) {
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
        return adminList.toString();
    }
}
