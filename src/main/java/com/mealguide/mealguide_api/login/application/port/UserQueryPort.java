package com.mealguide.mealguide_api.login.application.port;

import com.mealguide.mealguide_api.login.domain.User;
import com.mealguide.mealguide_api.login.domain.UserRole;

import java.util.Optional;

public interface UserQueryPort {
    Optional<User> findByGoogleAccount(String providerUserId);

    boolean existsInactiveGoogleAccount(String providerUserId);

    Optional<User> findById(Long userId);

    Optional<UserRole> findActiveRoleById(Long userId);

    boolean existsActiveById(Long userId);

    boolean existsNonCascadeUserReference(Long userId);

    boolean softDeleteActiveById(Long userId);

    boolean hardDeleteActiveById(Long userId);

    User createGoogleUser(String providerUserId, String providerEmail);
}

