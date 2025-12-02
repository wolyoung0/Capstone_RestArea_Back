package com.capstone.restArea.restArea.dto;

import com.capstone.restArea.foodmenu.dto.FoodMenuInputDto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
public class RestAreaInputDto {
    private Long restAreaId;
    private String name;
    private String routeName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private List<String> facilities;
    private List<FoodMenuInputDto> foodMenus; // ✅ 이게 필수!
}
