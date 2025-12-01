package com.capstone.restArea.restArea.dto;

import com.capstone.restArea.foodmenu.dto.FoodMenuResponseDto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
public class RestAreaResponseDto {
    private Long restAreaId;
    private String name;
    private String routeName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private List<String> facilities;

    // 메뉴 목록을 포함하되, DTO 리스트로 포함합니다.
    private List<FoodMenuResponseDto> foodMenus;
    private String bestMenuName;
    private String recommendationReason;
}
