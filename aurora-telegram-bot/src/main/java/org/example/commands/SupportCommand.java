package org.example.commands;

import org.example.AuroraBot;
import org.example.interfaces.BotCommandHandler;
import org.example.enums.DialogMode;
import org.example.models.SupportRequest;
import org.example.services.SupportRequestService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.logging.Logger;

public class SupportCommand implements BotCommandHandler {
    private static final Logger logger = Logger.getLogger(SupportCommand.class.getName());

    private static final long MIN_INTERVAL_MINUTES = 15;

    private final AuroraBot bot;
    private final SupportRequestService supportRequestService;

    public SupportCommand(AuroraBot bot, SupportRequestService supportRequestService) {
        this.bot = bot;
        this.supportRequestService = supportRequestService;
    }

    @Override
    public void handle(Long userId) {
        if (isRequestTooFrequent(userId)) {
            return;
        }

        bot.getUserModes().put(userId, DialogMode.SUPPORT);
        bot.sendTextMessage(userId, "Пожалуйста, опишите вашу проблему. Максимальная длина сообщения - 2000 символов. " +
                "Вы можете отправить не более одного сообщения раз в 15 минут. Если вы передумали писать, нажмите /profile.");
        logger.info("Support mode activated for userId: " + userId);
    }

    private boolean isRequestTooFrequent(Long userId) {
        Optional<SupportRequest> lastRequest = supportRequestService.getLastSupportRequest(userId);
        return lastRequest.map(request -> {
            LocalDateTime lastRequestTime = request.getCreatedAt();
            Duration duration = Duration.between(lastRequestTime, LocalDateTime.now());
            long minutesLeft = MIN_INTERVAL_MINUTES - duration.toMinutes();
            if (minutesLeft > 0) {
                bot.sendTextMessage(userId, String.format("Вы можете отправить сообщение только раз в 15 минут. Пожалуйста, подождите ещё %d минут.", minutesLeft));
                logger.warning("Support request too frequent for userId: " + userId);
                return true;
            }
            return false;
        }).orElse(false);
    }
}
