package org.example.callbacks;

import org.example.AuroraBot;
import org.example.interfaces.CallbackQueryHandler;
import org.example.models.UserInfo;
import org.example.services.UserInfoService;

import java.util.logging.Logger;

public class ToggleVisibilityCallbackHandler implements CallbackQueryHandler {
    private static final Logger logger = Logger.getLogger(ToggleVisibilityCallbackHandler.class.getName());
    private final AuroraBot bot;
    private final UserInfoService userInfoService;

    public ToggleVisibilityCallbackHandler(AuroraBot bot, UserInfoService userInfoService) {
        this.bot = bot;
        this.userInfoService = userInfoService;
    }

    @Override
    public void handle(Long userId, Integer messageId) {
        try {
            userInfoService.toggleVisibility(userId);
            UserInfo userInfo = userInfoService.getUserInfoByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            String updatedMessage = formatUserProfileMessage(userId, userInfo);
            bot.editTextMessageWithButtons(
                    userId,
                    messageId,
                    updatedMessage,
                    "Редактировать", "accepted",
                    "Сменить статус видимости", "toggle_visibility"
            );

            logger.info("Visibility toggled for userId: " + userId);
        } catch (Exception e) {
            logger.severe("Error handling toggle visibility for userId: " + userId + " - " + e.getMessage());
        }
    }

    private String formatUserProfileMessage(Long userId, UserInfo userInfo) {
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
