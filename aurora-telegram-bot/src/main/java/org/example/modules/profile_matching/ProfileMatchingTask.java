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
        List<UserInfo> users = userInfoService.getVisibleUsers();
        ProfileMatchingResult result = new ProfileMatchingResult();
        result.setExecutionTime(LocalDateTime.now());
        result.setMatchedUsers(new ArrayList<>());
        result.setUnpairedUsers(new ArrayList<>());

        try {
            List<TextSimilarity.SimilarityPair> pairs = processUserInfos(users.toArray(new UserInfo[0]));
            logger.info("Similarity pairs: {}", pairs);

            boolean[] paired = new boolean[users.size()];

            pairs.forEach(pair -> {
                handlePair(users, paired, pair);
                result.getMatchedUsers().add(pair.userId1() + " <-> " + pair.userId2());
            });

            handleUnpaired(users, paired, result);
            result.setStatus("SUCCESS");
        } catch (IOException | org.apache.lucene.queryparser.classic.ParseException e) {
            logger.error("Error processing text similarity: ", e);
            result.setStatus("FAILED");
            result.setErrorMessage(e.getMessage());
        }

        resultService.saveResult(result);
        logger.info("User profiles sent based on similarity pairs.");
    }

    private void handlePair(List<UserInfo> users, boolean[] paired, TextSimilarity.SimilarityPair pair) {
        int index1 = findIndexByUserId(users, pair.userId1());
        int index2 = findIndexByUserId(users, pair.userId2());

        if (!paired[index1] && !paired[index2]) {
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
                sendUserProfile(specialUserId, unpairedUser);

                Optional<UserInfo> specialUserInfo = userInfoService.getUserInfoByUserId(specialUserId);
                specialUserInfo.ifPresent(info -> sendUserProfile(unpairedUser.getUserId(), info));

                result.getUnpairedUsers().add(unpairedUser.getUserId());
                logger.info("Unpaired profile sent to special chat ID.");
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

        if (photoUrl != null) {
            auroraBot.sendPhotoMessage(userId, photoUrl);
        }
        auroraBot.sendTextMessage(userId, profileMessage);
    }
}
