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

    @Column(name = "email_encrypted", columnDefinition = "TEXT")
    private String emailEncrypted;

    @Column(name = "email_hash", length = 64)
    private String emailHash;

    @Column(name = "country_code", length = 10)
    private String countryCode;

    @Column(name = "language_code", length = 10)
    private String languageCode;

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

    public static User createForFirstGoogleLogin(String emailEncrypted, String emailHash) {
        User user = new User();
        user.schoolId = null;
        user.emailEncrypted = emailEncrypted;
        user.emailHash = emailHash;
        user.countryCode = null;
        user.languageCode = null;
        user.status = UserStatus.ACTIVE;
        user.role = UserRole.defaultRole();
        user.onboardingCompleted = false;
        user.deletedAt = null;
        return user;
    }
}

