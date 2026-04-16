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
@Table(name = "meal_menu")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MealMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meal_schedule_id", nullable = false)
    private Long mealScheduleId;

    @Column(name = "menu_id", nullable = false)
    private Long menuId;

    @Column(name = "corner_name", length = 100)
    private String cornerName;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "ingredient_info_source_type", nullable = false, length = 20)
    private String ingredientInfoSourceType;

    @Column(name = "ingredient_info_status", nullable = false, length = 20)
    private String ingredientInfoStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static MealMenu create(
            Long mealScheduleId,
            Long menuId,
            String cornerName,
            Integer displayOrder,
            String ingredientInfoSourceType,
            String ingredientInfoStatus
    ) {
        MealMenu mealMenu = new MealMenu();
        mealMenu.mealScheduleId = mealScheduleId;
        mealMenu.menuId = menuId;
        mealMenu.cornerName = cornerName;
        mealMenu.displayOrder = displayOrder;
        mealMenu.ingredientInfoSourceType = ingredientInfoSourceType;
        mealMenu.ingredientInfoStatus = ingredientInfoStatus;
        mealMenu.createdAt = LocalDateTime.now();
        mealMenu.updatedAt = mealMenu.createdAt;
        return mealMenu;
    }

    public void updateMenu(Long menuId, String cornerName, String ingredientInfoSourceType, String ingredientInfoStatus) {
        this.menuId = menuId;
        this.cornerName = cornerName;
        this.ingredientInfoSourceType = ingredientInfoSourceType;
        this.ingredientInfoStatus = ingredientInfoStatus;
        this.updatedAt = LocalDateTime.now();
    }
}

