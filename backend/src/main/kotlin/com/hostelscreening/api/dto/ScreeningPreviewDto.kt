package com.hostelscreening.api.dto

import com.hostelscreening.domain.building.BuildingChangeHistoryItem
import com.hostelscreening.domain.building.BuildingFloor
import com.hostelscreening.domain.building.BuildingProfile
import com.hostelscreening.domain.screening.OverallVerdict
import com.hostelscreening.domain.screening.ScreeningContext
import com.hostelscreening.domain.screening.ScreeningItemResult
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import java.math.BigDecimal
import java.time.LocalDate

/**
 * POST /api/v1/screenings/preview 요청 바디.
 *
 * docs/openapi.yaml의 ScreeningRequestInput은 buildingId(저장된 건물) 기준이지만,
 * WBS Phase 3(공공데이터 연동)이 아직 구현되지 않아 건물을 DB에 저장하는 흐름이 없다.
 * 이 엔드포인트는 그 전 단계에서 판정 엔진(Phase 4)을 실제로 검증·시연할 수 있도록
 * BuildingProfile 전체를 요청 바디로 직접 받는 임시(interim) 계약이다.
 * Phase 3에서 건물 저장/조회가 구현되면 buildingId 기반의 정식 엔드포인트로 대체된다.
 */
data class ScreeningPreviewRequest(
    @field:NotBlank val buildingRegistryNo: String,
    @field:NotBlank val jibunAddress: String,
    @field:NotBlank val parcelNumber: String,
    val roadAddress: String? = null,
    val siteAreaSqm: BigDecimal? = null,
    val buildingAreaSqm: BigDecimal? = null,
    val totalFloorAreaSqm: BigDecimal? = null,
    val landUseZone: String? = null,
    val landUseDistrict: String? = null,
    val landUseArea: String? = null,
    val mainStructure: String? = null,
    val mainUsageText: String? = null,
    val floorsBelowGround: Int = 0,
    val floorsAboveGround: Int = 0,
    val hasRooftopFloor: Boolean = false,
    val approvalDate: LocalDate? = null,
    val isViolatingBuilding: Boolean = false,
    val hadPastViolation: Boolean = false,
    val sewageFacilityType: String? = null,
    val sewageFacilityCapacityM3: BigDecimal? = null,
    val parkingIndoorCount: Int? = null,
    val parkingOutdoorCount: Int? = null,
    val floors: List<BuildingFloorDto> = emptyList(),
    val changeHistory: List<BuildingChangeHistoryItemDto> = emptyList(),
    @field:NotEmpty val targetFloorLabels: Set<String>,
    val hasKitchenFacility: Boolean = true,
) {
    fun toBuildingProfile(): BuildingProfile = BuildingProfile(
        buildingRegistryNo = buildingRegistryNo,
        jibunAddress = jibunAddress,
        parcelNumber = parcelNumber,
        roadAddress = roadAddress,
        siteAreaSqm = siteAreaSqm,
        buildingAreaSqm = buildingAreaSqm,
        totalFloorAreaSqm = totalFloorAreaSqm,
        landUseZone = landUseZone,
        landUseDistrict = landUseDistrict,
        landUseArea = landUseArea,
        mainStructure = mainStructure,
        mainUsageText = mainUsageText,
        floorsBelowGround = floorsBelowGround,
        floorsAboveGround = floorsAboveGround,
        hasRooftopFloor = hasRooftopFloor,
        approvalDate = approvalDate,
        isViolatingBuilding = isViolatingBuilding,
        hadPastViolation = hadPastViolation,
        sewageFacilityType = sewageFacilityType,
        sewageFacilityCapacityM3 = sewageFacilityCapacityM3,
        parkingIndoorCount = parkingIndoorCount,
        parkingOutdoorCount = parkingOutdoorCount,
        floors = floors.map { it.toDomain() },
        changeHistory = changeHistory.map { it.toDomain() },
    )
}

data class BuildingFloorDto(
    val buildingGroup: String,
    val floorLabel: String,
    val structureType: String? = null,
    val usageText: String,
    val areaSqm: BigDecimal,
) {
    fun toDomain() = BuildingFloor(buildingGroup, floorLabel, structureType, usageText, areaSqm)
}

data class BuildingChangeHistoryItemDto(
    val changedAt: LocalDate,
    val description: String,
    val isViolationRelated: Boolean = false,
) {
    fun toDomain() = BuildingChangeHistoryItem(changedAt, description, isViolationRelated)
}

data class ScreeningItemDto(
    val itemCode: String,
    val status: String,
    val legalBasis: String,
    val requiredAction: String,
    val computedValue: Map<String, Any>,
) {
    companion object {
        fun from(result: ScreeningItemResult) = ScreeningItemDto(
            itemCode = result.itemCode.name,
            status = result.status.name,
            legalBasis = result.legalBasis,
            requiredAction = result.requiredAction,
            computedValue = result.computedValues,
        )
    }
}

data class ScreeningPreviewResponse(
    val targetFloorLabels: Set<String>,
    val targetAreaSqm: BigDecimal,
    val overallVerdict: String,
    val disclaimer: String = "⚠️ 본 결과는 1차 스크리닝이며 최종 인허가 판단이 아닙니다. " +
        "관할 건축과·소방서·도시계획과 확인 후 최종 결정하시기 바랍니다.",
    val items: List<ScreeningItemDto>,
) {
    companion object {
        fun of(context: ScreeningContext, items: List<ScreeningItemResult>, verdict: OverallVerdict) =
            ScreeningPreviewResponse(
                targetFloorLabels = context.targetFloorLabels,
                targetAreaSqm = context.targetAreaSqm,
                overallVerdict = verdict.name,
                items = items.map { ScreeningItemDto.from(it) },
            )
    }
}
