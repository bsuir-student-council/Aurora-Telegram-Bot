package org.example.dialogs;

import org.example.AuroraBot;
import org.example.interfaces.DialogHandler;
import org.example.models.UserInfo;
import org.example.services.UserInfoService;

import java.util.List;

public class PromoteUserDialogHandler implements DialogHandler {
    private final AuroraBot bot;
    private final UserInfoService userInfoService;

    public PromoteUserDialogHandler(AuroraBot bot, UserInfoService userInfoService) {
        this.bot = bot;
        this.userInfoService = userInfoService;
    }

    @Override
    public void handle(Long userId, String username) {
        Long targetUserId = null;

        List<UserInfo> allUsers = userInfoService.getAllUsers();

        for (UserInfo user : allUsers) {
            String userAlias = bot.getUserAlias(user.getUserId());
            if (userAlias != null && userAlias.equals(username)) {
                targetUserId = user.getUserId();
                break;
            }
        }

        if (targetUserId == null) {
            bot.sendTextMessage(userId, "Пользователь не найден или не зарегистрирован в системе.");
            return;
        }

        UserInfo userInfo = userInfoService.getUserInfoByUserId(targetUserId).orElseThrow();

        if (userInfo.getRole() == UserInfo.Role.ADMIN) {
            bot.sendTextMessage(userId, "Этот пользователь уже является админом.");
            return;
        }

        try {
            userInfo.setRole(UserInfo.Role.ADMIN);
            userInfoService.saveUserInfo(userInfo);
            bot.sendTextMessage(userId, "Пользователь успешно повышен до роли Админа.");
            bot.getUserModes().remove(userId);
        } catch (Exception e) {
            bot.sendTextMessage(userId, "Произошла ошибка при обновлении роли пользователя.");
        }
    }
}
