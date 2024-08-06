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
        List<UserInfo> users = userInfoService.getAllUsers().stream().filter(UserInfo::getIsVisible).toList();
        UserInfo[] userInfos = users.toArray(new UserInfo[0]);

        try {
            List<TextSimilarity.SimilarityPair> pairs = processUserInfos(userInfos);
            logger.info(pairs.toString());
            boolean[] paired = new boolean[userInfos.length];

            for (TextSimilarity.SimilarityPair pair : pairs) {
                int index1 = findIndexByUserId(userInfos, pair.userId1());
                int index2 = findIndexByUserId(userInfos, pair.userId2());

                if (!paired[index1] && !paired[index2]) {
                    // Отправка анкеты первого пользователя второму
                    sendUserProfile(userInfos[index1].getUserId(), userInfos[index2]);
                    // Отправка анкеты второго пользователя первому
                    sendUserProfile(userInfos[index2].getUserId(), userInfos[index1]);

                    paired[index1] = true;
                    paired[index2] = true;
                }
            }

            // Проверка на оставшуюся анкету
            for (int i = 0; i < userInfos.length; i++) {
                if (!paired[i]) {
                    // Отправка оставшейся анкеты
                    sendUserProfile(specialUserId, userInfos[i]);
                    sendUserProfile(userInfos[i].getUserId(), userInfoService.getUserInfoByUserId(specialUserId).orElseThrow());
                    logger.info("Unpaired profile sent to special chat ID.");
                    break;
                }
            }

            logger.info("User profiles sent based on similarity pairs.");

        } catch (IOException | org.apache.lucene.queryparser.classic.ParseException e) {
            logger.error("Error processing text similarity: ", e);
        }
    }

    private int findIndexByUserId(UserInfo[] userInfos, Long userId) {
        for (int i = 0; i < userInfos.length; i++) {
            if (userInfos[i].getUserId().equals(userId)) {
                return i;
            }
        }
        return -1;
    }

    private void sendUserProfile(Long userId, UserInfo userInfo) {
        String photoUrl = networkingBot.getUserPhotoUrl(userInfo.getUserId());
        String userAlias = networkingBot.getUserAliasWithoutUpdate(userInfo.getUserId());
        boolean isAliasValid = userAlias != null && !userAlias.equals("@null");
        String contactInfo = isAliasValid ? userAlias : String.format("<a href=\"tg://user?id=%d\">Профиль пользователя</a>", userId);

        String profileMessage = "Привет!👋\n" +
                "Ваш собеседник на эту неделю:\n" + userInfoService.formatUserProfile(userInfo, contactInfo);

        if (photoUrl != null) {
            networkingBot.sendPhotoMessage(userId, photoUrl, true);
        }
        networkingBot.sendTextMessage(userId, profileMessage);
    }
}
