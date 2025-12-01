package com.capstone.restArea.restArea.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PolylineRequestDto {
    private List<List<Double>> polyline;
    private List<String> routeNames;
}
