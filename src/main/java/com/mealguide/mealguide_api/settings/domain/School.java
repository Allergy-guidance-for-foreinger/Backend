package com.mealguide.mealguide_api.settings.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "school")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class School {

    @Id
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "source_url", nullable = false, length = 500)
    private String sourceUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
