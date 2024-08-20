package org.example.callbacks;

import org.example.AuroraBot;
import org.example.interfaces.CallbackQueryHandler;
import org.example.models.UserInfo;
import org.example.services.UserInfoService;

public class ToggleVisibilityCallbackHandler implements CallbackQueryHandler {
    private final AuroraBot bot;
    private final UserInfoService userInfoService;

    public ToggleVisibilityCallbackHandler(AuroraBot bot, UserInfoService userInfoService) {
        this.bot = bot;
        this.userInfoService = userInfoService;
    }

    @Override
    public void handle(Long userId, Integer messageId) {
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
    }

    public String formatUserProfileMessage(Long userId, UserInfo userInfo) {
        String userAlias = bot.getUserAlias(userId);
        String contactInfo = (userAlias != null && !userAlias.equals("@null"))
                ? userAlias
                : String.format("<a href=\"tg://user?id=%d\">Профиль пользователя</a>", userId);

        String visibilityStatus = userInfo.getIsVisible()
                ? "\n✅ Ваша анкета видна."
                : "\n❌ На данный момент вашу анкету никто не видит.";

        return String.format(
                "Вот так будет выглядеть ваш профиль в сообщении, которое мы пришлём вашему собеседнику:\n⏬\n%s%s",
                userInfoService.formatUserProfile(userInfo, contactInfo),
                visibilityStatus
        );
    }
}
