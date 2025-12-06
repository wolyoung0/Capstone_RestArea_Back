package com.capstone.restArea.restArea.repository;

import com.capstone.restArea.restArea.entity.RestArea;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestAreaRepository extends JpaRepository<RestArea, Long> {
    /**
     * PostGIS를 사용하여 특정 경로(Polyline) 1km 반경 내의 모든 휴게소를 찾습니다.
     *
     * @param polyline "LINESTRING(경도1 위도1, 경도2 위도2, ...)" 형태의 WKT 문자열
     * @return 경로상 휴게소 목록
     */
    @Query(
            value = "SELECT * FROM rest_areas r " +
                    "WHERE ST_DWithin(" +
                    "    ST_SetSRID(ST_MakePoint(r.longitude, r.latitude), 4326)::geography, " + // 1. 휴게소 위치 (geography로 캐스팅)
                    "    ST_GeomFromText(:polylineWKT, 4326)::geography, " +    // 2. 경로선 (geography로 캐스팅)
                    "    500 " +                                               // 3. 거리 (1000 미터 = 1km)
                    ") " +
//                    "AND r.route_name IN (:routeNames) " +
                    "AND (r.direction = :direction OR r.direction = '양방향')",
            nativeQuery = true
    )
    List<RestArea> findRestAreasOnRoute(@Param("polylineWKT") String polylineWKT, @Param("direction") String direction, @Param("routeNames") List<String> routeNames);

    @EntityGraph(attributePaths = "foodMenus")
    Optional<RestArea> findByName(String name);

    List<RestArea> findByNameContaining(String keyword);
}