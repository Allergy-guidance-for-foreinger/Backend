package com.mealguide.mealguide_api.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "menu_review")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "cafeteria_id", nullable = false)
    private Long cafeteriaId;

    @Column(name = "menu_id", nullable = false)
    private Long menuId;

    @Column(name = "meal_menu_id")
    private Long mealMenuId;

    @Column(name = "meal_date")
    private LocalDate mealDate;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "like_count", nullable = false)
    private Long likeCount;

    @Column(name = "comment_count", nullable = false)
    private Long commentCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static MenuReview create(
            Long userId,
            Long cafeteriaId,
            Long menuId,
            Long mealMenuId,
            LocalDate mealDate,
            String content
    ) {
        MenuReview review = new MenuReview();
        review.userId = userId;
        review.cafeteriaId = cafeteriaId;
        review.menuId = menuId;
        review.mealMenuId = mealMenuId;
        review.mealDate = mealDate;
        review.content = content;
        review.likeCount = 0L;
        review.commentCount = 0L;
        review.createdAt = LocalDateTime.now();
        review.updatedAt = review.createdAt;
        return review;
    }
}
