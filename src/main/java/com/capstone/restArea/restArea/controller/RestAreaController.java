package com.capstone.restArea.restArea.controller;

import com.capstone.restArea.restArea.dto.RestAreaDetailResponseDto;
import com.capstone.restArea.restArea.dto.RestAreaListResponseDto;
import com.capstone.restArea.restArea.service.RestAreaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rest-areas")
@CrossOrigin(origins = "http://localhost:5173")
public class RestAreaController {

    private final RestAreaService restAreaService;

    @Autowired
    public RestAreaController(RestAreaService restAreaService) {
        this.restAreaService = restAreaService;
    }

    @GetMapping // GET /api/rest-areas
    public ResponseEntity<List<RestAreaListResponseDto>> getAllRestAreas() {
        List<RestAreaListResponseDto> restAreas = restAreaService.findAll();
        return ResponseEntity.ok(restAreas);
    }

    @GetMapping("/{id}") //findById' (상세 조회)도 Entity가 아닌 DTO를 반환
    public ResponseEntity<RestAreaDetailResponseDto> getRestAreaById(@PathVariable Long id) {
        RestAreaDetailResponseDto restArea = restAreaService.findById(id);
        return ResponseEntity.ok(restArea);
    }

    // 3. "휴게소 검색"을 위한 핵심 API 엔드포인트 (이것만 필요!)
    // React에서 fetch(`.../api/rest-areas/search?keyword=안성`)로 호출
    @GetMapping("/search")
    public ResponseEntity<List<RestAreaListResponseDto>> searchRestAreas(
            @RequestParam String keyword // 4. 프론트가 보낸 ?keyword= 값을 받음
    ) {
        List<RestAreaListResponseDto> restAreas = restAreaService.searchByKeyword(keyword);
        return ResponseEntity.ok(restAreas);
    }
}
