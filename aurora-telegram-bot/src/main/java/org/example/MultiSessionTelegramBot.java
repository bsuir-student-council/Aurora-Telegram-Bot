package org.example;

import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final Logger logger = LoggerFactory.getLogger(MultiSessionTelegramBot.class);

    /**
     * Initializes the bot with the specified name and token.
     *
     * @param name  the name of the bot
     * @param token the authentication token of the bot
     */
    public void initialize(String name, String token) {
        this.name = name;
        this.token = token;
    }

    /**
     * Returns the username of the bot.
     *
     * @return the bot username
     */
    @Override
    public String getBotUsername() {
        return name;
    }

    /**
     * Returns the token of the bot.
     *
     * @return the bot token
     */
    @Override
    public String getBotToken() {
        return token;
    }

    /**
     * Handles an incoming update from Telegram.
     *
     * @param update the update object
     */
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

    /**
     * Processes the received update event. Can be overridden for custom behavior.
     *
     * @param update the update object
     */
    public void onUpdateEventReceived(Update update) {
        // do nothing
    }

    /**
     * Retrieves the callback query button key for a specific user.
     *
     * @param userId the user ID
     * @return the callback query data or an empty string if not available
     */
    public String getCallbackQueryButtonKey(Long userId) {
        Update update = userUpdates.get(userId);
        return update.hasCallbackQuery() ? update.getCallbackQuery().getData() : "";
    }

    /**
     * Sends a text message to a user.
     *
     * @param userId the user ID
     * @param text   the text message
     */
    public boolean sendTextMessage(Long userId, String text) {
        SendMessage command = createApiSendMessageCommandWithChat(userId, text);
        command.setParseMode(ParseMode.HTML);

        try {
            executeTelegramApiMethod(command);
            return true;
        } catch (Exception e) {
            logger.error("Failed to send text message. UserId: {}, Error: {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * Sends a photo message to a user.
     *
     * @param userId   the user ID
     * @param photoKey the photo key or file ID
     */
    public boolean sendPhotoMessage(Long userId, String photoKey) {
        SendPhoto photoMessage = new SendPhoto();
        photoMessage.setPhoto(new InputFile(photoKey));
        photoMessage.setParseMode(ParseMode.HTML);
        photoMessage.setChatId(userId);

        try {
            executeTelegramApiMethod(photoMessage);
            return true;
        } catch (Exception e) {
            logger.error("Failed to send photo message. UserId: {}, Error: {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * Sends a text message with inline buttons to a user.
     *
     * @param userId  the user ID
     * @param text    the text message
     * @param buttons an array of button names and callback data
     * @return boolean status of message sending (true if successful, false if failed)
     */
    public boolean sendTextButtonsMessage(Long userId, String text, String... buttons) {
        SendMessage command = createApiSendMessageCommandWithChat(userId, text);
        command.setParseMode(ParseMode.HTML);
        if (buttons.length > 0)
            attachButtons(command, List.of(buttons));

        try {
            executeTelegramApiMethod(command);
            return true;
        } catch (TelegramApiException e) {
            logger.error("Failed to send text buttons message. UserId: {}, Error: {}", userId, e.getMessage());
            return false;
        }
    }


    /**
     * Attaches inline buttons to a message.
     *
     * @param message the message object
     * @param buttons a list of button names and callback data
     * @param <T>     the type of the message object
     */
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

    /**
     * Loads a message text from a file.
     *
     * @param name the name of the message file
     * @return the message text
     */
    public static String loadMessage(String name) {
        try {
            var is = ClassLoader.getSystemResourceAsStream("messages/" + name + ".txt");
            assert is != null;
            return new String(is.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException("Can't load message!");
        }
    }

    /**
     * Loads an image from a file.
     *
     * @param name the name of the image file
     * @return the input stream of the image
     */
    public static InputStream loadImage(String name) {
        try {
            return ClassLoader.getSystemResourceAsStream("images/" + name + ".jpg");
        } catch (Exception e) {
            throw new RuntimeException("Can't load photo!");
        }
    }

    /**
     * Retrieves the user ID from an update.
     *
     * @param update the update object
     * @return the user ID or null if not available
     */
    public Long getUserId(Update update) {
        if (update.hasMessage() && update.getMessage().getFrom() != null) {
            return update.getMessage().getFrom().getId();
        }

        if (update.hasCallbackQuery() && update.getCallbackQuery().getFrom() != null) {
            return update.getCallbackQuery().getFrom().getId();
        }

        return null;
    }

    /**
     * Retrieves the user alias from Telegram chat information.
     *
     * @param userId the user ID
     * @return the user alias or null if not available
     */
    public String getUserAlias(Long userId) {
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

    /**
     * Retrieves the user's profile photo URL.
     *
     * @param userId the user ID
     * @return the file ID of the photo or null if not available
     */
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

    /**
     * Retrieves the text of a message from an update.
     *
     * @param userId the user ID
     * @return the message text or an empty string if not available
     */
    public String getMessageText(Long userId) {
        Update update = userUpdates.get(userId);
        if (update.hasMessage() && update.getMessage().hasText()) {
            return update.getMessage().getText();
        } else if (update.hasCallbackQuery() && update.getCallbackQuery().getData() != null) {
            return update.getCallbackQuery().getData();
        }
        return "";
    }

    /**
     * Edits an existing text message with new text and optional buttons.
     *
     * @param userId    the user ID
     * @param messageId the message ID to edit
     * @param newText   the new text message
     * @param buttons   an array of button names and callback data
     * @return boolean status of message editing (true if successful, false if failed)
     */
    public boolean editTextMessageWithButtons(Long userId, Integer messageId, String newText, String... buttons) {
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(userId);
        editMessage.setMessageId(messageId);
        editMessage.setText(newText);
        editMessage.setParseMode(ParseMode.HTML);
        if (buttons.length > 0)
            attachButtons(editMessage, List.of(buttons));

        try {
            executeTelegramApiMethod(editMessage);
            return true;
        } catch (TelegramApiException e) {
            logger.error("Failed to edit message with buttons. UserId: {}, MessageId: {}, Error: {}", userId, messageId, e.getMessage());
            return false;
        }
    }

    /**
     * Creates a SendMessage command with chat ID and text.
     *
     * @param userId the user ID
     * @param text   the text message
     * @return the SendMessage object
     */
    private SendMessage createApiSendMessageCommandWithChat(Long userId, String text) {
        SendMessage message = new SendMessage();
        message.setText(text);
        message.setChatId(userId);
        return message;
    }

    /**
     * Executes a Telegram API method for sending a photo message.
     *
     * @param message the SendPhoto object
     */
    private void executeTelegramApiMethod(SendPhoto message) throws TelegramApiException {
        super.execute(message);
    }

    /**
     * Executes a Telegram API method for sending text-based API methods.
     *
     * @param method   the BotApiMethod object
     * @param <T>      the type of the method result
     * @param <Method> the type of the method
     */
    private <T extends Serializable, Method extends BotApiMethod<T>> void executeTelegramApiMethod(Method method) throws TelegramApiException {
        super.sendApiMethod(method);
    }
}
