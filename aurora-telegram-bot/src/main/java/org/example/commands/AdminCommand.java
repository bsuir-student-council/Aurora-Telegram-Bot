package org.example.commands;

import org.example.AuroraBot;
import org.example.interfaces.BotCommandHandler;
import org.example.models.UserInfo;
import org.example.services.UserInfoService;

import java.util.Optional;

public class AdminCommand implements BotCommandHandler {
    private final AuroraBot bot;
    private final UserInfoService userInfoService;

    public AdminCommand(AuroraBot bot, UserInfoService userInfoService) {
        this.bot = bot;
        this.userInfoService = userInfoService;
    }

    @Override
    public void handle(Long userId) {
        Optional<UserInfo> userInfoOptional = userInfoService.getUserInfoByUserId(userId);
        if (userInfoOptional.isEmpty() || userInfoOptional.get().getRole() != UserInfo.Role.ADMIN) {
            bot.sendTextMessage(userId, "У вас нет прав для выполнения этой команды.");
            return;
        }

        String userCommands = """
                Команды пользователя:
                - /start: Инициализация работы с ботом и начало взаимодействия.
                - /profile: Просмотр текущей анкеты пользователем.
                - /help: Получение справочной информации о функционале бота и доступных командах.
                - /support: Отправка запроса в техническую поддержку.
                """;

        String adminCommands = """
                Команды администратора:
                - /admin: Отображение всех доступных команд с кратким описанием.
                - /list_admins: Вывести список всех администраторов.
                - /promote: Сделать пользователя администратором.
                """;

        bot.sendTextMessage(userId, userCommands + "\n" + adminCommands);
    }
}
