package org.example.commands;

import org.example.AuroraBot;
import org.example.interfaces.BotCommandHandler;
import org.example.models.UserInfo;
import org.example.services.UserInfoService;

public class AdminCommand implements BotCommandHandler {

    private static final String NO_PERMISSION_MESSAGE = "У вас нет прав для выполнения этой команды.";
    private static final String USER_COMMANDS = """
            Команды пользователя:
            - /start: Инициализация работы с ботом и начало взаимодействия.
            - /profile: Просмотр текущей анкеты пользователем.
            - /help: Получение справочной информации о функционале бота и доступных командах.
            - /support: Отправка запроса в техническую поддержку.
            """;

    private static final String ADMIN_COMMANDS = """
            Команды администратора:
            - /admin: Отображение всех доступных команд с кратким описанием.
            - /list_admins: Вывести список всех администраторов.
            - /promote: Сделать пользователя администратором.
            - /match: Запустить процесс подбора профилей пользователей.
            - /profile_stats: Просмотреть статистику профилей за последние 7 дней.
            - /broadcast: Отправить сообщение всем пользователям.
            """;

    private final AuroraBot bot;
    private final UserInfoService userInfoService;

    public AdminCommand(AuroraBot bot, UserInfoService userInfoService) {
        this.bot = bot;
        this.userInfoService = userInfoService;
    }

    @Override
    public void handle(Long userId) {
        userInfoService.getUserInfoByUserId(userId).ifPresentOrElse(
                userInfo -> checkAdminAndSendCommands(userId, userInfo),
                () -> bot.sendTextMessage(userId, NO_PERMISSION_MESSAGE)
        );
    }

    private void checkAdminAndSendCommands(Long userId, UserInfo userInfo) {
        if (userInfo.getRole() == UserInfo.Role.ADMIN) {
            bot.sendTextMessage(userId, USER_COMMANDS + "\n" + ADMIN_COMMANDS);
        } else {
            bot.sendTextMessage(userId, NO_PERMISSION_MESSAGE);
        }
    }
}
