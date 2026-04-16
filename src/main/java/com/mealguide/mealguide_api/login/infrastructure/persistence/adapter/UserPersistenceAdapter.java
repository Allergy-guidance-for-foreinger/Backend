package com.mealguide.mealguide_api.login.infrastructure.persistence.adapter;

import com.mealguide.mealguide_api.login.domain.User;
import com.mealguide.mealguide_api.login.domain.UserOauthAccount;
import com.mealguide.mealguide_api.login.domain.UserRole;
import com.mealguide.mealguide_api.login.domain.UserStatus;
import com.mealguide.mealguide_api.login.application.port.UserQueryPort;
import com.mealguide.mealguide_api.login.infrastructure.persistence.repository.UserOauthAccountJpaRepository;
import com.mealguide.mealguide_api.login.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserQueryPort {

    private static final String GOOGLE_PROVIDER = "GOOGLE";
    private static final UserStatus ACTIVE_STATUS = UserStatus.ACTIVE;

    private final UserJpaRepository userJpaRepository;
    private final UserOauthAccountJpaRepository userOauthAccountJpaRepository;

    @Override
    public Optional<User> findByGoogleAccount(String providerUserId, String providerEmail) {
        Optional<User> userByProviderSubject = userOauthAccountJpaRepository
                .findByProviderAndProviderUserIdAndUserDeletedAtIsNullAndUserStatus(GOOGLE_PROVIDER, providerUserId, ACTIVE_STATUS)
                .map(oauthAccount -> oauthAccount.getUser());

        if (userByProviderSubject.isPresent() || providerEmail == null || providerEmail.isBlank()) {
            return userByProviderSubject;
        }

        return userOauthAccountJpaRepository
                .findFirstByProviderAndProviderEmailAndUserDeletedAtIsNullAndUserStatus(GOOGLE_PROVIDER, providerEmail, ACTIVE_STATUS)
                .map(oauthAccount -> oauthAccount.getUser());
    }

    @Override
    public Optional<User> findById(Long userId) {
        return userJpaRepository.findByIdAndDeletedAtIsNullAndStatus(userId, ACTIVE_STATUS);
    }

    @Override
    public Optional<UserRole> findActiveRoleById(Long userId) {
        return userJpaRepository.findRoleByIdAndDeletedAtIsNullAndStatus(userId, ACTIVE_STATUS);
    }

    @Override
    public boolean existsActiveById(Long userId) {
        return userJpaRepository.existsByIdAndDeletedAtIsNullAndStatus(userId, ACTIVE_STATUS);
    }

    @Override
    public User createGoogleUser(String providerUserId, String providerEmail, String name) {
        User savedUser = userJpaRepository.save(User.createForFirstGoogleLogin(providerEmail, name));
        userOauthAccountJpaRepository.save(UserOauthAccount.createGoogleAccount(savedUser, providerUserId, providerEmail));
        return savedUser;
    }
}

