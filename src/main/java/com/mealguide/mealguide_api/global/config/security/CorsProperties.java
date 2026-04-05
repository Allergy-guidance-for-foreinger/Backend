package com.mealguide.mealguide_api.global.config.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "mealguide.cors")
public class CorsProperties {
    private List<String> origins = Collections.emptyList();
}
