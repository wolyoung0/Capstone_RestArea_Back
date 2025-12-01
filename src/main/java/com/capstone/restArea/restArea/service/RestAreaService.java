package com.capstone.restArea.restArea.service;

import com.capstone.restArea.foodmenu.dto.FoodMenuInputDto;
import com.capstone.restArea.foodmenu.dto.FoodMenuResponseDto;
import com.capstone.restArea.restArea.dto.*;
import com.capstone.restArea.restArea.entity.RestArea;
import com.capstone.restArea.restArea.repository.RestAreaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
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

    @Value("${kakao.mobility.api.key}")
    private String kakaoApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private final String FASTAPI_URL = "http://127.0.0.1:8000/recommend";

    public String getDirection(String origin, String destination) { // 출발지와 목적지는 필수로 요청해야하며 다른 것도 선택가능.

        // 길찾기 Api 요청 주소
        String url = "https://apis-navi.kakaomobility.com/v1/directions" +
                    "?origin=" + origin + // 출발지 (예: "127.110153,37.394727" (경도,위도))
                    "&destination=" + destination; // 목적지 (예: "127.108242,37.401932" (경도, 위도))

        // kakao 맵 ApiKey를 헤더로 전달
        // 조사 필요
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoApiKey);

        // 헤더를 포함한 요청 엔티티 생성
        // 이건 좀 더 조사 필요
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            // RestTemplate을 사용하여 GET 요청 보내고 응답 받기.
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            // 응답받은 JSON 문자열 반환
            return response.getBody();
        } catch (Exception e) {

            // API 호출 실패 시 에러 처리.
            e.printStackTrace();
            return "{\"error\":\"API 호출에 실패했습니다.\"}";
        }
    }

    public List<RestAreaResponseDto> findRestAreasOnRoute(List<List<Double>> polyline, List<String> routeNames) {

        // 1. React에서 받은 좌표 리스트를 PostGIS WKT 문자열로 변환
        String polylineWKT = convertToWKT(polyline);

        // 1.5 출발지->도착지 방위각을 계산하여 '상행/하행' 결정
        String direction = determineDirection(polyline);
        System.out.println("탐색된 방향: " + direction); // 로그 확인용

        // 2. DB에서 경로상 휴게소 목록을 조회합니다 (PostGIS)
        List<RestArea> restAreasOnRoute = restAreaRepository.findRestAreasOnRoute(polylineWKT, direction, routeNames);

        // 3. FastAPI에 보낼 요청 DTO를 생성합니다.
        RecommendationRequestDto requestDto = createRecommendationRequest(restAreasOnRoute);

        // 4. FastAPI 서버에 추천을 요청합니다.
        HttpEntity<RecommendationRequestDto> entity = new HttpEntity<>(requestDto);
        ResponseEntity<List<RecommendationResponseDto>> response = restTemplate.exchange(
                FASTAPI_URL,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<List<RecommendationResponseDto>>() {}
        );

        List<RecommendationResponseDto> scoreList = response.getBody();

        // 5. FastAPI가 반환한 점수(score)로 휴게소 목록을 정렬합니다.
        if (scoreList != null) {
            Map<Long, Double> scoreMap = scoreList.stream()
                    .collect(Collectors.toMap(RecommendationResponseDto::getRestAreaId, RecommendationResponseDto::getScore));

            restAreasOnRoute.sort(Comparator.comparingDouble(area ->
                    scoreMap.getOrDefault(((RestArea) area).getRestAreaId(), 0.0)
            ).reversed());
        }

        // 6. 최종 정렬된 리스트를 반환합니다.
        // RecommendationResponseDto 리스트를 Map으로 변환 (Key: 휴게소ID, Value: 응답객체)
        Map<Long, RecommendationResponseDto> recommendationMap = (scoreList != null) ?
                scoreList.stream().collect(Collectors.toMap(RecommendationResponseDto::getRestAreaId, r -> r)) : Map.of();

        return restAreasOnRoute.stream()
                .map(area -> {
                    RestAreaResponseDto restAreaResponseDto = this.convertToDto(area);

                    // FastAPI가 추천해준 정보(베스트 메뉴, 이유)를 결과 DTO에 주입
                    if (recommendationMap.containsKey(area.getRestAreaId())) {
                        RecommendationResponseDto recommendationResponseDto = recommendationMap.get(area.getRestAreaId());
                        restAreaResponseDto.setBestMenuName(recommendationResponseDto.getRecommendedMenu()); // DTO에 필드 추가 필요
                        restAreaResponseDto.setRecommendationReason(recommendationResponseDto.getReason());  // DTO에 필드 추가 필요
                    }
                    return restAreaResponseDto;
                })
                .collect(Collectors.toList());
    }

    private String determineDirection(List<List<Double>> polyline) {
        if (polyline == null || polyline.isEmpty()) {
            return "하행"; // 기본값
        }

        // polyline 좌표 형식: [longitude(경도, x), latitude(위도, y)]
        // 시작점과 끝점 가져오기
        List<Double> start = polyline.get(0);
        List<Double> end = polyline.get(polyline.size() - 1);

        // 라디안으로 변환 (Math.sin, cos 등은 라디안을 사용함)
        double startLng = Math.toRadians(start.get(0));
        double startLat = Math.toRadians(start.get(1));
        double endLng = Math.toRadians(end.get(0));
        double endLat = Math.toRadians(end.get(1));

        // 방위각(Bearing) 공식
        double y = Math.sin(endLng - startLng) * Math.cos(endLat);
        double x = Math.cos(startLat) * Math.sin(endLat) -
                Math.sin(startLat) * Math.cos(endLat) * Math.cos(endLng - startLng);

        // 아크탄젠트로 각도 구하기 (라디안 -> 도)
        double bearing = Math.toDegrees(Math.atan2(y, x));

        // 0~360도 사이 값으로 정규화 (음수가 나올 경우 처리)
        bearing = (bearing + 360) % 360;

        // 대한민국 도로 기준 판단
        // 45도(북동) ~ 225도(남서) 사이를 바라보면 -> 남쪽/동쪽으로 내려가는 것 -> "하행"
        // 그 외(225도 ~ 45도) -> 북쪽/서쪽으로 올라가는 것 -> "상행"
        if (bearing >= 45 && bearing < 225) {
            return "하행";
        } else {
            return "상행";
        }
    }

    private RestAreaResponseDto convertToDto(RestArea restArea) {

        List<FoodMenuResponseDto> menuDto = restArea.getFoodMenus().stream()
                .map(menu -> FoodMenuResponseDto.builder()
                        .menuId(menu.getMenuId())
                        .name(menu.getName())
                        .price(menu.getPrice())
                        .isPremium(menu.getIsPremium()) // Boolean 타입
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
                .foodMenus(menuDto)
                .build();

        restAreaResponseDto.setFoodMenus(menuDto);
        return restAreaResponseDto;
    }

    private String convertToWKT(List<List<Double>> polyline) {
        if (polyline == null || polyline.isEmpty()) {
            return "LINESTRING EMPTY";
        }

        // "lng lat" 형식의 문자열로 변환 후 콤마(,)로 연결
        String points = polyline.stream()
                .map(coords -> coords.get(0) + " " + coords.get(1)) // [lng, lat] -> "lng lat"
                .collect(Collectors.joining(", ")); // "lng1 lat1, lng2 lat2, ..."

        return "LINESTRING(" + points + ")";
    }

    private RecommendationRequestDto createRecommendationRequest(List<RestArea> restAreas) {

        // 1. 휴게소 엔티티 -> FastAPI 입력용 DTO 변환
        List<RestAreaInputDto> restAreaInputs = restAreas.stream().map(area -> {

            // (1) 휴게소 기본 정보 매핑
            RestAreaInputDto dto = RestAreaInputDto.builder()
                    .restAreaId(area.getRestAreaId())
                    .name(area.getName())
                    .routeName(area.getRouteName())
                    .latitude(area.getLatitude())
                    .longitude(area.getLongitude())
                    .facilities(area.getFacilities())
                    .build();

            // (2) ⭐ 핵심: 음식 메뉴 리스트 매핑 (이게 없으면 추천 로직 작동 불가)
            List<FoodMenuInputDto> menuDtos = area.getFoodMenus().stream()
                    .filter(menu -> menu.getName() != null && !menu.getName().isEmpty())
                    .map(menu -> {
                        FoodMenuInputDto menuDto = FoodMenuInputDto.builder()
                                .menuId(menu.getMenuId())
                                .name(menu.getName())
                                .price(menu.getPrice() != null ? menu.getPrice() : 0)
                                .isPremium(menu.getIsPremium())
                                .averageRating(menu.getAverageRating() != null ? menu.getAverageRating() : 0.0)
                                .build();

                        return menuDto;
                    })
                    .collect(Collectors.toList());

            dto.setFoodMenus(menuDtos); // DTO에 메뉴 리스트 설정

            return dto;
        }).collect(Collectors.toList());

        // 2. 현재 상황(Context) 데이터 설정
        // TODO: 실제로는 외부 API(기상청, 도로공사)에서 받아온 값을 넣어야 합니다.
        RouteDataDto routeData = RouteDataDto.builder()
                .traffic_state(2)
                .weather("Rainy")
                .current_time(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")))
                .build();

        // 3. 최종 요청 객체 생성
        RecommendationRequestDto requestDto = RecommendationRequestDto.builder()
                .route_data(routeData)
                .rest_areas(restAreaInputs)
                .user_preference("meal")
                .build();

        return requestDto;
    }

    public String getRecommendationForSingleArea(String restAreaName, String style) {

        // 1. DB에서 휴게소 정보(메뉴 포함) 조회
        RestArea restArea = restAreaRepository.findByName(restAreaName)
                .orElseThrow(() -> new IllegalArgumentException("해당 이름의 휴게소를 찾을 수 없습니다: " + restAreaName));

        // 2. FastAPI 요청용 DTO 생성 (기존 헬퍼 메소드 활용)
        // 리스트 형태여야 하므로 List.of()로 감쌉니다.
        RecommendationRequestDto requestDto = createRecommendationRequest(List.of(restArea));

        // 3. [중요] 사용자가 선택한 스타일(user_preference) 주입
        // RecommendationRequestDto에 setter나 toBuilder가 없다면 필드를 수정하거나 새로 빌드해야 합니다.
        // 여기서는 @Builder가 있다고 가정하고 새로 빌드하는 방식 예시:
        requestDto = RecommendationRequestDto.builder()
                .route_data(requestDto.getRoute_data())
                .rest_areas(requestDto.getRest_areas())
                .user_preference(style) // ★ React에서 받은 스타일 적용
                .build();

        // 4. FastAPI 호출
        HttpEntity<RecommendationRequestDto> entity = new HttpEntity<>(requestDto);

        try {
            ResponseEntity<List<RecommendationResponseDto>> response = restTemplate.exchange(
                    FASTAPI_URL,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<List<RecommendationResponseDto>>() {}
            );

            List<RecommendationResponseDto> results = response.getBody();

            // 5. 결과 반환 (단일 휴게소이므로 첫 번째 결과만 JSON 문자열로 반환하거나 DTO로 반환)
            // React가 기대하는 포맷에 맞춰 JSON 문자열로 만들어주는 것이 좋습니다.
            if (results != null && !results.isEmpty()) {
                RecommendationResponseDto best = results.get(0);

                // 간단히 JSON 문자열로 리턴 (Jackson ObjectMapper 사용 가능)
                return String.format(
                        "{\"bestMenu\": \"%s\", \"reason\": \"%s\", \"score\": %.1f}",
                        best.getRecommendedMenu(), best.getReason(), best.getScore()
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "{}"; // 실패 시 빈 객체
    }
}
