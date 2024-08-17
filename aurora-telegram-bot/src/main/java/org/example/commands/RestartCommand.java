package org.example.commands;

import org.example.AuroraBot;
import org.example.interfaces.BotCommandHandler;
import org.example.models.UserInfo;
import org.example.services.UserInfoService;

import java.util.Map;

public class RestartCommand implements BotCommandHandler {
    private final AuroraBot bot;
    private final UserInfoService userInfoService;
    private final Map<Long, UserInfo> userInfos;

    public RestartCommand(AuroraBot bot,
                          UserInfoService userInfoService,
                          Map<Long, UserInfo> userInfos) {
        this.bot = bot;
        this.userInfoService = userInfoService;
        this.userInfos = userInfos;
    }

    @Override
    public void execute(Long userId) {
        userInfos.remove(userId);
        userInfoService.deleteUserInfo(userId);
        bot.askFullName(userId);
    }
}
