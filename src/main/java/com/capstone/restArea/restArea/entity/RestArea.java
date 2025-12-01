package com.capstone.restArea.restArea.entity;

import com.capstone.restArea.config.BaseEntity;
import com.capstone.restArea.foodmenu.entity.FoodMenu;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rest_areas")
@Getter
@NoArgsConstructor
public class RestArea extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rest_area_id")
    private Long restAreaId;

    @Column(name = "service_area_code", length = 50, nullable = false, unique = true)
    private String serviceAreaCode;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "route_name", length = 100)
    private String routeName;

    @Column(name = "address", length = 255)
    private String address;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    // ----- 신규 추가된 컬럼 및 관계 -----

    // 편의시설 목록 (예: "주유소", "전기차충전소" 등)
    // 별도의 테이블(rest_area_facilities)에 저장됩니다.
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "rest_area_facilities", joinColumns = @JoinColumn(name = "rest_area_id"))
    @Column(name = "facility_name")
    private List<String> facilities = new ArrayList<>();

    // 이 휴게소가 보유한 메뉴 목록 (1:N 관계)
    @OneToMany(mappedBy = "restArea", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FoodMenu> foodMenus = new ArrayList<>();
}