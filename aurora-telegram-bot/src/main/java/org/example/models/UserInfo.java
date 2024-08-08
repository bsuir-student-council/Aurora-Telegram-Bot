package org.example.models;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

@Entity
@Getter
@Setter
@Table(name = "user_info")
public class UserInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;

    @Size(max = 255, message = "Имя не должно превышать 255 символов.")
    private String name;

    @Size(max = 255, message = "Возраст не должен превышать 255 символов.")
    private String age;

    @Size(max = 500, message = "Тема обсуждения не должна превышать 500 символов.")
    private String discussionTopic;

    @Size(max = 500, message = "Интересный факт не должен превышать 500 символов.")
    private String funFact;

    private Boolean isVisible = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    public enum Role {
        ADMIN,
        USER
    }
}
