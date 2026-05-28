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
@Table(name = "menu_review_comment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuReviewComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static MenuReviewComment create(Long reviewId, Long userId, String content) {
        MenuReviewComment comment = new MenuReviewComment();
        comment.reviewId = reviewId;
        comment.userId = userId;
        comment.content = content;
        comment.createdAt = LocalDateTime.now();
        comment.updatedAt = comment.createdAt;
        return comment;
    }
}
