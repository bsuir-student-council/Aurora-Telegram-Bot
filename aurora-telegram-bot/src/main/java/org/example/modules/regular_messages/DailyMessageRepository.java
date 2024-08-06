package org.example.modules.regular_messages;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DailyMessageRepository extends JpaRepository<DailyMessage, Long> {
    Optional<DailyMessage> findFirstBySentFalseOrderByCreatedAtAsc();
}
