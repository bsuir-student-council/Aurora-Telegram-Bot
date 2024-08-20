package org.example.dialogs;

import org.example.AuroraBot;
import org.example.interfaces.DialogHandler;
import org.example.models.UserInfo;
import org.example.services.UserInfoService;

import java.util.logging.Logger;

public class ProfileDialogHandler implements DialogHandler {
    private static final Logger logger = Logger.getLogger(ProfileDialogHandler.class.getName());

    private static final int MAX_INPUT_LENGTH = 255;

    private final AuroraBot bot;
    private final UserInfoService userInfoService;

    public ProfileDialogHandler(AuroraBot bot, UserInfoService userInfoService) {
        this.bot = bot;
        this.userInfoService = userInfoService;
    }

    @Override
    public void handle(Long userId, String message) {
        if (message.length() > MAX_INPUT_LENGTH) {
            bot.sendTextMessage(userId, "Ваш ввод слишком длинный. Пожалуйста, сократите его до 255 символов.");
            return;
        }

        UserInfo userInfo = bot.getUserInfos().get(userId);
        int questionCount = bot.getUserQuestionCounts().getOrDefault(userId, 1);

        switch (questionCount) {
            case 1 -> handleNameInput(userId, userInfo, message);
            case 2 -> handleAgeInput(userId, userInfo, message);
            case 3 -> handleDiscussionTopicInput(userId, userInfo, message);
            case 4 -> handleFunFactInput(userId, userInfo, message);
            default -> bot.sendTextMessage(userId, "Неизвестный этап анкеты.");
        }
    }

    private void handleNameInput(Long userId, UserInfo userInfo, String message) {
        userInfo.setName(message);
        bot.getUserQuestionCounts().put(userId, 2);
        bot.sendTextMessage(userId, "Пожалуйста, укажите ваш возраст.");
    }

    private void handleAgeInput(Long userId, UserInfo userInfo, String message) {
        userInfo.setAge(message);
        bot.getUserQuestionCounts().put(userId, 3);
        bot.sendTextMessage(userId, "👀 Что бы вы хотели обсудить?");
    }

    private void handleDiscussionTopicInput(Long userId, UserInfo userInfo, String message) {
        userInfo.setDiscussionTopic(message);
        bot.getUserQuestionCounts().put(userId, 4);
        bot.sendTextMessage(userId, "Пожалуйста, поделитесь интересным фактом о себе.");
    }

    private void handleFunFactInput(Long userId, UserInfo userInfo, String message) {
        userInfo.setFunFact(message);
        try {
            userInfoService.saveUserInfo(userInfo);
            sendUserProfile(userId, userInfo);
            bot.getUserModes().remove(userId);
            logger.info("Profile saved for userId: " + userId);
        } catch (Exception e) {
            bot.sendTextMessage(userId, "Произошла ошибка при сохранении профиля. Пожалуйста, попробуйте снова.");
            logger.severe("Error saving profile for userId: " + userId + " - " + e.getMessage());
        }
    }

    private void sendUserProfile(Long userId, UserInfo userInfo) {
        String photoUrl = bot.getUserPhotoUrl(userId);
        String profileMessage = buildProfileMessage(userId, userInfo);

        if (photoUrl != null) {
            bot.sendPhotoMessage(userId, photoUrl, true);
        }
        bot.sendTextButtonsMessage(userId, profileMessage, "Редактировать", "accepted", "Сменить статус видимости", "toggle_visibility");
    }

    private String buildProfileMessage(Long userId, UserInfo userInfo) {
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
