package com.mealguide.mealguide_api.login.domain;

public enum UserRole {
    USER,
    MANAGER,
    ADMIN;

    public static UserRole defaultRole() {
        return USER;
    }
}
