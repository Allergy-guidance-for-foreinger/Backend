package com.mealguide.mealguide_api.login.infrastructure.google;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.login.application.port.GoogleIdTokenVerifierPort;
import com.mealguide.mealguide_api.login.domain.google.GoogleUserInfo;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class GoogleTokenInfoClient implements GoogleIdTokenVerifierPort {

    private final RestClient restClient;
    private final GoogleOAuthProperties googleOAuthProperties;

    public GoogleTokenInfoClient(RestClient.Builder restClientBuilder, GoogleOAuthProperties googleOAuthProperties) {
        this.restClient = restClientBuilder.baseUrl("https://oauth2.googleapis.com").build();
        this.googleOAuthProperties = googleOAuthProperties;
    }

    @Override
    public GoogleUserInfo verify(String idToken) {
        try {
            GoogleTokenInfoResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/tokeninfo").queryParam("id_token", idToken).build())
                    .retrieve()
                    .body(GoogleTokenInfoResponse.class);

            if (response == null || response.audience() == null || !response.audience().equals(googleOAuthProperties.getClientId())) {
                throw new ServiceException(ErrorCode.GOOGLE_ID_TOKEN_INVALID);
            }

            return new GoogleUserInfo(
                    response.subject(),
                    response.email(),
                    response.name(),
                    Boolean.parseBoolean(response.emailVerified())
            );
        } catch (RestClientException exception) {
            throw new ServiceException(ErrorCode.GOOGLE_ID_TOKEN_INVALID);
        }
    }

    private record GoogleTokenInfoResponse(
            @JsonProperty("sub")
            String subject,
            @JsonProperty("aud")
            String audience,
            String email,
            String name,
            @JsonProperty("email_verified")
            String emailVerified
    ) {
    }
}
