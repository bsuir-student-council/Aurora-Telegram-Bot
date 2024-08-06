package org.example;

import lombok.NoArgsConstructor;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.GetUserProfilePhotos;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor
public class MultiSessionTelegramBot extends TelegramLongPollingBot {

    private String name;
    private String token;
    private final ConcurrentHashMap<Long, Update> userUpdates = new ConcurrentHashMap<>();

    public void initialize(String name, String token) {
        this.name = name;
        this.token = token;
    }

    @Override
    public String getBotUsername() {
        return name;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public final void onUpdateReceived(Update update) {
        Long userId = getUserId(update);
        userUpdates.put(userId, update);
        try {
            onUpdateEventReceived(update);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Обработка события обновления
    public void onUpdateEventReceived(Update update) {
        // do nothing
    }

    // Получение Callback Query Button Key
    public String getCallbackQueryButtonKey(Long userId) {
        Update update = userUpdates.get(userId);
        return update.hasCallbackQuery() ? update.getCallbackQuery().getData() : "";
    }

    // Отправка текстового сообщения
    public void sendTextMessage(Long userId, String text) {
        SendMessage command = createApiSendMessageCommandWithChat(userId, text);
        command.setParseMode(ParseMode.HTML);
        executeTelegramApiMethod(command);
    }

    public void sendPhotoMessage(Long userId, String photoKey, boolean isFileId) {
        // Отправка сообщения с фото, текстом и кнопками
        SendPhoto photoMessage = new SendPhoto();

        if (isFileId) {
            photoMessage.setPhoto(new InputFile(photoKey));
        } else {
            InputStream is = loadImage(photoKey);
            photoMessage.setPhoto(new InputFile(is, photoKey));
        }

        photoMessage.setParseMode(ParseMode.HTML);
        photoMessage.setChatId(userId);

        executeTelegramApiMethod(photoMessage);
    }

    // Отправка текстового сообщения с кнопками
    public void sendTextButtonsMessage(Long userId, String text, String... buttons) {
        SendMessage command = createApiSendMessageCommandWithChat(userId, text);
        command.setParseMode(ParseMode.HTML);
        if (buttons.length > 0)
            attachButtons(command, List.of(buttons));

        executeTelegramApiMethod(command);
    }

    // Универсальный метод для прикрепления кнопок
    private <T> void attachButtons(T message, List<String> buttons) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (int i = 0; i < buttons.size(); i += 2) {
            String buttonName = buttons.get(i);
            String buttonValue = buttons.get(i + 1);

            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(buttonName);
            button.setCallbackData(buttonValue);

            keyboard.add(List.of(button));
        }

        markup.setKeyboard(keyboard);

        if (message instanceof SendMessage) {
            ((SendMessage) message).setReplyMarkup(markup);
        } else if (message instanceof EditMessageText) {
            ((EditMessageText) message).setReplyMarkup(markup);
        } else {
            throw new IllegalArgumentException("Unsupported message type");
        }
    }

    // Загрузка текста сообщения из файла
    public static String loadMessage(String name) {
        try {
            var is = ClassLoader.getSystemResourceAsStream("messages/" + name + ".txt");
            return new String(is.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException("Can't load message!");
        }
    }

    // Загрузка изображения из файла
    public static InputStream loadImage(String name) {
        try {
            return ClassLoader.getSystemResourceAsStream("images/" + name + ".jpg");
        } catch (Exception e) {
            throw new RuntimeException("Can't load photo!");
        }
    }

    // Получение ID пользователя
    public Long getUserId(Update update) {
        if (update.hasMessage() && update.getMessage().getFrom() != null) {
            return update.getMessage().getFrom().getId();
        }

        if (update.hasCallbackQuery() && update.getCallbackQuery().getFrom() != null) {
            return update.getCallbackQuery().getFrom().getId();
        }

        return null;
    }

    // Получение алиаса пользователя
    public String getUserAlias(Long userId) {
        Update update = userUpdates.get(userId);
        if (update.hasMessage() && update.getMessage().getFrom() != null) {
            String userName = update.getMessage().getFrom().getUserName();
            return userName != null ? "@" + userName : null;
        }

        if (update.hasCallbackQuery() && update.getCallbackQuery().getFrom() != null) {
            String userName = update.getCallbackQuery().getFrom().getUserName();
            return userName != null ? "@" + userName : null;
        }

        return null;
    }

    public String getUserAliasWithoutUpdate(Long userId) {
        GetChat getChat = new GetChat();
        getChat.setChatId(userId.toString());

        try {
            Chat chat = execute(getChat);
            return chat.getUserName() != null ? "@" + chat.getUserName() : null;
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getUserPhotoUrl(Long userId) {
        try {
            GetUserProfilePhotos getUserProfilePhotos = new GetUserProfilePhotos();
            getUserProfilePhotos.setUserId(userId);

            UserProfilePhotos photos = execute(getUserProfilePhotos);
            if (photos.getTotalCount() > 0 && !photos.getPhotos().isEmpty()) {
                List<PhotoSize> photoSizes = photos.getPhotos().get(0);
                if (!photoSizes.isEmpty()) {
                    return photoSizes.get(0).getFileId();
                }
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return null;
    }


    // Получение текста сообщения
    public String getMessageText(Long userId) {
        Update update = userUpdates.get(userId);
        if (update.hasMessage() && update.getMessage().hasText()) {
            return update.getMessage().getText();
        } else if (update.hasCallbackQuery() && update.getCallbackQuery().getData() != null) {
            return update.getCallbackQuery().getData();
        }
        return "";
    }

    public void editTextMessageWithButtons(Long userId, Integer messageId, String newText, String... buttons) {
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(userId);
        editMessage.setMessageId(messageId);
        editMessage.setText(newText);
        editMessage.setParseMode(ParseMode.HTML);
        if (buttons.length > 0)
            attachButtons(editMessage, List.of(buttons));

        executeTelegramApiMethod(editMessage);
    }

    private SendMessage createApiSendMessageCommandWithChat(Long userId, String text) {
        SendMessage message = new SendMessage();
        message.setText(text);
        message.setChatId(userId);
        return message;
    }

    private void executeTelegramApiMethod(SendPhoto message) {
        try {
            super.execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    // Выполнение метода Telegram API для текстовых сообщений
    private <T extends Serializable, Method extends BotApiMethod<T>> void executeTelegramApiMethod(Method method) {
        try {
            super.sendApiMethod(method);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
