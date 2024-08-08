package org.example.modules.profile_matching;

import org.example.models.UserInfo;
import org.example.NetworkingBot;
import org.example.services.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.example.modules.profile_matching.TextSimilarity.processUserInfos;

@Component
public class ProfileMatchingTask {

    private final UserInfoService userInfoService;
    private final NetworkingBot networkingBot;
    private final Logger logger = LoggerFactory.getLogger(ProfileMatchingTask.class);

    @Value("${special.user.id}")
    private Long specialUserId;

    @Autowired
    public ProfileMatchingTask(UserInfoService userInfoService, NetworkingBot networkingBot) {
        this.userInfoService = userInfoService;
        this.networkingBot = networkingBot;
    }

    @Scheduled(cron = "0 0 11 * * ?") // 11:00
    public void sendMatchedProfiles() {
        List<UserInfo> users = userInfoService.getVisibleUsers();

        try {
            List<TextSimilarity.SimilarityPair> pairs = processUserInfos(users.toArray(new UserInfo[0]));
            logger.info("Similarity pairs: {}", pairs);

            boolean[] paired = new boolean[users.size()];

            pairs.forEach(pair -> handlePair(users, paired, pair));

            handleUnpaired(users, paired);

            logger.info("User profiles sent based on similarity pairs.");
        } catch (IOException | org.apache.lucene.queryparser.classic.ParseException e) {
            logger.error("Error processing text similarity: ", e);
        }
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

    private void handleUnpaired(List<UserInfo> users, boolean[] paired) {
        for (int i = 0; i < users.size(); i++) {
            if (!paired[i]) {
                UserInfo unpairedUser = users.get(i);
                sendUserProfile(specialUserId, unpairedUser);

                Optional<UserInfo> specialUserInfo = userInfoService.getUserInfoByUserId(specialUserId);
                specialUserInfo.ifPresent(info -> sendUserProfile(unpairedUser.getUserId(), info));

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
        String photoUrl = networkingBot.getUserPhotoUrl(userInfo.getUserId());
        String userAlias = networkingBot.getUserAlias(userInfo.getUserId());
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
            networkingBot.sendPhotoMessage(userId, photoUrl, true);
        }
        networkingBot.sendTextMessage(userId, profileMessage);
    }
}
