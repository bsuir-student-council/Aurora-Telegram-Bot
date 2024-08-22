package org.example.modules.profile_matching;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfileMatchingResultRepository extends JpaRepository<ProfileMatchingResult, Long> {
}
