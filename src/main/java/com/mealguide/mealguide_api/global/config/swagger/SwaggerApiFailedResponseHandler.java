package com.mealguide.mealguide_api.global.config.swagger;

import com.mealguide.mealguide_api.global.base.dto.FailedResponseBody;
import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.Builder;
import lombok.Getter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "swagger.enabled", havingValue = "true")
public class SwaggerApiFailedResponseHandler {

    public void handle(Operation operation, HandlerMethod handlerMethod) {
        // ✅ 인터페이스에 붙은 @SwaggerApiResponses 까지 찾기
        SwaggerApiResponses apiResponses = SwaggerAnnotationSupport.findSwaggerApiResponses(handlerMethod);
        if (apiResponses == null) return;

        ApiResponses responses = operation.getResponses();

        List<SwaggerApiFailedResponse> apiFailedResponses = Arrays.asList(apiResponses.errors());

        Map<Integer, List<ExampleHolder>> grouped = apiFailedResponses.stream()
                .map(this::createExampleHolder)
                .collect(Collectors.groupingBy(ExampleHolder::getResponseCode));

        addExamplesToResponses(responses, grouped);
    }

    private ExampleHolder createExampleHolder(SwaggerApiFailedResponse apiFailedResponse) {
        ErrorCode exceptionType = apiFailedResponse.value();
        String description = apiFailedResponse.description().isBlank()
                ? exceptionType.getMessage()
                : apiFailedResponse.description();

        return ExampleHolder.builder()
                .responseCode(exceptionType.getStatus().value())
                .exceptionName(exceptionType.name())
                .exceptionCode(exceptionType.getCode())
                .description(description)
                .holder(createSwaggerExample(exceptionType, description))
                .build();
    }

    private Example createSwaggerExample(ErrorCode exceptionType, String description) {
        FailedResponseBody failed = new FailedResponseBody(exceptionType.getCode(), exceptionType.getMessage());

        // 실제 실패 응답 포맷(현재 DTO 기준): { "success":"false", "code":"..", "msg":".." }
        ExampleFailedResponseBody exampleBody = new ExampleFailedResponseBody("false", failed);

        Example example = new Example();
        example.setValue(exampleBody);
        example.setDescription(description);
        return example;
    }

    private void addExamplesToResponses(ApiResponses responses, Map<Integer, List<ExampleHolder>> grouped) {
        grouped.forEach((status, exampleHolders) -> {
            Content content = new Content();
            MediaType mediaType = new MediaType();
            ApiResponse apiResponse = new ApiResponse();

            exampleHolders.forEach(exampleHolder ->
                    mediaType.addExamples(exampleHolder.getExceptionName(), exampleHolder.getHolder())
            );

            content.addMediaType("application/json", mediaType);
            apiResponse.setContent(content);

            responses.addApiResponse(String.valueOf(status), apiResponse);
        });
    }

    @Getter
    @Builder
    public static class ExampleHolder {
        private final int responseCode;
        private final String exceptionName;
        private final String exceptionCode;
        private final String description;
        private final Example holder;
    }

    @Getter
    public static class ExampleFailedResponseBody {
        private final String success;
        private final String code;
        private final String msg;

        public ExampleFailedResponseBody(String success, FailedResponseBody failed) {
            this.success = success;
            this.code = failed.getCode();
            this.msg = failed.getMsg();
        }
    }
}

