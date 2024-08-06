package org.example.repositories;

import org.example.models.SupportRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> {
    Optional<SupportRequest> findTopByUserIdOrderByCreatedAtDesc(Long userId);
}
