package com.capstone.restArea.restArea.controller;

import com.capstone.restArea.restArea.dto.PolylineRequestDto;
import com.capstone.restArea.restArea.dto.RestAreaResponseDto;
import com.capstone.restArea.restArea.entity.RestArea;
import com.capstone.restArea.restArea.service.RestAreaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rest-areas")
@RequiredArgsConstructor
public class RestAreaController {

    private final RestAreaService restAreaService;

    @GetMapping("/route")
    public ResponseEntity<String> getRoute(@RequestParam String origin, @RequestParam String destination) {

        // Service에서 길찾기 APi 호출하고 결과 반환.
        String directionsResult = restAreaService.getDirection(origin, destination);

        return ResponseEntity.ok(directionsResult);
    }

    @PostMapping("/route-polyline")
    public ResponseEntity<List<RestAreaResponseDto>> getRestAreasOnRoute(@RequestBody PolylineRequestDto polylineRequestDto) {

        List<RestAreaResponseDto> restArea = restAreaService.findRestAreasOnRoute(polylineRequestDto.getPolyline(), polylineRequestDto.getRouteNames());
        return ResponseEntity.ok((restArea));
    }
}
