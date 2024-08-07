package org.example;

import lombok.NoArgsConstructor;
import org.example.models.SupportRequest;
import org.example.models.UserInfo;
import org.example.services.SupportRequestService;
import org.example.services.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PostConstruct;

@Component
@NoArgsConstructor
public class NetworkingBot extends MultiSessionTelegramBot implements CommandLineRunner {

    private final ConcurrentHashMap<Long, DialogMode> userModes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, UserInfo> userInfos = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Integer> userQuestionCounts = new ConcurrentHashMap<>();

    private UserInfoService userInfoService;
    private SupportRequestService supportRequestService;

    @Autowired
    public NetworkingBot(UserInfoService userInfoService, SupportRequestService supportRequestService) {
        this.userInfoService = userInfoService;
        this.supportRequestService = supportRequestService;
    }

    @Value("${telegram.bot.name}")
    private String botName;

    @Value("${telegram.bot.token}")
    private String botToken;

    public enum DialogMode {
        PROFILE,
        SUPPORT
    }

    @PostConstruct
    private void initializeBot() {
        initialize(botName, botToken);
    }

    @Override
    public void run(String... args) throws Exception {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(this);
        setMyCommands();
    }

    private void setMyCommands() {
        List<BotCommand> commands = List.of(
                new BotCommand("/profile", "Моя анкета"),
                new BotCommand("/help", "Помощь"),
                new BotCommand("/restart","Заново")
        );

        try {
            execute(new SetMyCommands(commands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpdateEventReceived(Update update) {
        Long userId = getUserId(update);
        String message = getMessageText(userId);
        String callbackData = getCallbackQueryButtonKey(userId);

        if (message != null && message.startsWith("/")) {
            handleCommand(userId, message);
        } else if (callbackData != null && !callbackData.isEmpty()) {
            handleCallbackQuery(userId, callbackData, update);
        } else if (message != null && !message.isEmpty()) {
            handleDialogMode(userId, message);
        }
    }

    private void handleCommand(Long userId, String command) {
        switch (command) {
            case "/start" -> handleStartCommand(userId);
            case "/profile" -> handleProfileCommand(userId);
            case "/help" -> handleHelpCommand(userId);
            case "/restart" -> handleRestartCommand(userId);
            case "/support" -> handleSupportCommand(userId);
            default -> sendTextMessage(userId, "Неизвестная команда. Попробуйте /start.");
        }
    }

    private void handleCallbackQuery(Long userId, String callbackData, Update update) {
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        switch (callbackData) {
            case "start" -> handleEditStartMessage(userId, messageId);
            case "accepted" -> handleEditInfoMessage(userId, messageId);
            case "toggle_visibility" -> handleToggleVisibility(userId, messageId);
            default -> sendTextMessage(userId, "Неизвестная команда. Попробуйте /start.");
        }
    }


    private void handleSupportCommand(Long userId) {
        Optional<SupportRequest> lastRequest = supportRequestService.getLastSupportRequest(userId);
        if (lastRequest.isPresent()) {
            LocalDateTime lastRequestTime = lastRequest.get().getCreatedAt();
            Duration duration = Duration.between(lastRequestTime, LocalDateTime.now());
            if (duration.toMinutes() < 15) {
                long minutesLeft = 15 - duration.toMinutes();
                sendTextMessage(userId, String.format("Вы уже отправили запрос в техподдержку недавно. Пожалуйста, подождите ещё %d минут.", minutesLeft));
                return;
            }
        }

        userModes.put(userId, DialogMode.SUPPORT);
        sendTextMessage(userId, "Пожалуйста, опишите вашу проблему. Максимальная длина сообщения - 2000 символов. Вы можете отправить не более одного сообщения раз в 15 минут. Если вы передумали писать, нажмите /profile.");
    }

    private void handleHelpCommand(Long userId) {
        String helpMessage = loadMessage("help");
        sendTextMessage(userId, helpMessage);
    }

    private void handleRestartCommand(Long userId) {
        userInfos.remove(userId);
        userInfoService.deleteUserInfo(userId);
        askFullName(userId);
    }

    private void handleStartCommand(Long userId) {
        //sendPhotoMessage(userId, "start", false);
        sendTextButtonsMessage(userId, loadMessage("start"), "Поехали🚀", "start");
    }

    private void handleProfileCommand(Long userId) {
        userInfoService.getUserInfoByUserId(userId).ifPresentOrElse(
                userInfo -> sendUserProfile(userId, userInfo),
                () -> sendTextMessage(userId, "Анкета не найдена. Пожалуйста, заполните анкету командой /start.")
        );
    }

    private void handleDialogMode(Long userId, String message) {
        DialogMode currentMode = userModes.getOrDefault(userId, null);
        if (currentMode == null) {
            sendTextMessage(userId, "Пожалуйста, начните с команды /start.");
            return;
        }

        switch (currentMode) {
            case PROFILE -> handleProfileDialog(userId, message);
            case SUPPORT -> handleSupportDialog(userId, message);
            default -> sendTextMessage(userId, "Неизвестный режим диалога.");
        }
    }

    private void handleSupportDialog(Long userId, String message) {
        if (message.length() > 2000) {
            sendTextMessage(userId, "Ваше сообщение слишком длинное. Пожалуйста, сократите его до 2000 символов.");
            return;
        }

        Optional<SupportRequest> lastRequest = supportRequestService.getLastSupportRequest(userId);
        if (lastRequest.isPresent()) {
            LocalDateTime lastRequestTime = lastRequest.get().getCreatedAt();
            Duration duration = Duration.between(lastRequestTime, LocalDateTime.now());
            if (duration.toMinutes() < 15) {
                long minutesLeft = 15 - duration.toMinutes();
                sendTextMessage(userId, String.format("Вы можете отправить сообщение только раз в 15 минут. Пожалуйста, подождите ещё %d минут.", minutesLeft));
                return;
            }
        }

        SupportRequest supportRequest = new SupportRequest();
        supportRequest.setUserId(userId);
        supportRequest.setMessage(message);
        supportRequest.setRequestStatus(SupportRequest.RequestStatus.OPEN);
        try {
            supportRequestService.saveSupportRequest(supportRequest);
            sendTextMessage(userId, "Ваш запрос в техподдержку успешно отправлен. Спасибо!");
            userModes.remove(userId);
        } catch (Exception e) {
            sendTextMessage(userId, "Произошла ошибка при сохранении запроса. Пожалуйста, попробуйте снова.");
        }
    }

    private void handleProfileDialog(Long userId, String message) {
        if (message.length() > 255) {
            sendTextMessage(userId, "Ваш ввод слишком длинный. Пожалуйста, сократите его до 255 символов.");
            return;
        }

        UserInfo userInfo = userInfos.get(userId);
        int questionCount = userQuestionCounts.getOrDefault(userId, 1);

        switch (questionCount) {
            case 1 -> handleNameInput(userId, userInfo, message);
            case 2 -> handleAgeInput(userId, userInfo, message);
            case 3 -> handleDiscussionTopicInput(userId, userInfo, message);
            case 4 -> handleFunFactInput(userId, userInfo, message);
            default -> sendTextMessage(userId, "Неизвестный этап анкеты.");
        }
    }

    private void handleNameInput(Long userId, UserInfo userInfo, String message) {
        userInfo.setName(message);
        userQuestionCounts.put(userId, 2);
        sendTextMessage(userId, "Пожалуйста, укажите ваш возраст.");
    }

    private void handleAgeInput(Long userId, UserInfo userInfo, String message) {
        userInfo.setAge(message);
        userQuestionCounts.put(userId, 3);
        sendTextMessage(userId, "👀 Что бы вы хотели обсудить?");
    }

    private void handleDiscussionTopicInput(Long userId, UserInfo userInfo, String message) {
        userInfo.setDiscussionTopic(message);
        userQuestionCounts.put(userId, 4);
        sendTextMessage(userId, "Пожалуйста, поделитесь интересным фактом о себе.");
    }

    private void handleFunFactInput(Long userId, UserInfo userInfo, String message) {
        userInfo.setFunFact(message);
        try {
            userInfoService.saveUserInfo(userInfo);
            sendUserProfile(userId, userInfo);
            userModes.remove(userId);
        } catch (Exception e) {
            sendTextMessage(userId, "Произошла ошибка при сохранении профиля. Пожалуйста, попробуйте снова.");
        }
    }

    private void sendUserProfile(Long userId, UserInfo userInfo) {
        String photoUrl = getUserPhotoUrl(userId);
        String profileMessage = formatUserProfileMessage(userId, userInfo);

        if (photoUrl != null) {
            sendPhotoMessage(userId, photoUrl, true);
        }
        sendTextButtonsMessage(userId, profileMessage, "Редактировать", "accepted", "Сменить статус видимости", "toggle_visibility");
    }

    private void handleEditStartMessage(Long userId, Integer messageId) {
        String updatedMessage = loadMessage("start") + "\n\n➪ Поехали🚀";
        editTextMessageWithButtons(userId, messageId, updatedMessage);
        sendTextButtonsMessage(userId, loadMessage("info"), "Принято 😊", "accepted");
    }

    private void handleEditInfoMessage(Long userId, Integer messageId) {
        String updatedMessage = loadMessage("info") + "\n\n➪ Принято 🫡";
        editTextMessageWithButtons(userId, messageId, updatedMessage);
        askFullName(userId);
    }

    private void handleToggleVisibility(Long userId, Integer messageId) {
        userInfoService.toggleVisibility(userId);
        handleEditProfileCommand(userId, messageId);
    }

    private void handleEditProfileCommand(Long userId, Integer messageId) {
        UserInfo userInfo = userInfoService.getUserInfoByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String updatedMessage = formatUserProfileMessage(userId, userInfo);
        editTextMessageWithButtons(
                userId,
                messageId,
                updatedMessage,
                "Редактировать", "accepted",
                "Сменить статус видимости", "toggle_visibility"
        );
    }

    private String formatUserProfileMessage(Long userId, UserInfo userInfo) {
        String userAlias = getUserAlias(userId);
        String contactInfo = (userAlias != null && !userAlias.equals("@null"))
                ? userAlias
                : String.format("<a href=\"tg://user?id=%d\">Профиль пользователя</a>", userId);

        String visibilityStatus = userInfo.getIsVisible()
                ? "\n✅ Ваша анкета видна."
                : "\n❌ На данный момент вашу анкету никто не видит.";

        String profileMessage = String.format(
                "Вот так будет выглядеть ваш профиль в сообщении, которое мы пришлём вашему собеседнику:\n⏬\n%s%s",
                userInfoService.formatUserProfile(userInfo, contactInfo),
                visibilityStatus
        );

        return profileMessage;
    }

    private void askFullName(Long userId) {
        userModes.put(userId, DialogMode.PROFILE);
        userQuestionCounts.put(userId, 1);

        UserInfo userInfo = userInfoService.getUserInfoByUserId(userId)
                .orElseGet(() -> {
                    UserInfo newUserInfo = new UserInfo();
                    newUserInfo.setUserId(userId);
                    return newUserInfo;
                });

        userInfos.put(userId, userInfo);
        sendPhotoMessage(userId, "name", false);

        String message = "Пожалуйста, укажите ваше имя.";
        sendTextMessage(userId, message);
    }
}
