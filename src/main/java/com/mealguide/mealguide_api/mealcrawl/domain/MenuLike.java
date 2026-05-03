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
@Table(name = "menu_like")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "cafeteria_id", nullable = false)
    private Long cafeteriaId;

    @Column(name = "menu_id", nullable = false)
    private Long menuId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static MenuLike create(Long userId, Long cafeteriaId, Long menuId) {
        MenuLike menuLike = new MenuLike();
        menuLike.userId = userId;
        menuLike.cafeteriaId = cafeteriaId;
        menuLike.menuId = menuId;
        menuLike.createdAt = LocalDateTime.now();
        return menuLike;
    }
}
