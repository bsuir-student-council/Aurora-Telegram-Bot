package org.example.dialogs;

import org.example.AuroraBot;
import org.example.interfaces.DialogHandler;
import org.example.models.UserInfo;
import org.example.services.UserInfoService;

import java.util.logging.Logger;

public class PromoteUserDialogHandler implements DialogHandler {
    private static final Logger logger = Logger.getLogger(PromoteUserDialogHandler.class.getName());

    private static final String USER_NOT_FOUND_MESSAGE = "Пользователь не найден или не зарегистрирован в системе.";
    private final AuroraBot bot;
    private final UserInfoService userInfoService;

    public PromoteUserDialogHandler(AuroraBot bot, UserInfoService userInfoService) {
        this.bot = bot;
        this.userInfoService = userInfoService;
    }

    @Override
    public void handle(Long userId, String username) {
        try {
            Long targetUserId = findUserIdByUsername(username);

            if (targetUserId == null) {
                bot.sendTextMessage(userId, USER_NOT_FOUND_MESSAGE);
                return;
            }

            UserInfo userInfo = userInfoService.getUserInfoByUserId(targetUserId)
                    .orElseThrow(() -> new IllegalStateException(USER_NOT_FOUND_MESSAGE));

            if (userInfo.getRole() == UserInfo.Role.ADMIN) {
                bot.sendTextMessage(userId, "Этот пользователь уже является админом.");
                return;
            }

            promoteUserToAdmin(userInfo, userId);

        } catch (Exception e) {
            bot.sendTextMessage(userId, "Произошла ошибка при обновлении роли пользователя.");
            logger.severe("Error promoting user: " + e.getMessage());
        }
    }

    private Long findUserIdByUsername(String username) {
        return userInfoService.getAllUsers().stream()
                .map(UserInfo::getUserId)
                .filter(userId -> username.equals(bot.getUserAlias(userId)))
                .findFirst()
                .orElse(null);
    }

    private void promoteUserToAdmin(UserInfo userInfo, Long userId) {
        userInfo.setRole(UserInfo.Role.ADMIN);
        userInfoService.saveUserInfo(userInfo);
        bot.sendTextMessage(userId, "Пользователь успешно повышен до Админа.");
        bot.getUserModes().remove(userId);
        logger.info("User promoted to admin: " + userInfo.getUserId());
    }
}
