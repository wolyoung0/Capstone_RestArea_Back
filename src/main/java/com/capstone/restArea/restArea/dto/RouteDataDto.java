package com.capstone.restArea.restArea.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RouteDataDto {
    private int traffic_state;
    private String current_time;
    private String weather;
}
