package com.capstone.restArea.restArea.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponseDto {
    private Long restAreaId;
    private String restAreaName;
    private String recommendedMenu;
    private Integer menuPrice;
    private Double score;
    private String reason;
}
