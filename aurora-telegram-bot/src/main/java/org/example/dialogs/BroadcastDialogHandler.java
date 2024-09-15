package org.example.dialogs;

import org.example.AuroraBot;
import org.example.interfaces.DialogHandler;
import org.example.models.UserInfo;
import org.example.services.UserInfoService;

import java.util.List;
import java.util.logging.Logger;

public class BroadcastDialogHandler implements DialogHandler {

    private static final Logger logger = Logger.getLogger(BroadcastDialogHandler.class.getName());
    private final AuroraBot bot;
    private final UserInfoService userInfoService;

    public BroadcastDialogHandler(AuroraBot bot, UserInfoService userInfoService) {
        this.bot = bot;
        this.userInfoService = userInfoService;
    }

    @Override
    public void handle(Long userId, String message) {
        if ("Отмена".equalsIgnoreCase(message.trim())) {
            bot.sendTextMessage(userId, "Рассылка отменена.");
            bot.getUserModes().remove(userId);
            logger.info("Broadcast canceled by user: " + userId);
            return;
        }

        int successCount = 0;
        int failureCount = 0;

        try {
            List<UserInfo> visibleUsers = userInfoService.getAllUsers().stream()
                    //.filter(UserInfo::getIsVisible)
                    .toList();

            for (UserInfo user : visibleUsers) {
                try {
                    bot.sendTextMessage(user.getUserId(), message);
                    logger.info("Sent broadcast message to user: " + user.getUserId());
                    successCount++;
                } catch (Exception e) {
                    logger.severe("Failed to send message to user: " + user.getUserId() + ". Error: " + e.getMessage());
                    failureCount++;
                }
            }

            bot.sendTextMessage(userId, "Сообщение успешно отправлено всем видимым пользователям.");
            bot.getUserModes().remove(userId);

        } catch (Exception e) {
            bot.sendTextMessage(userId, "Произошла ошибка при отправке сообщения.");
            logger.severe("Error sending broadcast message: " + e.getMessage());
        } finally {
            logger.info("Broadcast summary: " + successCount + " messages sent successfully, " + failureCount + " failed.");
        }
    }
}
