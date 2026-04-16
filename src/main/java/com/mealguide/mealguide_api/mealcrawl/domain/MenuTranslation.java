package com.mealguide.mealguide_api.mealcrawl.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "menu_translation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "menu_id", nullable = false)
    private Long menuId;

    @Column(name = "lang_code", nullable = false, length = 10)
    private String langCode;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "is_auto_translated", nullable = false)
    private boolean autoTranslated;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static MenuTranslation create(Long menuId, String langCode, String name) {
        MenuTranslation translation = new MenuTranslation();
        translation.menuId = menuId;
        translation.langCode = langCode;
        translation.name = name;
        translation.autoTranslated = true;
        translation.createdAt = LocalDateTime.now();
        translation.updatedAt = translation.createdAt;
        return translation;
    }
}

