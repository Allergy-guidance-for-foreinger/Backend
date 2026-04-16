package com.mealguide.mealguide_api.settings.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "language")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Language {

    @Id
    @Column(length = 10)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "english_name", nullable = false, length = 50)
    private String englishName;
}

