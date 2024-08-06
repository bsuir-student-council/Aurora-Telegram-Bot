package org.example.modules.statistics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfileStatisticsRepository extends JpaRepository<ProfileStatistics, Long> {
}
