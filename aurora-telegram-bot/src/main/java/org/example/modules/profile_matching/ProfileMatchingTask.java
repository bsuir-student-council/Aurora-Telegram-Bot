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
import java.util.*;

import static org.example.modules.profile_matching.TextSimilarity.processUserInfos;

@Component
public class ProfileMatchingTask {

    private final UserInfoService userInfoService;
    private final ProfileMatchingResultService resultService;
    private final AuroraBot auroraBot;
    private final Logger logger = LoggerFactory.getLogger(ProfileMatchingTask.class);

    @Value("${special.user.id}")
    private Long specialUserId;

    private boolean isRandomMatchingEnabled = false; // Флаг для включения случайного распределения

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

            // Фильтрация пользователей
            List<UserInfo> activeUsers = filterActiveUsers(allUsers);

            List<TextSimilarity.SimilarityPair> pairs;
            if (isRandomMatchingEnabled) {
                // Случайное распределение пользователей по парам
                pairs = getRandomPairs(activeUsers);
                logger.info("Randomly assigned pairs: {}", pairs);
            } else {
                // Нормальный ход работы через processUserInfos
                pairs = processUserInfos(activeUsers.toArray(new UserInfo[0]));
                logger.info("Similarity pairs: {}", pairs);
            }

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

    // Метод для случайного распределения
    private List<TextSimilarity.SimilarityPair> getRandomPairs(List<UserInfo> users) {
        List<TextSimilarity.SimilarityPair> pairs = new ArrayList<>();
        List<UserInfo> shuffledUsers = new ArrayList<>(users);
        Collections.shuffle(shuffledUsers, new Random());

        for (int i = 0; i < shuffledUsers.size() - 1; i += 2) {
            pairs.add(new TextSimilarity.SimilarityPair(shuffledUsers.get(i).getUserId(), shuffledUsers.get(i + 1).getUserId(), 0.0f));
        }

        if (shuffledUsers.size() % 2 != 0) {
            pairs.add(new TextSimilarity.SimilarityPair(shuffledUsers.get(shuffledUsers.size() - 1).getUserId(), specialUserId, 0.0f));
        }

        return pairs;
    }

    // Метод фильтрации активных пользователей
    private List<UserInfo> filterActiveUsers(List<UserInfo> allUsers) {
        List<UserInfo> validUserIds = allUsers.stream()
                .filter(user -> user.getUserId() != null)
                .toList();

        List<UserInfo> visibleUsers = validUserIds.stream()
                .filter(user -> Boolean.TRUE.equals(user.getIsVisible()))
                .toList();

        return visibleUsers.stream()
                .filter(user -> Boolean.FALSE.equals(user.getIsBotBlocked()) && Boolean.FALSE.equals(user.getIsBanned()))
                .toList();
    }

    private void handlePair(List<UserInfo> users, boolean[] paired, TextSimilarity.SimilarityPair pair) {
        Long userId1 = pair.userId1();
        Long userId2 = pair.userId2();

        if (userId1 == null || userId2 == null) {
            logger.error("Skipping pair due to null userId: userId1 = {}, userId2 = {}", userId1, userId2);
            return;  // Пропускаем пару, если один из пользователей имеет null userId
        }

        int index1 = findIndexByUserId(users, userId1);
        int index2 = findIndexByUserId(users, userId2);

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

                if (unpairedUser == null || unpairedUser.getUserId() == null) {
                    logger.error("Skipping unpaired user due to null user or userId: user = {}, userId = {}", unpairedUser, unpairedUser != null ? unpairedUser.getUserId() : "null");
                    continue;  // Пропускаем, если пользователь или его userId равны null
                }

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
