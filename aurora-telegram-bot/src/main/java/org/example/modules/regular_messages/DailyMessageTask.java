package org.example.modules.regular_messages;

import org.example.models.UserInfo;
import org.example.services.UserInfoService;
import org.example.AuroraBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Component
public class DailyMessageTask {

    private static final Logger logger = LoggerFactory.getLogger(DailyMessageTask.class);

    private final UserInfoService userInfoService;
    private final DailyMessageService dailyMessageService;
    private final AuroraBot auroraBot;

    @Autowired
    public DailyMessageTask(UserInfoService userInfoService, DailyMessageService dailyMessageService, AuroraBot auroraBot) {
        this.userInfoService = userInfoService;
        this.dailyMessageService = dailyMessageService;
        this.auroraBot = auroraBot;
    }

    @Scheduled(cron = "0 0 7 * * *") // 07:00
    public void sendDailyMessage() {
        logger.info("Starting daily message task.");

        List<UserInfo> users = userInfoService.getAllUsers();
        logger.debug("Retrieved {} users.", users.size());

        dailyMessageService.getUnsentDailyMessage().ifPresentOrElse(dailyMessage -> {
            String text = dailyMessage.getText();
            users.forEach(user -> {
                auroraBot.sendTextMessage(user.getUserId(), text);
                logger.debug("Sent message to user: {}", user.getUserId());
            });
            dailyMessage.setSent(true);
            dailyMessageService.saveDailyMessage(dailyMessage);
            logger.info("Daily message sent to all users.");
        }, () -> logger.info("No unsent daily messages found."));
    }
}
