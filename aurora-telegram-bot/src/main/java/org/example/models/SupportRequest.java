package org.example.models;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "support_request")
public class SupportRequest {

    public enum RequestStatus {
        OPEN,
        IN_PROGRESS,
        CLOSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;

    @Size(max = 2000, message = "Сообщение не должно превышать 2000 символов.")
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RequestStatus requestStatus = RequestStatus.OPEN;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
