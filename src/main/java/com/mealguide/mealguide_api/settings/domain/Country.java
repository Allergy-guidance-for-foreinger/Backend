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
@Table(name = "country")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Country {

    @Id
    @Column(length = 10)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;
}
