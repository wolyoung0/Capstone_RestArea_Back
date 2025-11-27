package com.capstone.restArea.restArea.service;

import com.capstone.restArea.restArea.dto.RestAreaDetailResponseDto;
import com.capstone.restArea.restArea.dto.RestAreaListResponseDto;
import com.capstone.restArea.restArea.repository.RestAreaRepository;
import com.capstone.restArea.restArea.entity.RestArea;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RestAreaService {

    private final RestAreaRepository restAreaRepository;

    @Autowired
    public RestAreaService(RestAreaRepository restAreaRepository) {
        this.restAreaRepository = restAreaRepository;
    }

    // 2. (수정) DTO를 반환하는 findAll()만 남깁니다.
    public List<RestAreaListResponseDto> findAll() {
        return restAreaRepository.findAll().stream()
                .map(RestAreaListResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public RestAreaDetailResponseDto findById(Long id) {
        RestArea restArea = restAreaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid rest area Id:" + id));
        // Entity를 DTO로 변환
        return new RestAreaDetailResponseDto(restArea);
    }

    // 3. "휴게소 검색"을 위한 핵심 메서드 (이것만 필요!)
    public List<RestAreaListResponseDto> searchByKeyword(String keyword) {

        // 4. Repository를 통해 DB에서 Entity 리스트를 조회
        List<RestArea> restAreas = restAreaRepository.findByNameContaining(keyword);

        // 5. Entity 리스트를 -> DTO 리스트로 변환
        return restAreas.stream()
                .map(RestAreaListResponseDto::new) // (restArea) -> new RestAreaListResponseDto(restArea) 와 동일
                .collect(Collectors.toList());
    }
}
