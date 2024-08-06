package org.example.modules.statistics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProfileStatisticsService {

    private final ProfileStatisticsRepository profileStatisticsRepository;

    @Autowired
    public ProfileStatisticsService(ProfileStatisticsRepository profileStatisticsRepository) {
        this.profileStatisticsRepository = profileStatisticsRepository;
    }

    public void saveProfileStatistics(ProfileStatistics profileStatistics) {
        profileStatisticsRepository.save(profileStatistics);
    }

    public List<ProfileStatistics> getAllProfileStatistics() {
        return profileStatisticsRepository.findAll();
    }
}
