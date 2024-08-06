package org.example.modules.statistics;

import org.example.repositories.UserInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ProfileStatisticsTask {

    private final UserInfoRepository userInfoRepository;
    private final ProfileStatisticsService profileStatisticsService;

    @Autowired
    public ProfileStatisticsTask(UserInfoRepository userInfoRepository, ProfileStatisticsService profileStatisticsService) {
        this.userInfoRepository = userInfoRepository;
        this.profileStatisticsService = profileStatisticsService;
    }

    @Scheduled(cron = "0 0 18 * * *") // 18:00
    public void collectProfileStatistics() {
        long totalProfiles = userInfoRepository.count();
        long activeProfiles = userInfoRepository.countByIsVisibleTrue();

        ProfileStatistics profileStatistics = new ProfileStatistics();
        profileStatistics.setDate(LocalDate.now());
        profileStatistics.setTotalProfiles(totalProfiles);
        profileStatistics.setActiveProfiles(activeProfiles);

        profileStatisticsService.saveProfileStatistics(profileStatistics);
    }
}
