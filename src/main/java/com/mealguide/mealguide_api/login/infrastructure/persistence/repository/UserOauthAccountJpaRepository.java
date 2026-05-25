package com.mealguide.mealguide_api.login.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.login.domain.UserOauthAccount;
import com.mealguide.mealguide_api.login.domain.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    boolean existsByProviderAndProviderUserIdAndUserStatus(
            String provider,
            String providerUserId,
            UserStatus status
    );

    boolean existsByProviderAndProviderEmailAndUserStatus(
            String provider,
            String providerEmail,
            UserStatus status
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from UserOauthAccount account
            where account.user.id = :userId
            """)
    int deleteByUserId(@Param("userId") Long userId);
}

