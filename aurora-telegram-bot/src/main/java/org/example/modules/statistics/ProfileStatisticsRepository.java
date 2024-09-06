package org.example.modules.statistics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ProfileStatisticsRepository extends JpaRepository<ProfileStatistics, Long> {
    @Query("SELECT ps FROM ProfileStatistics ps WHERE ps.date >= :startDate ORDER BY ps.date DESC")
    List<ProfileStatistics> findLast7DaysStatistics(LocalDate startDate);
}
