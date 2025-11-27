package com.capstone.restArea.restArea.dto;

import com.capstone.restArea.foodmenu.dto.FoodMenuDto;
import com.capstone.restArea.restArea.entity.RestArea;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
public class RestAreaDetailResponseDto {
    private Long restAreaId;
    private String name;
    private String routeName;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private List<String> amenities;
    private List<FoodMenuDto> foodMenus; // Entity가 아닌 DTO 리스트

    // 엔티티를 DTO로 변환하는 생성자
    public RestAreaDetailResponseDto(RestArea restArea) {
        this.restAreaId = restArea.getRestAreaId();
        this.name = restArea.getName();
        this.routeName = restArea.getRouteName();
        this.address = restArea.getAddress();
        this.latitude = restArea.getLatitude();
        this.longitude = restArea.getLongitude();
        this.amenities = restArea.getFacilities();

        // Entity 리스트(restArea.getFoodMenus())를
        // DTO 리스트(foodMenus)로 변환합니다.
        this.foodMenus = restArea.getFoodMenus().stream()
                .map(FoodMenuDto::new) // .map(foodMenu -> new FoodMenuDto(foodMenu))
                .collect(Collectors.toList());
    }
}
