package com.mealguide.mealguide_api.login.domain;

public enum UserRole {
    USER,
    ADMIN,
    MANAGER;

    public static UserRole defaultRole() {
        return USER;
    }
}
