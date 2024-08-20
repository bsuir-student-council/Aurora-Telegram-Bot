package org.example.callbacks;

import org.example.AuroraBot;
import org.example.MultiSessionTelegramBot;
import org.example.enums.DialogMode;
import org.example.interfaces.CallbackQueryHandler;
import org.example.models.UserInfo;
import org.example.services.UserInfoService;

import java.util.logging.Logger;

public class AcceptedCallbackHandler implements CallbackQueryHandler {
    private final AuroraBot bot;
    private final UserInfoService userInfoService;
    private static final Logger logger = Logger.getLogger(AcceptedCallbackHandler.class.getName());

    public AcceptedCallbackHandler(AuroraBot bot, UserInfoService userInfoService) {
        this.bot = bot;
        this.userInfoService = userInfoService;
    }

    @Override
    public void handle(Long userId, Integer messageId) {
        String baseMessage = MultiSessionTelegramBot.loadMessage("info");
        String updatedMessage = String.format("%s\n\n➪ Принято 🫡", baseMessage);

        bot.editTextMessageWithButtons(userId, messageId, updatedMessage);

        bot.getUserModes().put(userId, DialogMode.PROFILE);
        bot.getUserQuestionCounts().put(userId, 1);

        UserInfo userInfo = userInfoService.getUserInfoByUserId(userId)
                .orElseGet(() -> createNewUserInfo(userId));

        bot.getUserInfos().put(userId, userInfo);

        bot.sendPhotoMessage(userId, "name", false);
        bot.sendTextMessage(userId, "Пожалуйста, укажите ваше имя.");

        logger.info("Handled accepted callback for userId: " + userId);
    }

    private UserInfo createNewUserInfo(Long userId) {
        UserInfo newUserInfo = new UserInfo();
        newUserInfo.setUserId(userId);
        return newUserInfo;
    }
}
