package org.example.interfaces;

public interface CallbackQueryHandler {
    void handle(Long userId, Integer messageId);
}
