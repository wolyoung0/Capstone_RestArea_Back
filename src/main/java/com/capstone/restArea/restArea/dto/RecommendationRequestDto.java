package com.capstone.restArea.restArea.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class RecommendationRequestDto {
    private RouteDataDto route_data;
    private List<RestAreaInputDto> rest_areas;

    private String user_preference;
}
