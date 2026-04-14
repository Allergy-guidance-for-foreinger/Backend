package com.mealguide.mealguide_api.login.application.port;

import com.mealguide.mealguide_api.login.domain.User;
import com.mealguide.mealguide_api.login.domain.UserRole;

import java.util.Optional;

public interface UserQueryPort {
    Optional<User> findByGoogleAccount(String providerUserId, String providerEmail);

    Optional<User> findById(Long userId);

    Optional<UserRole> findActiveRoleById(Long userId);

    boolean existsActiveById(Long userId);

    User createGoogleUser(String providerUserId, String providerEmail, String name);
}
