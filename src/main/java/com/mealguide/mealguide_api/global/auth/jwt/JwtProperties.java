package com.mealguide.mealguide_api.global.auth.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
    @Size(min = 32)
    private String accessSecret;
    @NotBlank
    @Size(min = 32)
    private String refreshSecret;
    @Positive
    private long accessTokenExpirationSeconds;
    @Positive
    private long refreshTokenExpirationSeconds;
}

