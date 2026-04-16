package com.mealguide.mealguide_api.global.config.swagger;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "swagger.enabled", havingValue = "true")
public class SwaggerApiSuccessResponseHandler {

    public void handle(Operation operation, HandlerMethod handlerMethod) {
        SwaggerApiResponses apiResponses = SwaggerAnnotationSupport.findSwaggerApiResponses(handlerMethod);
        if (apiResponses == null) {
            return;
        }

        SwaggerApiSuccessResponse apiSuccessResponse = apiResponses.success();
        if (apiSuccessResponse == null) {
            return;
        }

        ApiResponses responses = operation.getResponses();

        String responseCode = String.valueOf(apiSuccessResponse.status().value());
        responses.remove("default");
        responses.remove("200");
        responses.remove(responseCode);

        Schema<?> dataSchema = resolveDataSchema(apiSuccessResponse);

        Schema<?> responseSchema = new Schema<>()
                .addProperty("success", new Schema<>().type("string").example("true"))
                .addProperty("data", dataSchema);

        ApiResponse apiResponse = new ApiResponse()
                .description(apiSuccessResponse.description())
                .content(new Content().addMediaType("application/json", new MediaType().schema(responseSchema)));

        responses.addApiResponse(responseCode, apiResponse);
    }

    private Schema<?> resolveDataSchema(SwaggerApiSuccessResponse apiSuccessResponse) {
        if (apiSuccessResponse.responsePage() != Void.class) {
            return buildPageSchema(apiSuccessResponse.responsePage());
        }

        if (apiSuccessResponse.response() != Void.class) {
            Class<?> responseClass = apiSuccessResponse.response();

            if (responseClass.isArray()) {
                Class<?> itemType = responseClass.getComponentType();
                return new ArraySchema().items(schemaOf(itemType));
            }

            return schemaOf(responseClass);
        }

        return new Schema<>().nullable(true).example(null);
    }

    private Schema<?> buildPageSchema(Class<?> responseClass) {
        return new Schema<>()
                .type("object")
                .addProperty("pageNumber", new IntegerSchema().example(0).description("현재 페이지 번호 (0부터 시작)"))
                .addProperty("pageSize", new IntegerSchema().example(12).description("페이지 당 항목 수"))
                .addProperty("totalElements", new IntegerSchema().example(100).description("전체 항목 수"))
                .addProperty("totalPages", new IntegerSchema().example(9).description("전체 페이지 수"))
                .addProperty("pageSort", new StringSchema().example("companyName: ASC").description("정렬 정보"))
                .addProperty(
                        "pageContents",
                        new ArraySchema()
                                .description("페이지 콘텐츠 목록")
                                .items(schemaOf(responseClass).description("요소 스키마는 API에 따라 달라집니다."))
                );
    }

    private Schema<?> schemaOf(Class<?> type) {
        Schema<?> primitive = primitiveSchema(type);
        if (primitive != null) {
            return primitive;
        }

        ResolvedSchema resolved = ModelConverters.getInstance()
                .readAllAsResolvedSchema(new AnnotatedType(type).resolveAsRef(false));

        Schema<?> schema = resolved.schema;
        if (schema == null) {
            return new Schema<>().type("object");
        }
        return schema;
    }

    private Schema<?> primitiveSchema(Class<?> type) {
        if (type == String.class) {
            return new StringSchema();
        }

        if (type == boolean.class || type == Boolean.class) {
            return new BooleanSchema();
        }

        if (type == int.class || type == Integer.class) {
            return new IntegerSchema().format("int32");
        }
        if (type == long.class || type == Long.class) {
            return new IntegerSchema().format("int64");
        }

        if (type == float.class || type == Float.class) {
            return new NumberSchema().format("float");
        }
        if (type == double.class || type == Double.class) {
            return new NumberSchema().format("double");
        }

        return null;
    }
}
