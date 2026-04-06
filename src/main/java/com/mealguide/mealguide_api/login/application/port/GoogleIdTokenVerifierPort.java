package com.mealguide.mealguide_api.login.application.port;

import com.mealguide.mealguide_api.login.domain.google.GoogleUserInfo;

public interface GoogleIdTokenVerifierPort {
    GoogleUserInfo verify(String idToken);
}
