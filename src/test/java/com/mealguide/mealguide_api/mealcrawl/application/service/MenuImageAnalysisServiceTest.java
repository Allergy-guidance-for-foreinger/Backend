package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.mealcrawl.application.dto.CurrentUserMealPreference;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealUserPreferencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.MenuImageStoragePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.PythonMealClientPort;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuImageAnalysisLog;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository.MenuAiAnalysisJpaRepository;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository.MenuImageAnalysisLogJpaRepository;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository.MenuJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MenuImageAnalysisServiceTest {

    @Test
    void emptyImageFailsBeforeUploadAndPythonCall() {
        MealUserPreferencePort preferencePort = mock(MealUserPreferencePort.class);
        when(preferencePort.getCurrentUserMealPreference(1L))
                .thenReturn(new CurrentUserMealPreference(1L, 1L, "en", List.of(), List.of()));

        MenuImageAnalysisLogJpaRepository logRepo = mock(MenuImageAnalysisLogJpaRepository.class);
        MenuImageAnalysisLog log = MenuImageAnalysisLog.createProcessing(1L);
        when(logRepo.findById(any())).thenReturn(Optional.of(log));

        MenuImageStoragePort storagePort = mock(MenuImageStoragePort.class);
        PythonMealClientPort pythonPort = mock(PythonMealClientPort.class);

        MealCrawlProperties props = new MealCrawlProperties();
        MenuImageAnalysisUsageReservationService reservationService = mock(MenuImageAnalysisUsageReservationService.class);
        MenuImageAnalysisService service = new MenuImageAnalysisService(
                preferencePort, logRepo, reservationService, storagePort, pythonPort,
                mock(MenuJpaRepository.class), mock(MenuAiAnalysisJpaRepository.class),
                mock(MealCrawlPersistencePort.class), props, mock(NamedParameterJdbcTemplate.class),
                new ObjectMapper(), new RiskLevelPolicyResolver(props)
        );

        MockMultipartFile file = new MockMultipartFile("image", "a.jpg", "image/jpeg", new byte[0]);
        assertThatThrownBy(() -> service.analyze(1L, file)).isInstanceOf(ServiceException.class);
        verify(logRepo, never()).countByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(anyLong(), any(), any());
        verify(logRepo, never()).save(any(MenuImageAnalysisLog.class));
        verify(reservationService, never()).reserve(anyLong(), any(), any());
        verify(storagePort, never()).upload(anyLong(), any());
        verify(pythonPort, never()).analyzeImage(any(), anyString());
    }

    @Test
    void invalidContentTypeFailsBeforeUploadAndPythonCall() {
        MealUserPreferencePort preferencePort = mock(MealUserPreferencePort.class);
        when(preferencePort.getCurrentUserMealPreference(1L))
                .thenReturn(new CurrentUserMealPreference(1L, 1L, "en", List.of(), List.of()));

        MenuImageAnalysisLogJpaRepository logRepo = mock(MenuImageAnalysisLogJpaRepository.class);
        MenuImageAnalysisLog log = MenuImageAnalysisLog.createProcessing(1L);
        when(logRepo.save(any(MenuImageAnalysisLog.class))).thenReturn(log);
        when(logRepo.findById(any())).thenReturn(Optional.of(log));

        MenuImageStoragePort storagePort = mock(MenuImageStoragePort.class);
        PythonMealClientPort pythonPort = mock(PythonMealClientPort.class);
        MealCrawlProperties props = new MealCrawlProperties();

        MenuImageAnalysisUsageReservationService reservationService = mock(MenuImageAnalysisUsageReservationService.class);
        MenuImageAnalysisService service = new MenuImageAnalysisService(
                preferencePort, logRepo, reservationService, storagePort, pythonPort,
                mock(MenuJpaRepository.class), mock(MenuAiAnalysisJpaRepository.class),
                mock(MealCrawlPersistencePort.class), props, mock(NamedParameterJdbcTemplate.class),
                new ObjectMapper(), new RiskLevelPolicyResolver(props)
        );

        MockMultipartFile file = new MockMultipartFile("image", "a.gif", "image/gif", new byte[]{1, 2});
        assertThatThrownBy(() -> service.analyze(1L, file)).isInstanceOf(ServiceException.class);
        verify(logRepo, never()).countByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(anyLong(), any(), any());
        verify(logRepo, never()).save(any(MenuImageAnalysisLog.class));
        verify(reservationService, never()).reserve(anyLong(), any(), any());
        verify(storagePort, never()).upload(anyLong(), any());
        verify(pythonPort, never()).analyzeImage(any(), anyString());
    }

    @Test
    void dailyLimitExceededFailsBeforeLogUploadAndPythonCall() {
        MealUserPreferencePort preferencePort = mock(MealUserPreferencePort.class);
        when(preferencePort.getCurrentUserMealPreference(1L))
                .thenReturn(new CurrentUserMealPreference(1L, 1L, "en", List.of(), List.of()));

        MenuImageAnalysisLogJpaRepository logRepo = mock(MenuImageAnalysisLogJpaRepository.class);
        MenuImageAnalysisUsageReservationService reservationService = mock(MenuImageAnalysisUsageReservationService.class);
        when(reservationService.reserve(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenThrow(new ServiceException(ErrorCode.MENU_IMAGE_ANALYSIS_LIMIT_EXCEEDED));

        MenuImageStoragePort storagePort = mock(MenuImageStoragePort.class);
        PythonMealClientPort pythonPort = mock(PythonMealClientPort.class);
        MealCrawlProperties props = new MealCrawlProperties();

        MenuImageAnalysisService service = new MenuImageAnalysisService(
                preferencePort, logRepo, reservationService, storagePort, pythonPort,
                mock(MenuJpaRepository.class), mock(MenuAiAnalysisJpaRepository.class),
                mock(MealCrawlPersistencePort.class), props, mock(NamedParameterJdbcTemplate.class),
                new ObjectMapper(), new RiskLevelPolicyResolver(props)
        );

        MockMultipartFile file = new MockMultipartFile("image", "a.jpg", "image/jpeg", new byte[]{1, 2});
        assertThatThrownBy(() -> service.analyze(1L, file))
                .isInstanceOfSatisfying(ServiceException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MENU_IMAGE_ANALYSIS_LIMIT_EXCEEDED));
        verify(logRepo, never()).save(any(MenuImageAnalysisLog.class));
        verify(storagePort, never()).upload(anyLong(), any());
        verify(pythonPort, never()).analyzeImage(any(), anyString());
    }

    @Test
    void usageReturnsUsedLimitRemainingAndLimitedFlag() {
        MenuImageAnalysisLogJpaRepository logRepo = mock(MenuImageAnalysisLogJpaRepository.class);
        when(logRepo.countByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(1L);

        MealCrawlProperties props = new MealCrawlProperties();
        MenuImageAnalysisService service = new MenuImageAnalysisService(
                mock(MealUserPreferencePort.class), logRepo, mock(MenuImageAnalysisUsageReservationService.class),
                mock(MenuImageStoragePort.class), mock(PythonMealClientPort.class),
                mock(MenuJpaRepository.class), mock(MenuAiAnalysisJpaRepository.class),
                mock(MealCrawlPersistencePort.class), props, mock(NamedParameterJdbcTemplate.class),
                new ObjectMapper(), new RiskLevelPolicyResolver(props)
        );

        var usage = service.getUsage(1L);

        assertThat(usage.usedCount()).isEqualTo(1);
        assertThat(usage.limitCount()).isEqualTo(2);
        assertThat(usage.remainingCount()).isEqualTo(1);
        assertThat(usage.limited()).isFalse();
        assertThat(usage.resetAt().getOffset().getId()).isEqualTo("+09:00");
    }

    @Test
    void uploadFailureStopsBeforePythonCall() {
        MealUserPreferencePort preferencePort = mock(MealUserPreferencePort.class);
        when(preferencePort.getCurrentUserMealPreference(1L))
                .thenReturn(new CurrentUserMealPreference(1L, 1L, "en", List.of(), List.of()));

        MenuImageAnalysisLogJpaRepository logRepo = mock(MenuImageAnalysisLogJpaRepository.class);
        MenuImageAnalysisLog log = MenuImageAnalysisLog.createProcessing(1L);
        when(logRepo.save(any(MenuImageAnalysisLog.class))).thenReturn(log);
        when(logRepo.findById(any())).thenReturn(Optional.of(log));

        MenuImageStoragePort storagePort = mock(MenuImageStoragePort.class);
        when(storagePort.upload(anyLong(), any())).thenThrow(new RuntimeException("upload failed"));
        PythonMealClientPort pythonPort = mock(PythonMealClientPort.class);
        MealCrawlProperties props = new MealCrawlProperties();
        MenuImageAnalysisUsageReservationService reservationService = mock(MenuImageAnalysisUsageReservationService.class);
        when(reservationService.reserve(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(log);

        MenuImageAnalysisService service = new MenuImageAnalysisService(
                preferencePort, logRepo, reservationService, storagePort, pythonPort,
                mock(MenuJpaRepository.class), mock(MenuAiAnalysisJpaRepository.class),
                mock(MealCrawlPersistencePort.class), props, mock(NamedParameterJdbcTemplate.class),
                new ObjectMapper(), new RiskLevelPolicyResolver(props)
        );

        MockMultipartFile file = new MockMultipartFile("image", "a.jpg", "image/jpeg", new byte[]{1, 2});
        assertThatThrownBy(() -> service.analyze(1L, file)).isInstanceOf(ServiceException.class);
        verify(pythonPort, never()).analyzeImage(any(), anyString());
    }

    @Test
    void reservationLocksCountsAndCreatesProcessingLogWhenUnderLimit() {
        MenuImageAnalysisLogJpaRepository logRepo = mock(MenuImageAnalysisLogJpaRepository.class);
        when(logRepo.countByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(1L);
        MenuImageAnalysisLog log = MenuImageAnalysisLog.createProcessing(1L);
        when(logRepo.save(any(MenuImageAnalysisLog.class))).thenReturn(log);

        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        MealCrawlProperties props = new MealCrawlProperties();
        MenuImageAnalysisUsageReservationService service = new MenuImageAnalysisUsageReservationService(logRepo, props, jdbc);

        MenuImageAnalysisLog reserved = service.reserve(1L, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));

        assertThat(reserved).isSameAs(log);
        verify(jdbc).query(startsWith("select pg_advisory_xact_lock"), any(SqlParameterSource.class), any(RowCallbackHandler.class));
        verify(logRepo).save(any(MenuImageAnalysisLog.class));
    }

    @Test
    void reservationDoesNotCreateLogWhenLimitExceeded() {
        MenuImageAnalysisLogJpaRepository logRepo = mock(MenuImageAnalysisLogJpaRepository.class);
        when(logRepo.countByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(2L);

        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        MealCrawlProperties props = new MealCrawlProperties();
        MenuImageAnalysisUsageReservationService service = new MenuImageAnalysisUsageReservationService(logRepo, props, jdbc);

        assertThatThrownBy(() -> service.reserve(1L, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1)))
                .isInstanceOfSatisfying(ServiceException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MENU_IMAGE_ANALYSIS_LIMIT_EXCEEDED));
        verify(jdbc).query(startsWith("select pg_advisory_xact_lock"), any(SqlParameterSource.class), any(RowCallbackHandler.class));
        verify(logRepo, never()).save(any(MenuImageAnalysisLog.class));
    }
}
