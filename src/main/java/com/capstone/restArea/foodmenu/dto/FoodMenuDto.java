package com.capstone.restArea.foodmenu.dto;

import com.capstone.restArea.foodmenu.entity.FoodMenu;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FoodMenuDto {
    private Long menuId;
    private String name;
    private Integer price;
    private String description;
    private String imageUrl;
    private Boolean isPremium; // "프리미엄" 태그
    private Double averageRating; // "4.2" 같은 평점

    // 엔티티를 DTO로 변환하는 생성자
    // (FoodMenu 엔티티의 @Getter가 이 메서드들을 자동으로 만들어줍니다)
    public FoodMenuDto(FoodMenu foodMenu) {
        this.menuId = foodMenu.getMenuId();
        this.name = foodMenu.getName();
        this.price = foodMenu.getPrice();
        this.description = foodMenu.getDescription();
        this.imageUrl = foodMenu.getImageUrl();
        this.isPremium = foodMenu.getIsPremium(); // 'isPremium' 필드의 Getter
        this.averageRating = foodMenu.getAverageRating();
    }
}
