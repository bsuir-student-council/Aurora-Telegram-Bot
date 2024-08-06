package org.example.services;

import org.example.models.UserInfo;
import org.example.repositories.UserInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserInfoService {

    private final UserInfoRepository userInfoRepository;

    @Autowired
    public UserInfoService(UserInfoRepository userInfoRepository) {
        this.userInfoRepository = userInfoRepository;
    }

    public List<UserInfo> getAllUsers() {
        return userInfoRepository.findAll();
    }

    public Optional<UserInfo> getUserInfoByUserId(Long userId) {
        return userInfoRepository.findByUserId(userId);
    }

    public void saveUserInfo(UserInfo userInfo) {
        userInfoRepository.save(userInfo);
    }

    public void deleteUserInfo(Long userId) {
        userInfoRepository.findByUserId(userId).ifPresent(userInfoRepository::delete);
    }

    public void toggleVisibility(Long userId) {
        userInfoRepository.findByUserId(userId).ifPresent(userInfo -> {
            userInfo.setIsVisible(!userInfo.getIsVisible());
            userInfoRepository.save(userInfo);
        });
    }

    public String formatUserProfile(UserInfo userInfo, String contactInfo) {

        return String.format("""
                        %s
                        Возраст: %s

                        Что интересно: %s
                        Фан-факт: %s

                        Напишите собеседнику в Telegram – %s
                        Рекомендуем не откладывать и договориться о встрече сразу. Также рекомендуем первый раз встретиться на территории университета 💻 

                        Появятся вопросы — пишите в support 😉
                        """,
                userInfo.getName(),
                userInfo.getAge(),
                userInfo.getDiscussionTopic(),
                userInfo.getFunFact(),
                contactInfo
        );
    }
}
