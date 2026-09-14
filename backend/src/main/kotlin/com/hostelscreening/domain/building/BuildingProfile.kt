package com.hostelscreening.domain.building

import java.math.BigDecimal
import java.time.LocalDate

/**
 * 건축물대장(표제부 + 을 건축물현황 + 변동사항)을 그대로 반영한 순수 도메인 모델.
 *
 * 실사례(2026.09.10, 부산 중구 남포동5가 58-1)에서 확인된 특성을 그대로 모델링한다:
 *  - siteAreaSqm, sewageFacilityType 등은 대장에 공란(null)인 경우가 흔하다.
 *    → 이 클래스는 프레임워크에 의존하지 않는 순수 모델이며, JPA 엔티티(infra/persistence)와는 별도로 둔다.
 *  - mainUsageText(표제부 주용도)와 floors(층별 실제 용도)가 서로 어긋날 수 있다.
 *    판정 로직은 반드시 floors를 기준으로 삼아야 한다 — mainUsageText는 참고용 원문일 뿐이다.
 */
data class BuildingProfile(
    val buildingRegistryNo: String,          // 고유번호
    val jibunAddress: String,                // 대지위치
    val parcelNumber: String,                // 지번 — 토지이음 조회 기준
    val roadAddress: String?,
    val siteAreaSqm: BigDecimal?,             // 총괄표제부에만 있는 경우 null
    val buildingAreaSqm: BigDecimal?,
    val totalFloorAreaSqm: BigDecimal?,
    val landUseZone: String?,                 // 용도지역 (예: 일반상업)
    val landUseDistrict: String?,             // 지구 (예: 방화지구 외 1)
    val landUseArea: String?,                 // 구역
    val mainStructure: String?,
    val mainUsageText: String?,               // 표제부 주용도 원문 (층별현황과 불일치 가능 — 참고용)
    val floorsBelowGround: Int,
    val floorsAboveGround: Int,
    val hasRooftopFloor: Boolean,
    val approvalDate: LocalDate?,
    val isViolatingBuilding: Boolean,
    val hadPastViolation: Boolean,
    val sewageFacilityType: String?,          // 공란이 흔함
    val sewageFacilityCapacityM3: BigDecimal?,
    val parkingIndoorCount: Int?,
    val parkingOutdoorCount: Int?,
    val floors: List<BuildingFloor>,
    val changeHistory: List<BuildingChangeHistoryItem>,
) {
    val isFireDistrict: Boolean
        get() = landUseDistrict?.contains("방화지구") == true

    fun floorsIn(labels: Set<String>): List<BuildingFloor> =
        floors.filter { it.floorLabel in labels }
}

data class BuildingFloor(
    val buildingGroup: String,   // 구분 (예: 주1)
    val floorLabel: String,      // 층별 (예: 지1, 1층, 2층, 옥탑1층)
    val structureType: String?,
    val usageText: String,       // 실제 용도 — 판정 로직의 기준
    val areaSqm: BigDecimal,
)

data class BuildingChangeHistoryItem(
    val changedAt: LocalDate,
    val description: String,
    val isViolationRelated: Boolean,
)
