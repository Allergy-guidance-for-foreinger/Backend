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

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "menu_review_like")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuReviewLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static MenuReviewLike create(Long reviewId, Long userId) {
        MenuReviewLike like = new MenuReviewLike();
        like.reviewId = reviewId;
        like.userId = userId;
        like.createdAt = LocalDateTime.now();
        return like;
    }
}
