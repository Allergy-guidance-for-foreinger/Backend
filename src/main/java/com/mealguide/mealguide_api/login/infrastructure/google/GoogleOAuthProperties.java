package com.mealguide.mealguide_api.login.infrastructure.google;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mealguide.google")
public class GoogleOAuthProperties {
    private String clientId;
}
