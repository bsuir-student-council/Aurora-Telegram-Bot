package org.example.callbacks;

import org.example.AuroraBot;
import org.example.MultiSessionTelegramBot;
import org.example.enums.DialogMode;
import org.example.interfaces.CallbackQueryHandler;
import org.example.models.UserInfo;
import org.example.services.UserInfoService;

public class AcceptedCallbackHandler implements CallbackQueryHandler {
    private final AuroraBot bot;
    private final UserInfoService userInfoService;

    public AcceptedCallbackHandler(AuroraBot bot, UserInfoService userInfoService) {
        this.bot = bot;
        this.userInfoService = userInfoService;
    }

    @Override
    public void handle(Long userId, Integer messageId) {
        String updatedMessage = MultiSessionTelegramBot.loadMessage("info") + "\n\n➪ Принято 🫡";
        bot.editTextMessageWithButtons(userId, messageId, updatedMessage);
        bot.getUserModes().put(userId, DialogMode.PROFILE);
        bot.getUserQuestionCounts().put(userId, 1);

        UserInfo userInfo = userInfoService.getUserInfoByUserId(userId)
                .orElseGet(() -> {
                    UserInfo newUserInfo = new UserInfo();
                    newUserInfo.setUserId(userId);
                    return newUserInfo;
                });

        bot.getUserInfos().put(userId, userInfo);
        bot.sendPhotoMessage(userId, "name", false);

        String message = "Пожалуйста, укажите ваше имя.";
        bot.sendTextMessage(userId, message);
    }
}
