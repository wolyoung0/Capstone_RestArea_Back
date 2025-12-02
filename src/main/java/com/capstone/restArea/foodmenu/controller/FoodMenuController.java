package com.capstone.restArea.foodmenu.controller;

import com.capstone.restArea.restArea.service.RestAreaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FoodMenuController {

    private final RestAreaService restAreaService;

    @GetMapping("/recommend")
    public ResponseEntity<String> getRecommendation(
            @RequestParam String restAreaName,
            @RequestParam String style
    ) {
        // 서비스에게 "이 휴게소(name)에서 이 스타일(style)로 추천해줘" 라고 위임
        String resultJson = restAreaService.getRecommendationForSingleArea(restAreaName, style);

        return ResponseEntity.ok(resultJson);
    }
}
