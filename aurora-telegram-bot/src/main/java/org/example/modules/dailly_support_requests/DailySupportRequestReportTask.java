package org.example.modules.dailly_support_requests;

import org.example.AuroraBot;
import org.example.models.SupportRequest;
import org.example.models.UserInfo;
import org.example.services.SupportRequestService;
import org.example.services.UserInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DailySupportRequestReportTask {
    private static final Logger logger = LoggerFactory.getLogger(DailySupportRequestReportTask.class);

    private final UserInfoService userInfoService;
    private final SupportRequestService supportRequestService;
    private final AuroraBot auroraBot;

    @Autowired
    public DailySupportRequestReportTask(UserInfoService userInfoService,
                                         SupportRequestService supportRequestService,
                                         AuroraBot auroraBot) {
        this.userInfoService = userInfoService;
        this.supportRequestService = supportRequestService;
        this.auroraBot = auroraBot;
    }

    @Scheduled(cron = "0 0 19 * * *") // 19:00
    public void sendDailySupportRequestReport() {
        logger.info("Starting daily support request report task.");

        long openRequests = supportRequestService.countByStatus(SupportRequest.RequestStatus.OPEN);
        long inProgressRequests = supportRequestService.countByStatus(SupportRequest.RequestStatus.IN_PROGRESS);

        String message = String.format(
                """
                        Ежедневный отчет по заявкам:

                        Количество открытых заявок: %d
                        Количество заявок в работе: %d
                        """,
                openRequests, inProgressRequests
        );

        List<UserInfo> admins = userInfoService.getUsersByRole(UserInfo.Role.ADMIN);
        logger.debug("Retrieved {} admins.", admins.size());

        admins.forEach(admin -> {
            auroraBot.sendTextMessage(admin.getUserId(), message);
            logger.debug("Sent daily report to admin: {}", admin.getUserId());
        });

        logger.info("Daily support request report sent to all admins.");
    }
}
