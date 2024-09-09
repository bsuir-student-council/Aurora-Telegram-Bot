package org.example.repositories;

import org.example.models.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {
    Optional<UserInfo> findByUserId(Long userId);

    long countByIsVisibleTrue();

    List<UserInfo> findAllByIsVisibleTrue();

    List<UserInfo> findByRole(UserInfo.Role role);

    long countByIsBannedTrue();

    long countByIsBotBlockedTrue();

    long countByIsVisibleTrueAndIsBannedFalseAndIsBotBlockedFalse();
}
