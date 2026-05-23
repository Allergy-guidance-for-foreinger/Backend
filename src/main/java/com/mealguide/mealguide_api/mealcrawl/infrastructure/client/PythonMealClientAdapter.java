package com.mealguide.mealguide_api.mealcrawl.infrastructure.client;

import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.mealcrawl.application.port.PythonMealClientPort;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMealCrawlRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMealCrawlResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuAnalysisRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuAnalysisResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuTranslationRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuTranslationResultDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuTranslationResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuImageAnalysisResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuImageAnalysisResultDto;
import org.springframework.core.io.ByteArrayResource;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Component
public class PythonMealClientAdapter implements PythonMealClientPort {

    private final RestClient restClient;
    private final MealCrawlProperties mealCrawlProperties;

    public PythonMealClientAdapter(RestClient.Builder restClientBuilder, MealCrawlProperties mealCrawlProperties) {
        this.restClient = restClientBuilder.baseUrl(mealCrawlProperties.getPythonBaseUrl()).build();
        this.mealCrawlProperties = mealCrawlProperties;
    }

    @Override
    public PythonMealCrawlResponse crawlMeals(PythonMealCrawlRequest request) {
        try {
            PythonMealCrawlResponse response = restClient.post()
                    .uri(mealCrawlProperties.getCrawlPath())
                    .body(request)
                    .retrieve()
                    .body(PythonMealCrawlResponse.class);

            if (response == null) {
                throw new ServiceException(ErrorCode.UNEXPECTED_SERVER_ERROR);
            }
            return response;
        } catch (RestClientException exception) {
            throw new ServiceException(ErrorCode.UNEXPECTED_SERVER_ERROR, exception);
        }
    }

    @Override
    public PythonMenuAnalysisResponse analyzeMenus(PythonMenuAnalysisRequest request) {
        try {
            PythonMenuAnalysisResponse response = restClient.post()
                    .uri(mealCrawlProperties.getAnalysisPath())
                    .body(request)
                    .retrieve()
                    .body(PythonMenuAnalysisResponse.class);

            if (response == null) {
                throw new ServiceException(ErrorCode.UNEXPECTED_SERVER_ERROR);
            }
            return response;
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            boolean retryable = isRetryableStatus(status);
            throw new PythonMealClientException(
                    "Python menu analysis request failed: status=" + status,
                    status,
                    exception.getResponseBodyAsString(),
                    retryable,
                    exception
            );
        } catch (ResourceAccessException exception) {
            throw new PythonMealClientException(
                    "Python menu analysis request failed: resource access error",
                    null,
                    null,
                    false,
                    exception
            );
        } catch (RestClientException exception) {
            throw new PythonMealClientException(
                    "Python menu analysis request failed",
                    null,
                    null,
                    true,
                    exception
            );
        }
    }

    @Override
    public PythonMenuTranslationResponse translateMenus(PythonMenuTranslationRequest request) {
        try {
            PythonMenuTranslationEnvelope response = restClient.post()
                    .uri(mealCrawlProperties.getTranslationPath())
                    .body(request)
                    .retrieve()
                    .body(PythonMenuTranslationEnvelope.class);

            if (response == null) {
                throw new ServiceException(ErrorCode.UNEXPECTED_SERVER_ERROR);
            }
            List<PythonMenuTranslationResultDto> results = response.results();
            if (results == null && response.data() != null) {
                results = response.data().results();
            }

            if (results == null) {
                log.warn("Python menu translation response has no results field");
                return new PythonMenuTranslationResponse(List.of());
            }
            return new PythonMenuTranslationResponse(results);
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            boolean retryable = isRetryableStatus(status);
            throw new PythonMealClientException(
                    "Python menu translation request failed: status=" + status,
                    status,
                    exception.getResponseBodyAsString(),
                    retryable,
                    exception
            );
        } catch (ResourceAccessException exception) {
            throw new PythonMealClientException(
                    "Python menu translation request failed: resource access error",
                    null,
                    null,
                    false,
                    exception
            );
        } catch (RestClientException exception) {
            throw new PythonMealClientException(
                    "Python menu translation request failed",
                    null,
                    null,
                    true,
                    exception
            );
        }
    }

    @Override
    public PythonMenuImageAnalysisResponse analyzeImage(MultipartFile image) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image", new NamedByteArrayResource(image.getBytes(), image.getOriginalFilename()));

            PythonMenuImageAnalysisEnvelope response = restClient.post()
                    .uri(mealCrawlProperties.getImageAnalysisPath())
                    .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(PythonMenuImageAnalysisEnvelope.class);

            if (response == null) {
                throw new ServiceException(ErrorCode.UNEXPECTED_SERVER_ERROR);
            }

            List<PythonMenuImageAnalysisResultDto> results = response.data() == null ? List.of() : response.data().results();
            return new PythonMenuImageAnalysisResponse(results == null ? List.of() : results);
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            boolean retryable = isRetryableStatus(status);
            throw new PythonMealClientException(
                    "Python image analysis request failed: status=" + status,
                    status,
                    exception.getResponseBodyAsString(),
                    retryable,
                    exception
            );
        } catch (ResourceAccessException exception) {
            throw new PythonMealClientException(
                    "Python image analysis request failed: resource access error",
                    null,
                    null,
                    false,
                    exception
            );
        } catch (Exception exception) {
            throw new PythonMealClientException(
                    "Python image analysis request failed",
                    null,
                    null,
                    true,
                    exception
            );
        }
    }

    private record PythonMenuTranslationEnvelope(
            List<PythonMenuTranslationResultDto> results,
            PythonMenuTranslationEnvelopeData data
    ) {
    }

    private record PythonMenuTranslationEnvelopeData(
            List<PythonMenuTranslationResultDto> results
    ) {
    }

    private record PythonMenuImageAnalysisEnvelope(
            boolean success,
            String code,
            String msg,
            PythonMenuImageAnalysisEnvelopeData data
    ) {
    }

    private record PythonMenuImageAnalysisEnvelopeData(
            List<PythonMenuImageAnalysisResultDto> results
    ) {
    }

    private static final class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename == null ? "image" : filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }

    private boolean isRetryableStatus(int status) {
        return status == 408
                || status == 429
                || status == 500
                || status == 502
                || status == 503
                || status == 504;
    }
}

