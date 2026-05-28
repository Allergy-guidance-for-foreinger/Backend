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
import java.util.List;

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
    public boolean existsNonCascadeUserReference(Long userId) {
        String sql = """
                select exists(
                    select 1
                    from meal_menu_confirmed_ingredient mmci
                    where mmci.confirmed_by_user_id = :userId
                ) or exists(
                    select 1
                    from meal_menu_confirmation_history mmch
                    where mmch.changed_by_user_id = :userId
                )
                """;
        Boolean exists = namedParameterJdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource("userId", userId),
                Boolean.class
        );
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public boolean softDeleteActiveById(Long userId) {
        List<Long> impactedReviewIds = findReviewIdsLikedByUser(userId);
        deleteReviewLikesByUserId(userId);
        recalculateReviewCounters(impactedReviewIds);
        return userJpaRepository.softDeleteActiveById(userId) > 0;
    }

    @Override
    public boolean hardDeleteActiveById(Long userId) {
        List<Long> impactedReviewIds = findImpactedReviewIdsByUserInteraction(userId);
        userOauthAccountJpaRepository.deleteByUserId(userId);
        boolean deleted = userJpaRepository.hardDeleteActiveById(userId) > 0;
        if (deleted) {
            recalculateReviewCounters(impactedReviewIds);
        }
        return deleted;
    }

    @Override
    public User createGoogleUser(String providerUserId, String providerEmail, String name) {
        User savedUser = userJpaRepository.save(User.createForFirstGoogleLogin(providerEmail, name));
        userOauthAccountJpaRepository.save(UserOauthAccount.createGoogleAccount(savedUser, providerUserId, providerEmail));
        return savedUser;
    }

    private List<Long> findImpactedReviewIdsByUserInteraction(Long userId) {
        String sql = """
                select distinct review_id
                from (
                    select mrl.review_id
                    from menu_review_like mrl
                    where mrl.user_id = :userId
                    union
                    select mrc.review_id
                    from menu_review_comment mrc
                    where mrc.user_id = :userId
                ) impacted
                """;
        return namedParameterJdbcTemplate.query(
                sql,
                new MapSqlParameterSource("userId", userId),
                (rs, rowNum) -> rs.getLong("review_id")
        );
    }

    private List<Long> findReviewIdsLikedByUser(Long userId) {
        String sql = """
                select distinct review_id
                from menu_review_like
                where user_id = :userId
                """;
        return namedParameterJdbcTemplate.query(
                sql,
                new MapSqlParameterSource("userId", userId),
                (rs, rowNum) -> rs.getLong("review_id")
        );
    }

    private void deleteReviewLikesByUserId(Long userId) {
        String sql = """
                delete from menu_review_like
                where user_id = :userId
                """;
        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource("userId", userId));
    }

    private void recalculateReviewCounters(List<Long> reviewIds) {
        if (reviewIds == null || reviewIds.isEmpty()) {
            return;
        }

        String sql = """
                with target_reviews as (
                    select unnest(:reviewIds::bigint[]) as review_id
                ),
                like_counts as (
                    select tr.review_id,
                           count(mrl.id) as like_count
                    from target_reviews tr
                    left join menu_review_like mrl on mrl.review_id = tr.review_id
                    group by tr.review_id
                ),
                comment_counts as (
                    select tr.review_id,
                           count(mrc.id) as comment_count
                    from target_reviews tr
                    left join menu_review_comment mrc
                           on mrc.review_id = tr.review_id
                          and mrc.deleted_at is null
                    group by tr.review_id
                )
                update menu_review mr
                set like_count = lc.like_count,
                    comment_count = cc.comment_count,
                    updated_at = now()
                from like_counts lc
                join comment_counts cc on cc.review_id = lc.review_id
                where mr.id = lc.review_id
                  and mr.deleted_at is null
                """;

        namedParameterJdbcTemplate.update(
                sql,
                new MapSqlParameterSource("reviewIds", reviewIds.toArray(Long[]::new))
        );
    }
}

