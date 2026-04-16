package com.mealguide.mealguide_api.login.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.login.domain.UserOauthAccount;
import com.mealguide.mealguide_api.login.domain.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserOauthAccountJpaRepository extends JpaRepository<UserOauthAccount, Long> {
    Optional<UserOauthAccount> findByProviderAndProviderUserIdAndUserDeletedAtIsNullAndUserStatus(
            String provider,
            String providerUserId,
            UserStatus status
    );

    Optional<UserOauthAccount> findFirstByProviderAndProviderEmailAndUserDeletedAtIsNullAndUserStatus(
            String provider,
            String providerEmail,
            UserStatus status
    );
}

