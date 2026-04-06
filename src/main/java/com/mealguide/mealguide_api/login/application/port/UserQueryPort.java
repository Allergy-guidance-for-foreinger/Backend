package com.mealguide.mealguide_api.login.application.port;

import com.mealguide.mealguide_api.login.domain.User;

import java.util.Optional;

public interface UserQueryPort {
    Optional<User> findByGoogleAccount(String providerUserId, String providerEmail);

    Optional<User> findById(Long userId);

    User createGoogleUser(String providerUserId, String providerEmail, String name);
}
