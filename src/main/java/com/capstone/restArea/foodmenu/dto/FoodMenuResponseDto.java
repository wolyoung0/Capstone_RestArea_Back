package com.capstone.restArea.foodmenu.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FoodMenuResponseDto {
    private Long menuId;
    private String name;
    private Integer price;
    private Boolean isPremium;
    private Double averageRating;
}
