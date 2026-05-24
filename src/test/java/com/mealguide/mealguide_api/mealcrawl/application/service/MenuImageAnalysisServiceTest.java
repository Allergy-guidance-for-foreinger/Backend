package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

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
        when(logRepo.save(any(MenuImageAnalysisLog.class))).thenReturn(log);
        when(logRepo.findById(any())).thenReturn(Optional.of(log));

        MenuImageStoragePort storagePort = mock(MenuImageStoragePort.class);
        PythonMealClientPort pythonPort = mock(PythonMealClientPort.class);

        MealCrawlProperties props = new MealCrawlProperties();
        MenuImageAnalysisService service = new MenuImageAnalysisService(
                preferencePort, logRepo, storagePort, pythonPort,
                mock(MenuJpaRepository.class), mock(MenuAiAnalysisJpaRepository.class),
                mock(MealCrawlPersistencePort.class), props, mock(NamedParameterJdbcTemplate.class),
                new ObjectMapper(), new RiskLevelPolicyResolver(props)
        );

        MockMultipartFile file = new MockMultipartFile("image", "a.jpg", "image/jpeg", new byte[0]);
        assertThatThrownBy(() -> service.analyze(1L, file)).isInstanceOf(ServiceException.class);
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

        MenuImageAnalysisService service = new MenuImageAnalysisService(
                preferencePort, logRepo, storagePort, pythonPort,
                mock(MenuJpaRepository.class), mock(MenuAiAnalysisJpaRepository.class),
                mock(MealCrawlPersistencePort.class), props, mock(NamedParameterJdbcTemplate.class),
                new ObjectMapper(), new RiskLevelPolicyResolver(props)
        );

        MockMultipartFile file = new MockMultipartFile("image", "a.gif", "image/gif", new byte[]{1, 2});
        assertThatThrownBy(() -> service.analyze(1L, file)).isInstanceOf(ServiceException.class);
        verify(storagePort, never()).upload(anyLong(), any());
        verify(pythonPort, never()).analyzeImage(any(), anyString());
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

        MenuImageAnalysisService service = new MenuImageAnalysisService(
                preferencePort, logRepo, storagePort, pythonPort,
                mock(MenuJpaRepository.class), mock(MenuAiAnalysisJpaRepository.class),
                mock(MealCrawlPersistencePort.class), props, mock(NamedParameterJdbcTemplate.class),
                new ObjectMapper(), new RiskLevelPolicyResolver(props)
        );

        MockMultipartFile file = new MockMultipartFile("image", "a.jpg", "image/jpeg", new byte[]{1, 2});
        assertThatThrownBy(() -> service.analyze(1L, file)).isInstanceOf(ServiceException.class);
        verify(pythonPort, never()).analyzeImage(any(), anyString());
    }
}
