package com.capstone.restArea.restArea.dto;

import com.capstone.restArea.restArea.entity.RestArea;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class RestAreaListResponseDto {
    private Long restAreaId;
    private String name;
    private String routeName;
    private String address;
    private String direction;
    private List<String> amenities;

    // Entity를 이 DTO로 변환하는 생성자
    public RestAreaListResponseDto(RestArea restArea) {
        this.restAreaId = restArea.getRestAreaId();
        this.name = restArea.getName();
        this.routeName = restArea.getRouteName();
        this.address = restArea.getAddress();
        this.direction = restArea.getDirection();
        this.amenities = restArea.getFacilities();
    }
}
