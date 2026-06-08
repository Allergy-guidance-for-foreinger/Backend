package com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.adapter;

import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.mealcrawl.application.dto.CurrentUserMealPreference;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealUserPreferencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MealUserPreferenceAdapter implements MealUserPreferencePort {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public CurrentUserMealPreference getCurrentUserMealPreference(Long userId) {
        String sql = """
                select u.id,
                       u.school_id,
                       u.language_code,
                       coalesce((
                           select array_agg(ua.allergy_code order by a.display_order)
                           from user_allergy ua
                           join allergy a on a.code = ua.allergy_code
                           where ua.user_id = u.id
                       ), array[]::varchar[]) as allergy_codes,
                       coalesce((
                           select array_agg(urfr.religious_food_restriction_code order by urfr.religious_food_restriction_code)
                           from user_religious_food_restriction urfr
                           where urfr.user_id = u.id
                       ), array[]::varchar[]) as religious_codes
                from users u
                where u.id = :userId
                  and u.deleted_at is null
                  and u.status = :status
                """;

        List<CurrentUserMealPreference> rows = namedParameterJdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("status", ACTIVE_STATUS),
                (rs, rowNum) -> new CurrentUserMealPreference(
                        rs.getLong("id"),
                        rs.getObject("school_id", Long.class),
                        rs.getString("language_code"),
                        toStringList(rs.getArray("religious_codes")),
                        toStringList(rs.getArray("allergy_codes"))
                )
        );
        return rows.stream()
                .findFirst()
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));
    }

    private List<String> toStringList(Array sqlArray) throws SQLException {
        if (sqlArray == null) {
            return List.of();
        }
        try {
            Object array = sqlArray.getArray();
            if (!(array instanceof Object[] values)) {
                return List.of();
            }
            return Arrays.stream(values)
                    .filter(value -> value != null)
                    .map(Object::toString)
                    .toList();
        } finally {
            sqlArray.free();
        }
    }
}
