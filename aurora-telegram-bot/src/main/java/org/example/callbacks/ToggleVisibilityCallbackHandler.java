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

        String updatedMessage = bot.formatUserProfileMessage(userId, userInfo);
        bot.editTextMessageWithButtons(
                userId,
                messageId,
                updatedMessage,
                "Редактировать", "accepted",
                "Сменить статус видимости", "toggle_visibility"
        );
    }
}
