package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuImageAnalysisLog;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository.MenuImageAnalysisLogJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuImageAnalysisUsageReservationService {

    private static final long LOCK_NAMESPACE = 0x4D49414C00000000L;
    private static final ZoneId DEFAULT_DAILY_LIMIT_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final MenuImageAnalysisLogJpaRepository logRepository;
    private final MealCrawlProperties properties;
    private final NamedParameterJdbcTemplate jdbc;

    @Transactional
    public MenuImageAnalysisLog reserve(Long userId, LocalDateTime startInclusive, LocalDateTime endExclusive) {
        acquireUserLock(userId);
        long usedCount = logRepository.countByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                userId,
                startInclusive,
                endExclusive
        );
        int limit = dailyAnalysisLimit();
        if (usedCount >= limit) {
            throw new ServiceException(ErrorCode.MENU_IMAGE_ANALYSIS_LIMIT_EXCEEDED);
        }

        return logRepository.save(MenuImageAnalysisLog.createProcessing(
                userId,
                LocalDateTime.now(resolveDailyLimitZoneId())
        ));
    }

    private void acquireUserLock(Long userId) {
        long lockKey = LOCK_NAMESPACE ^ Objects.requireNonNull(userId, "userId must not be null");
        jdbc.query(
                "select pg_advisory_xact_lock(:lockKey)",
                new MapSqlParameterSource("lockKey", lockKey),
                rs -> {
                }
        );
    }

    private int dailyAnalysisLimit() {
        int limit = properties.getMenuImage().getDailyAnalysisLimit();
        return limit > 0 ? limit : 2;
    }

    private ZoneId resolveDailyLimitZoneId() {
        String zoneId = properties.getMenuImage().getDailyAnalysisLimitZoneId();
        if (zoneId == null || zoneId.isBlank()) {
            return DEFAULT_DAILY_LIMIT_ZONE_ID;
        }
        try {
            return ZoneId.of(zoneId.trim());
        } catch (DateTimeException e) {
            log.warn("Invalid menu image daily limit zone id: {}. Fallback to Asia/Seoul.", zoneId);
            return DEFAULT_DAILY_LIMIT_ZONE_ID;
        }
    }
}
