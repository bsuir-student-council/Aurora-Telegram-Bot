package org.example.modules.profile_matching;

import org.example.models.UserInfo;
import org.example.AuroraBot;
import org.example.services.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.example.modules.profile_matching.TextSimilarity.processUserInfos;

@Component
public class ProfileMatchingTask {

    private final UserInfoService userInfoService;
    private final ProfileMatchingResultService resultService;
    private final AuroraBot auroraBot;
    private final Logger logger = LoggerFactory.getLogger(ProfileMatchingTask.class);

    @Value("${special.user.id}")
    private Long specialUserId;

    @Autowired
    public ProfileMatchingTask(UserInfoService userInfoService, ProfileMatchingResultService resultService, AuroraBot auroraBot) {
        this.userInfoService = userInfoService;
        this.resultService = resultService;
        this.auroraBot = auroraBot;
    }

    @Scheduled(cron = "0 0 11 ? * MON") // Runs every Monday at 11:00 AM
    public void sendMatchedProfiles() {
        ProfileMatchingResult result = new ProfileMatchingResult();
        result.setExecutionTime(LocalDateTime.now());
        result.setMatchedUsers(new ArrayList<>());
        result.setUnpairedUsers(new ArrayList<>());
        result.setStatus("SUCCESS");

        try {
            // Получаем всех пользователей
            List<UserInfo> allUsers = userInfoService.getAllUsers();
            logger.info("Total users before filtering: {}", allUsers.size());

            // Этап 1: Исключаем пользователей с userId == null
            List<UserInfo> validUserIds = allUsers.stream()
                    .filter(user -> user.getUserId() != null)
                    .toList();
            logger.info("Users with valid userId after filtering: {}, filtered out: {}", validUserIds.size(), allUsers.size() - validUserIds.size());

            // Этап 2: Фильтруем видимых пользователей
            List<UserInfo> visibleUsers = validUserIds.stream()
                    .filter(user -> Boolean.TRUE.equals(user.getIsVisible()))
                    .toList();
            logger.info("Visible users after filtering: {}, filtered out: {}", visibleUsers.size(), validUserIds.size() - visibleUsers.size());

            // Этап 3: Исключаем пользователей, которые заблокировали бота
            List<UserInfo> notBlockedByBotUsers = visibleUsers.stream()
                    .filter(user -> Boolean.FALSE.equals(user.getIsBotBlocked()))
                    .toList();
            logger.info("Users not blocked by bot after filtering: {}, filtered out: {}", notBlockedByBotUsers.size(), visibleUsers.size() - notBlockedByBotUsers.size());

            // Этап 4: Исключаем забаненных пользователей
            List<UserInfo> activeUsers = notBlockedByBotUsers.stream()
                    .filter(user -> Boolean.FALSE.equals(user.getIsBanned()))
                    .toList();
            logger.info("Active users after filtering: {}, filtered out: {}", activeUsers.size(), notBlockedByBotUsers.size() - activeUsers.size());

            // Обрабатываем отфильтрованных пользователей
            List<TextSimilarity.SimilarityPair> pairs = processUserInfos(activeUsers.toArray(new UserInfo[0]));
            logger.info("Similarity pairs: {}", pairs);

            boolean[] paired = new boolean[activeUsers.size()];

            pairs.forEach(pair -> {
                try {
                    handlePair(activeUsers, paired, pair);
                    result.getMatchedUsers().add(pair.userId1() + " <-> " + pair.userId2());
                } catch (Exception e) {
                    logger.error("Error handling pair: {} <-> {}", pair.userId1(), pair.userId2(), e);
                }
            });

            handleUnpaired(activeUsers, paired, result);
        } catch (IOException | org.apache.lucene.queryparser.classic.ParseException e) {
            logger.error("Error processing text similarity: ", e);
            result.setStatus("FAILED");
            result.setErrorMessage(e.getMessage());
        } finally {
            resultService.saveResult(result);
            logger.info("User profiles sent based on similarity pairs.");
        }
    }

    private void handlePair(List<UserInfo> users, boolean[] paired, TextSimilarity.SimilarityPair pair) {
        int index1 = findIndexByUserId(users, pair.userId1());
        int index2 = findIndexByUserId(users, pair.userId2());

        if (index1 != -1 && index2 != -1 && !paired[index1] && !paired[index2]) {
            sendUserProfile(users.get(index1).getUserId(), users.get(index2));
            sendUserProfile(users.get(index2).getUserId(), users.get(index1));

            paired[index1] = true;
            paired[index2] = true;
        }
    }

    private void handleUnpaired(List<UserInfo> users, boolean[] paired, ProfileMatchingResult result) {
        for (int i = 0; i < users.size(); i++) {
            if (!paired[i]) {
                UserInfo unpairedUser = users.get(i);
                try {
                    sendUserProfile(specialUserId, unpairedUser);

                    Optional<UserInfo> specialUserInfo = userInfoService.getUserInfoByUserId(specialUserId);
                    specialUserInfo.ifPresent(info -> sendUserProfile(unpairedUser.getUserId(), info));

                    result.getUnpairedUsers().add(unpairedUser.getUserId());
                    logger.info("Unpaired profile sent to special chat ID.");
                } catch (Exception e) {
                    logger.error("Error sending unpaired profile to user: {}", unpairedUser.getUserId(), e);
                }
                break;
            }
        }
    }

    private int findIndexByUserId(List<UserInfo> users, Long userId) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUserId().equals(userId)) {
                return i;
            }
        }
        return -1;
    }

    private void sendUserProfile(Long userId, UserInfo userInfo) {
        try {
            String photoUrl = auroraBot.getUserPhotoUrl(userInfo.getUserId());
            String userAlias = auroraBot.getUserAlias(userInfo.getUserId());
            boolean isAliasValid = userAlias != null && !userAlias.equals("@null");

            String contactInfo = isAliasValid ? userAlias :
                    String.format("<a href=\"tg://user?id=%d\">Профиль пользователя</a>", userId);

            String profileMessage = String.format(
                    """
                            Привет! 👋
                            Ваш собеседник на эту неделю:
                            %s
                            Рекомендуем не откладывать и договориться о встрече сразу. Также рекомендуем первый раз встретиться на территории университета 💻
                                
                            Появятся вопросы — пишите в /support 😉""",
                    userInfoService.formatUserProfile(userInfo, contactInfo)
            );

            boolean photoSent = true;
            if (photoUrl != null) {
                photoSent = auroraBot.sendPhotoMessage(userId, photoUrl);
            }

            boolean textSent = auroraBot.sendTextMessage(userId, profileMessage);

            if (!photoSent || !textSent) {
                logger.warn("Failed to send profile to user {}. Marking user as potentially blocked.", userId);
                userInfo.setIsBotBlocked(true);
                userInfoService.saveUserInfo(userInfo);
            }
        } catch (Exception e) {
            logger.error("Error sending user profile to user: {}", userId, e);
        }
    }
}
