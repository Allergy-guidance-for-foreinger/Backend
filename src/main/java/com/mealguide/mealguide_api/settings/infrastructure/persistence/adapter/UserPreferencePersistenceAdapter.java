package com.mealguide.mealguide_api.settings.infrastructure.persistence.adapter;

import com.mealguide.mealguide_api.settings.application.port.UserPreferencePort;
import com.mealguide.mealguide_api.settings.domain.UserAllergy;
import com.mealguide.mealguide_api.settings.infrastructure.persistence.repository.UserAllergyJpaRepository;
import com.mealguide.mealguide_api.settings.infrastructure.persistence.repository.UserPreferenceJpaRepository;
import com.mealguide.mealguide_api.settings.domain.UserPreference;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserPreferencePersistenceAdapter implements UserPreferencePort {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final UserPreferenceJpaRepository userPreferenceJpaRepository;
    private final UserAllergyJpaRepository userAllergyJpaRepository;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public Optional<UserPreference> findActiveUserById(Long userId) {
        return userPreferenceJpaRepository.findByIdAndDeletedAtIsNullAndStatus(userId, ACTIVE_STATUS);
    }

    @Override
    public List<String> findAllergyCodesByUserId(Long userId) {
        return userAllergyJpaRepository.findAllergyCodesByUserIdOrderByDisplayOrder(userId);
    }

    @Override
    public void replaceAllergies(Long userId, List<String> allergyCodes) {
        userAllergyJpaRepository.deleteByUserId(userId);
        List<UserAllergy> userAllergies = allergyCodes.stream()
                .map(allergyCode -> UserAllergy.create(userId, allergyCode))
                .toList();
        userAllergyJpaRepository.saveAll(userAllergies);
    }

    @Override
    public List<String> findReligiousCodesByUserId(Long userId) {
        String sql = """
                select religious_food_restriction_code
                from user_religious_food_restriction
                where user_id = :userId
                order by religious_food_restriction_code
                """;
        return namedParameterJdbcTemplate.query(
                sql,
                new MapSqlParameterSource("userId", userId),
                (rs, rowNum) -> rs.getString("religious_food_restriction_code")
        );
    }

    @Override
    public void replaceReligiousCodes(Long userId, List<String> religiousCodes) {
        String deleteSql = """
                delete from user_religious_food_restriction
                where user_id = :userId
                """;
        namedParameterJdbcTemplate.update(deleteSql, new MapSqlParameterSource("userId", userId));
        if (religiousCodes == null || religiousCodes.isEmpty()) {
            return;
        }

        String insertSql = """
                insert into user_religious_food_restriction (user_id, religious_food_restriction_code, created_at)
                values (:userId, :religiousCode, now())
                """;
        SqlParameterSource[] batchParams = religiousCodes.stream()
                .map(religiousCode -> new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("religiousCode", religiousCode))
                .toArray(SqlParameterSource[]::new);
        namedParameterJdbcTemplate.batchUpdate(insertSql, batchParams);
    }
}

