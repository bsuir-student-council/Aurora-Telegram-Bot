package org.example.callbacks;

import org.example.AuroraBot;
import org.example.interfaces.CallbackQueryHandler;

import java.util.logging.Logger;

public class StartCallbackHandler implements CallbackQueryHandler {
    private static final Logger logger = Logger.getLogger(StartCallbackHandler.class.getName());
    private final AuroraBot bot;

    public StartCallbackHandler(AuroraBot bot) {
        this.bot = bot;
    }

    @Override
    public void handle(Long userId, Integer messageId) {
        try {
            String startMessage = """
                    Привет! Я Аврора, твой бот для Random Coffee! ☕️

                    Я помогу тебе найти новых друзей и интересных собеседников.
                    Каждую неделю я буду подбирать для тебя нового интересного человека на основе твоих интересов.

                    Готов к неожиданным знакомствам?""";
            String updatedMessage = startMessage + "\n\n➪ Поехали🚀";
            bot.editTextMessageWithButtons(userId, messageId, updatedMessage);

            String infoMessage = """
                    > Вы заполняете анкету из трёх вопросов.

                    > Каждую неделю наш алгоритм анализирует анкеты и выбирает для вас идеального партнёра для беседы.

                    > День и время, формат встречи вы выбираете сами — свяжитесь с новым знакомым и договоритесь о встрече; если планы изменились, предупредите партнёра заранее.

                    > Если собеседник не отвечает, напишите в бот, и мы подберём вам нового собеседника.""";
            bot.sendTextButtonsMessage(userId, infoMessage, "Принято 😊", "accepted");

            logger.info("Handled start callback for userId: " + userId);
        } catch (Exception e) {
            logger.severe("Error handling start callback for userId: " + userId + " - " + e.getMessage());
        }
    }
}
