package org.example.commands;

import org.example.AuroraBot;
import org.example.interfaces.BotCommandHandler;
import org.example.modules.statistics.ProfileStatistics;
import org.example.modules.statistics.ProfileStatisticsRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ProfileStatsCommand implements BotCommandHandler {
    private final AuroraBot bot;
    private final ProfileStatisticsRepository profileStatisticsRepository;

    public ProfileStatsCommand(AuroraBot bot, ProfileStatisticsRepository profileStatisticsRepository) {
        this.bot = bot;
        this.profileStatisticsRepository = profileStatisticsRepository;
    }

    @Override
    public void handle(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(7);

        List<ProfileStatistics> stats = profileStatisticsRepository.findLast7DaysStatistics(startDate);

        if (stats.isEmpty()) {
            bot.sendTextMessage(userId, "Нет данных для отображения за последние 7 дней.");
            return;
        }

        StringBuilder statsMessage = new StringBuilder("Статистика за последние 7 дней:\n\n");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        for (ProfileStatistics stat : stats) {
            String formattedDate = dateFormatter.format(stat.getDate());
            statsMessage.append(String.format("%-12s Профилей всего: %-5d Активных профилей: %-5d\n",
                    formattedDate, stat.getTotalProfiles(), stat.getActiveProfiles()));
        }

        bot.sendTextMessage(userId, statsMessage.toString());
    }
}
