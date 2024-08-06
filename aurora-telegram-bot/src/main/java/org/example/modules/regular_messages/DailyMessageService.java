package org.example.modules.regular_messages;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DailyMessageService {

    private final DailyMessageRepository dailyMessageRepository;

    @Autowired
    public DailyMessageService(DailyMessageRepository dailyMessageRepository) {
        this.dailyMessageRepository = dailyMessageRepository;
    }

    public Optional<DailyMessage> getUnsentDailyMessage() {
        return dailyMessageRepository.findFirstBySentFalseOrderByCreatedAtAsc();
    }

    public DailyMessage saveDailyMessage(DailyMessage dailyMessage) {
        dailyMessageRepository.save(dailyMessage);
        return dailyMessage;
    }

    public List<DailyMessage> getAllDailyMessages() {
        return dailyMessageRepository.findAll();
    }

    public Optional<DailyMessage> updateDailyMessage(Long id, DailyMessage dailyMessage) {
        return dailyMessageRepository.findById(id)
                .map(existingDailyMessage -> {
                    dailyMessage.setId(existingDailyMessage.getId());
                    return dailyMessageRepository.save(dailyMessage);
                });
    }

    public boolean deleteDailyMessage(Long id) {
        if (dailyMessageRepository.existsById(id)) {
            dailyMessageRepository.deleteById(id);
            return true;
        } else {
            return false;
        }
    }
}
