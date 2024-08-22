package org.example.modules.profile_matching;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "profile_matching_result")
public class ProfileMatchingResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime executionTime;

    @ElementCollection
    @CollectionTable(name = "matched_users", joinColumns = @JoinColumn(name = "result_id"))
    @Column(name = "user_pair")
    private List<String> matchedUsers;

    @ElementCollection
    @CollectionTable(name = "unpaired_users", joinColumns = @JoinColumn(name = "result_id"))
    @Column(name = "user_id")
    private List<Long> unpairedUsers;

    private String status;

    private String errorMessage;
}
