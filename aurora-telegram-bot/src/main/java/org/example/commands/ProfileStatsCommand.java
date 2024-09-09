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

        StringBuilder statsMessage = new StringBuilder("Подробная статистика за последние 7 дней:\n\n");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        for (int i = stats.size() - 1; i >= 0; i--) {
            ProfileStatistics stat = stats.get(i);
            String formattedDate = dateFormatter.format(stat.getDate());
            statsMessage.append(String.format("%-12s Всего профилей: %-5d Активных: %-5d Забанено: %-5d Заблокировали бота: %-5d Участвуют: %-5d\n",
                    formattedDate, stat.getTotalProfiles(), stat.getActiveProfiles(),
                    stat.getBannedProfiles(), stat.getBotBlockedProfiles(), stat.getEligibleProfiles()));
        }

        bot.sendTextMessage(userId, statsMessage.toString());
    }
}
