package org.example.services;

import org.example.models.SupportRequest;
import org.example.repositories.SupportRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SupportRequestService {

    private final SupportRequestRepository supportRequestRepository;

    @Autowired
    public SupportRequestService(SupportRequestRepository supportRequestRepository) {
        this.supportRequestRepository = supportRequestRepository;
    }

    public void saveSupportRequest(SupportRequest supportRequest) {
        supportRequestRepository.save(supportRequest);
    }

    public Optional<SupportRequest> getLastSupportRequest(Long userId) {
        return supportRequestRepository.findTopByUserIdOrderByCreatedAtDesc(userId);
    }

    public long countByStatus(SupportRequest.RequestStatus status) {
        return supportRequestRepository.countByRequestStatus(status);
    }
}
