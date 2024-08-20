package org.example.dialogs;

import org.example.AuroraBot;
import org.example.interfaces.DialogHandler;
import org.example.models.SupportRequest;
import org.example.services.SupportRequestService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.logging.Logger;

public class SupportDialogHandler implements DialogHandler {
    private static final Logger logger = Logger.getLogger(SupportDialogHandler.class.getName());
    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final int MIN_INTERVAL_MINUTES = 15;

    private final AuroraBot bot;
    private final SupportRequestService supportRequestService;

    public SupportDialogHandler(AuroraBot bot, SupportRequestService supportRequestService) {
        this.bot = bot;
        this.supportRequestService = supportRequestService;
    }

    @Override
    public void handle(Long userId, String message) {
        if (isMessageTooLong(message)) {
            bot.sendTextMessage(userId, "Ваше сообщение слишком длинное. Пожалуйста, сократите его до 2000 символов.");
            return;
        }

        if (isRequestTooFrequent(userId)) {
            return;
        }

        createAndSaveSupportRequest(userId, message);
    }

    private boolean isMessageTooLong(String message) {
        return message.length() > MAX_MESSAGE_LENGTH;
    }

    private boolean isRequestTooFrequent(Long userId) {
        Optional<SupportRequest> lastRequest = supportRequestService.getLastSupportRequest(userId);
        if (lastRequest.isPresent()) {
            LocalDateTime lastRequestTime = lastRequest.get().getCreatedAt();
            Duration duration = Duration.between(lastRequestTime, LocalDateTime.now());
            long minutesLeft = MIN_INTERVAL_MINUTES - duration.toMinutes();
            if (minutesLeft > 0) {
                bot.sendTextMessage(userId, String.format(
                        "Вы можете отправить сообщение только раз в 15 минут. Пожалуйста, подождите ещё %d минут.", minutesLeft));
                logger.warning("Support request too frequent for userId: " + userId);
                return true;
            }
        }
        return false;
    }

    private void createAndSaveSupportRequest(Long userId, String message) {
        SupportRequest supportRequest = new SupportRequest();
        supportRequest.setUserId(userId);
        supportRequest.setMessage(message);

        try {
            supportRequestService.saveSupportRequest(supportRequest);
            bot.sendTextMessage(userId, "Ваш запрос в техподдержку успешно отправлен. Спасибо!");
            bot.getUserModes().remove(userId);
            logger.info("Support request saved for userId: " + userId);
        } catch (Exception e) {
            bot.sendTextMessage(userId, "Произошла ошибка при сохранении запроса. Пожалуйста, попробуйте снова.");
            logger.severe("Error saving support request for userId: " + userId + " - " + e.getMessage());
        }
    }
}
