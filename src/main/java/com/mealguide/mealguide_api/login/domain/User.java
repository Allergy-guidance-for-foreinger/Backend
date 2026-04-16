package com.mealguide.mealguide_api.login.domain;

import com.mealguide.mealguide_api.global.config.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE users SET status = 'INACTIVE' WHERE id = ?")
@SQLRestriction("status <> 'INACTIVE'")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id")
    private Long schoolId;

    @Column(length = 255)
    private String email;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "language_code", length = 10)
    private String languageCode;

    @Column(name = "religious_code", length = 30)
    private String religiousCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public boolean isDeleted() {
        return status == UserStatus.INACTIVE;
    }

    public static User createForFirstGoogleLogin(String email, String name) {
        User user = new User();
        user.schoolId = null;
        user.email = email;
        user.name = (name == null || name.isBlank()) ? email : name;
        user.languageCode = null;
        user.religiousCode = null;
        user.status = UserStatus.ACTIVE;
        user.role = UserRole.defaultRole();
        user.onboardingCompleted = false;
        user.deletedAt = null;
        return user;
    }
}

