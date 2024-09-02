package org.example.commands;

import org.example.AuroraBot;
import org.example.interfaces.BotCommandHandler;
import org.example.models.UserInfo;
import org.example.services.UserInfoService;

import java.util.logging.Logger;

public class ProfileCommand implements BotCommandHandler {
    private static final Logger logger = Logger.getLogger(ProfileCommand.class.getName());
    private final AuroraBot bot;
    private final UserInfoService userInfoService;

    public ProfileCommand(AuroraBot bot, UserInfoService userInfoService) {
        this.bot = bot;
        this.userInfoService = userInfoService;
    }

    @Override
    public void handle(Long userId) {
        userInfoService.getUserInfoByUserId(userId).ifPresentOrElse(
                userInfo -> sendUserProfile(userId, userInfo),
                () -> bot.sendTextMessage(userId, "Анкета не найдена. Пожалуйста, заполните анкету командой /start.")
        );
    }

    private void sendUserProfile(Long userId, UserInfo userInfo) {
        try {
            String photoUrl = bot.getUserPhotoUrl(userId);
            String profileMessage = buildProfileMessage(userId, userInfo);

            if (photoUrl != null) {
                bot.sendPhotoMessage(userId, photoUrl);
            }
            bot.sendTextButtonsMessage(userId, profileMessage,
                    "Редактировать", "accepted",
                    "Сменить статус видимости", "toggle_visibility");

            logger.info("Profile sent to userId: " + userId);
        } catch (Exception e) {
            logger.severe("Error sending profile to userId: " + userId + " - " + e.getMessage());
        }
    }

    private String buildProfileMessage(Long userId, UserInfo userInfo) {
        String contactInfo = getContactInfo(userId);
        String visibilityStatus = getVisibilityStatus(userInfo);

        return String.format(
                "Вот так будет выглядеть ваш профиль в сообщении, которое мы пришлём вашему собеседнику:\n⏬\n%s%s",
                userInfoService.formatUserProfile(userInfo, contactInfo),
                visibilityStatus
        );
    }

    private String getContactInfo(Long userId) {
        String userAlias = bot.getUserAlias(userId);
        return (userAlias != null && !userAlias.equals("@null"))
                ? userAlias
                : String.format("<a href=\"tg://user?id=%d\">Профиль пользователя</a>", userId);
    }

    private String getVisibilityStatus(UserInfo userInfo) {
        return userInfo.getIsVisible()
                ? "\n✅ Ваша анкета видна."
                : "\n❌ На данный момент вашу анкету никто не видит.";
    }
}
