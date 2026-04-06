package com.mealguide.mealguide_api.global.auth.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mealguide.jwt")
public class JwtProperties {
    private String accessSecret;
    private String refreshSecret;
    private long accessTokenExpirationSeconds;
    private long refreshTokenExpirationSeconds;
}
