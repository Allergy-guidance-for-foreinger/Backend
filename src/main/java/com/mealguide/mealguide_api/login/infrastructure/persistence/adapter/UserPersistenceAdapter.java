package com.mealguide.mealguide_api.login.infrastructure.persistence.adapter;

import com.mealguide.mealguide_api.login.domain.User;
import com.mealguide.mealguide_api.login.domain.UserOauthAccount;
import com.mealguide.mealguide_api.login.domain.UserRole;
import com.mealguide.mealguide_api.login.domain.UserStatus;
import com.mealguide.mealguide_api.login.application.port.UserQueryPort;
import com.mealguide.mealguide_api.login.infrastructure.persistence.repository.UserOauthAccountJpaRepository;
import com.mealguide.mealguide_api.login.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserQueryPort {

    private static final String GOOGLE_PROVIDER = "GOOGLE";
    private static final UserStatus ACTIVE_STATUS = UserStatus.ACTIVE;
    private static final UserStatus INACTIVE_STATUS = UserStatus.INACTIVE;

    private final UserJpaRepository userJpaRepository;
    private final UserOauthAccountJpaRepository userOauthAccountJpaRepository;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

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
    public boolean existsInactiveGoogleAccount(String providerUserId, String providerEmail) {
        boolean existsBySubject = userOauthAccountJpaRepository.existsByProviderAndProviderUserIdAndUserStatus(
                GOOGLE_PROVIDER,
                providerUserId,
                INACTIVE_STATUS
        );
        if (existsBySubject) {
            return true;
        }

        if (providerEmail == null || providerEmail.isBlank()) {
            return false;
        }

        return userOauthAccountJpaRepository.existsByProviderAndProviderEmailAndUserStatus(
                GOOGLE_PROVIDER,
                providerEmail,
                INACTIVE_STATUS
        );
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
    public boolean softDeleteActiveById(Long userId) {
        softDeleteReviewsByUserId(userId);
        softDeleteReviewCommentsByUserId(userId);
        return userJpaRepository.softDeleteActiveById(userId) > 0;
    }

    @Override
    public boolean hardDeleteActiveById(Long userId) {
        userOauthAccountJpaRepository.deleteByUserId(userId);
        return userJpaRepository.hardDeleteActiveById(userId) > 0;
    }

    @Override
    public User createGoogleUser(String providerUserId, String providerEmail, String name) {
        User savedUser = userJpaRepository.save(User.createForFirstGoogleLogin(providerEmail, name));
        userOauthAccountJpaRepository.save(UserOauthAccount.createGoogleAccount(savedUser, providerUserId, providerEmail));
        return savedUser;
    }

    private void softDeleteReviewCommentsByUserId(Long userId) {
        String sql = """
                with soft_deleted as (
                    update menu_review_comment
                    set deleted_at = now(),
                        updated_at = now()
                    where user_id = :userId
                      and deleted_at is null
                    returning review_id
                ),
                target_reviews as (
                    select distinct review_id
                    from soft_deleted
                ),
                recalculated as (
                    select tr.review_id,
                           count(c.id) as active_comment_count
                    from target_reviews tr
                    left join menu_review_comment c
                           on c.review_id = tr.review_id
                          and c.deleted_at is null
                    group by tr.review_id
                )
                update menu_review mr
                set comment_count = r.active_comment_count,
                    updated_at = now()
                from recalculated r
                where mr.id = r.review_id
                """;
        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource("userId", userId));
    }

    private void softDeleteReviewsByUserId(Long userId) {
        String sql = """
                update menu_review
                set deleted_at = now(),
                    updated_at = now()
                where user_id = :userId
                  and deleted_at is null
                """;
        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource("userId", userId));
    }
}

