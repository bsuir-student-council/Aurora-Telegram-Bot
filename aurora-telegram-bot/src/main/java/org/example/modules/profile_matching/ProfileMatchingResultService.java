package org.example.modules.profile_matching;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProfileMatchingResultService {

    private final ProfileMatchingResultRepository repository;

    @Autowired
    public ProfileMatchingResultService(ProfileMatchingResultRepository repository) {
        this.repository = repository;
    }

    public void saveResult(ProfileMatchingResult result) {
        repository.save(result);
    }
}
