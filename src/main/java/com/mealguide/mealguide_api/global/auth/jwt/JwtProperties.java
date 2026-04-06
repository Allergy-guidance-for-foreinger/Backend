package com.mealguide.mealguide_api.global.auth.jwt;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "mealguide.jwt")
public class JwtProperties {
    @NotBlank
    private String accessSecret;
    @NotBlank
    private String refreshSecret;
    @NotBlank
    private long accessTokenExpirationSeconds;
    @NotBlank
    private long refreshTokenExpirationSeconds;
}
