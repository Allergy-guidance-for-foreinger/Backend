package com.mealguide.mealguide_api.global.config.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.method.HandlerMethod;

@Configuration
@ConditionalOnProperty(name = "swagger.enabled", havingValue = "true")
public class SwaggerConfig {
    @Value("${swagger.server-url}")
    private String serverUrl;

    @Value("${swagger.description}")
    private String serverDescription;

    @Bean
    public OpenAPI openAPI() {
        SecurityRequirement securityRequirement = new SecurityRequirement().addList("Access Token");

        return new OpenAPI()
                .info(apiInfo())
                .addServersItem(new Server()
                        .url(serverUrl)
                        .description(serverDescription))
                .addSecurityItem(securityRequirement)
                .components(securitySchemes())
                ;
    }

    @Bean
    public OperationCustomizer customize(
            SwaggerApiSuccessResponseHandler apiSuccessResponseHandler,
            SwaggerApiFailedResponseHandler apiFailedResponseHandler
    ) {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            apiSuccessResponseHandler.handle(operation, handlerMethod);
            apiFailedResponseHandler.handle(operation, handlerMethod);

            return operation;
        };
    }

    private Info apiInfo() {
        return new Info()
                .title("mealguide for foreigner")
                .description("<p>이 문서는 mealguide for foreigner API의 사용 방법과 예시를 제공합니다.</p>" +
                        "<p>API 사용 중 문제가 발생하거나 문의가 필요한 경우, 담당자에게 문의해 주세요.</p>" +
                        "<p>입력 유효성 오류가 명시되어 있지 않으면 <code>4XX</code> 상태 코드를 반환합니다." +
                        "<code>5XX</code> 상태 코드는 정의되지 않은 서버 오류이므로 담당자에게 문의해 주세요.</p>")
                .version("1.0.0");
    }

    private Components securitySchemes() {
        final SecurityScheme accessTokenSecurityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("Bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        return new Components()
                .addSecuritySchemes("Access Token", accessTokenSecurityScheme);
    }
}
