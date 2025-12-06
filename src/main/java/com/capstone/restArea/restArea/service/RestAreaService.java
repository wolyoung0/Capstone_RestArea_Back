package com.capstone.restArea.restArea.service;

import com.capstone.restArea.foodmenu.dto.FoodMenuInputDto;
import com.capstone.restArea.foodmenu.dto.FoodMenuResponseDto;
import com.capstone.restArea.restArea.dto.*;
import com.capstone.restArea.restArea.entity.RestArea;
import com.capstone.restArea.restArea.repository.RestAreaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RestAreaService {

    private final RestAreaRepository restAreaRepository;

    // [NEW] JSON 파싱용 ObjectMapper
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${kakao.mobility.api.key}")
    private String kakaoApiKey;

    // [NEW] 날씨 API Key (application.properties에 추가 필요)
    @Value("${weather.api.key}")
    private String weatherApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private final String FASTAPI_URL = "http://127.0.0.1:8000/recommend";

    // [NEW] OpenWeatherMap API URL
    private final String WEATHER_API_URL = "https://api.openweathermap.org/data/2.5/weather";

    // --- 1. 길찾기 API (기존 유지) ---
    public String getDirection(String origin, String destination) {
        String url = "https://apis-navi.kakaomobility.com/v1/directions" +
                "?origin=" + origin +
                "&destination=" + destination;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoApiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\":\"API 호출에 실패했습니다.\"}";
        }
    }

    // --- 2. 경로상 휴게소 찾기 (날씨 적용) ---
    public List<RestAreaResponseDto> findRestAreasOnRoute(List<List<Double>> polyline, List<String> routeNames) {
        String polylineWKT = convertToWKT(polyline);
        String direction = determineDirection(polyline);
        System.out.println("탐색된 방향: " + direction);

        List<RestArea> restAreasOnRoute = restAreaRepository.findRestAreasOnRoute(polylineWKT, direction, routeNames);

        // [MODIFIED] 날씨 적용된 요청 생성
        RecommendationRequestDto requestDto = createRecommendationRequest(restAreasOnRoute);

        HttpEntity<RecommendationRequestDto> entity = new HttpEntity<>(requestDto);
        ResponseEntity<List<RecommendationResponseDto>> response = restTemplate.exchange(
                FASTAPI_URL,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<List<RecommendationResponseDto>>() {}
        );

        List<RecommendationResponseDto> scoreList = response.getBody();

        if (scoreList != null) {
            Map<Long, Double> scoreMap = scoreList.stream()
                    .collect(Collectors.toMap(RecommendationResponseDto::getRestAreaId, RecommendationResponseDto::getScore));

            restAreasOnRoute.sort(Comparator.comparingDouble(area ->
                    scoreMap.getOrDefault(((RestArea) area).getRestAreaId(), 0.0)
            ).reversed());
        }

        Map<Long, RecommendationResponseDto> recommendationMap = (scoreList != null) ?
                scoreList.stream().collect(Collectors.toMap(RecommendationResponseDto::getRestAreaId, r -> r)) : Map.of();

        return restAreasOnRoute.stream()
                .map(area -> {
                    RestAreaResponseDto restAreaResponseDto = this.convertToDto(area);
                    if (recommendationMap.containsKey(area.getRestAreaId())) {
                        RecommendationResponseDto recommendationResponseDto = recommendationMap.get(area.getRestAreaId());
                        restAreaResponseDto.setBestMenuName(recommendationResponseDto.getRecommendedMenu());
                        restAreaResponseDto.setRecommendationReason(recommendationResponseDto.getReason());

                        int price = area.getFoodMenus().stream()
                                .filter(menu -> menu.getName().equals(recommendationResponseDto.getRecommendedMenu())) // 이름이 같은 메뉴 찾기
                                .findFirst()
                                .map(menu -> menu.getPrice()) // 가격 가져오기
                                .orElse(0); // 없으면 0원

                        restAreaResponseDto.setBestMenuPrice(price);
                    }
                    return restAreaResponseDto;
                })
                .collect(Collectors.toList());
    }

    // --- 3. 특정 휴게소 상세 추천 (★ 요청하신 대로 포맷 유지 ★) ---
    public String getRecommendationForSingleArea(String restAreaName, String style) {
        RestArea restArea = restAreaRepository.findByName(restAreaName)
                .orElseThrow(() -> new IllegalArgumentException("해당 이름의 휴게소를 찾을 수 없습니다: " + restAreaName));

        // 날씨 포함 요청 생성 (createRecommendationRequest 내부에서 처리됨)
        RecommendationRequestDto requestDto = createRecommendationRequest(List.of(restArea));

        requestDto = RecommendationRequestDto.builder()
                .route_data(requestDto.getRoute_data())
                .rest_areas(requestDto.getRest_areas())
                .user_preference(style)
                .build();

        HttpEntity<RecommendationRequestDto> entity = new HttpEntity<>(requestDto);

        try {
            ResponseEntity<List<RecommendationResponseDto>> response = restTemplate.exchange(
                    FASTAPI_URL,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<List<RecommendationResponseDto>>() {}
            );

            List<RecommendationResponseDto> results = response.getBody();

            // [KEEP] 요청하신 대로 String.format을 사용하여 JSON 문자열 반환
            if (results != null && !results.isEmpty()) {
                RecommendationResponseDto best = results.get(0);

                int price = restArea.getFoodMenus().stream()
                        .filter(menu -> menu.getName().equals(best.getRecommendedMenu()))
                        .findFirst()
                        .map(menu -> menu.getPrice())
                        .orElse(0);

                return String.format(
                        "{\"bestMenu\": \"%s\", \"reason\": \"%s\", \"score\": %.1f, \"price\": %d}",
                        best.getRecommendedMenu(), best.getReason(), best.getScore(), price
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "{}";
    }

    // --- [Helper] 날씨 API 호출 ---
    private String fetchRealTimeWeather(Double lat, Double lon) {
        if (lat == null || lon == null) return "Clear";

        String url = String.format("%s?lat=%f&lon=%f&appid=%s", WEATHER_API_URL, lat, lon, weatherApiKey);

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            if (root.has("weather") && root.get("weather").isArray()) {
                String weatherMain = root.get("weather").get(0).get("main").asText();

                // ★ [로그 1] 실제 받아온 날씨 출력
                System.out.println(">>> [Weather API] 좌표(" + lat + "," + lon + ") 날씨: " + weatherMain);

                return weatherMain;
            }
        } catch (Exception e) {
            System.err.println("날씨 API 호출 실패 (기본값 사용): " + e.getMessage());
            return "Clear";
        }
        return "Clear";
    }

    // --- [Helper] 추천 요청 DTO 생성 (날씨 적용) ---
    private RecommendationRequestDto createRecommendationRequest(List<RestArea> restAreas) {

        List<RestAreaInputDto> restAreaInputs = restAreas.stream().map(area -> {
            RestAreaInputDto dto = RestAreaInputDto.builder()
                    .restAreaId(area.getRestAreaId())
                    .name(area.getName())
                    .routeName(area.getRouteName())
                    .latitude(area.getLatitude())
                    .longitude(area.getLongitude())
                    .facilities(area.getFacilities())
                    .build();

            List<FoodMenuInputDto> menuDtos = area.getFoodMenus().stream()
                    .filter(menu -> menu.getName() != null && !menu.getName().isEmpty())
                    .map(menu -> FoodMenuInputDto.builder()
                            .menuId(menu.getMenuId())
                            .name(menu.getName())
                            .price(menu.getPrice() != null ? menu.getPrice() : 0)
                            .isPremium(menu.getIsPremium())
                            .averageRating(menu.getAverageRating() != null ? menu.getAverageRating() : 0.0)
                            .build())
                    .collect(Collectors.toList());

            dto.setFoodMenus(menuDtos);
            return dto;
        }).collect(Collectors.toList());

        // [MODIFIED] OpenWeatherMap API 호출
        String currentWeather = "Clear";
        if (!restAreas.isEmpty()) {
            RestArea firstArea = restAreas.get(0);

            // [FIX] BigDecimal -> Double 변환 에러 해결 (.doubleValue())
            Double lat = (firstArea.getLatitude() != null) ? firstArea.getLatitude().doubleValue() : null;
            Double lon = (firstArea.getLongitude() != null) ? firstArea.getLongitude().doubleValue() : null;

            currentWeather = fetchRealTimeWeather(lat, lon);
        }

        RouteDataDto routeData = RouteDataDto.builder()
                .traffic_state(2)
                .weather(currentWeather)
                .current_time(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")))
                .build();

        return RecommendationRequestDto.builder()
                .route_data(routeData)
                .rest_areas(restAreaInputs)
                .user_preference("meal")
                .build();
    }

    // ... (기타 헬퍼 메서드는 기존 유지) ...
    private String determineDirection(List<List<Double>> polyline) {
        if (polyline == null || polyline.isEmpty()) return "하행";
        List<Double> start = polyline.get(0);
        List<Double> end = polyline.get(polyline.size() - 1);
        double startLng = Math.toRadians(start.get(0));
        double startLat = Math.toRadians(start.get(1));
        double endLng = Math.toRadians(end.get(0));
        double endLat = Math.toRadians(end.get(1));
        double y = Math.sin(endLng - startLng) * Math.cos(endLat);
        double x = Math.cos(startLat) * Math.sin(endLat) - Math.sin(startLat) * Math.cos(endLat) * Math.cos(endLng - startLng);
        double bearing = Math.toDegrees(Math.atan2(y, x));
        bearing = (bearing + 360) % 360;
        return (bearing >= 45 && bearing < 225) ? "하행" : "상행";
    }

    private RestAreaResponseDto convertToDto(RestArea restArea) {
        List<FoodMenuResponseDto> menuDto = restArea.getFoodMenus().stream()
                .map(menu -> FoodMenuResponseDto.builder()
                        .menuId(menu.getMenuId())
                        .name(menu.getName())
                        .price(menu.getPrice())
                        .isPremium(menu.getIsPremium())
                        .averageRating(menu.getAverageRating())
                        .build())
                .collect(Collectors.toList());

        RestAreaResponseDto restAreaResponseDto = RestAreaResponseDto.builder()
                .restAreaId(restArea.getRestAreaId())
                .name(restArea.getName())
                .routeName(restArea.getRouteName())
                .latitude(restArea.getLatitude())
                .longitude(restArea.getLongitude())
                .facilities(restArea.getFacilities())
                .address(restArea.getAddress())
                .build();
        restAreaResponseDto.setFoodMenus(menuDto);
        return restAreaResponseDto;
    }

    private String convertToWKT(List<List<Double>> polyline) {
        if (polyline == null || polyline.isEmpty()) return "LINESTRING EMPTY";
        return "LINESTRING(" + polyline.stream()
                .map(coords -> coords.get(0) + " " + coords.get(1))
                .collect(Collectors.joining(", ")) + ")";
    }

    public List<RestAreaListResponseDto> findAll() {
        return restAreaRepository.findAll().stream().map(RestAreaListResponseDto::new).collect(Collectors.toList());
    }

    @Transactional
    public RestAreaDetailResponseDto findById(Long id) {
        RestArea restArea = restAreaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid rest area Id:" + id));
        return new RestAreaDetailResponseDto(restArea);
    }

    public List<RestAreaListResponseDto> searchByKeyword(String keyword) {
        return restAreaRepository.findByNameContaining(keyword).stream()
                .map(RestAreaListResponseDto::new)
                .collect(Collectors.toList());
    }
}