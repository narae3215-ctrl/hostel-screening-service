package com.hostelscreening.adapter.publicdata

import com.hostelscreening.adapter.geocoding.dto.GeocodedParcel
import com.hostelscreening.adapter.publicdata.dto.BrFlrOulnInfoItem
import com.hostelscreening.adapter.publicdata.dto.BrTitleInfoItem
import com.hostelscreening.domain.building.BuildingFloor
import com.hostelscreening.domain.building.BuildingProfile
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * getBrTitleInfo + getBrFlrOulnInfo 원본 응답을 순수 도메인 BuildingProfile로 변환한다.
 *
 * ⚠️ 필드 신뢰도가 균일하지 않다 — BrTitleInfoItem/BrFlrOulnInfoItem 주석에 적어둔 대로,
 * 주소·면적·구조·주용도·층수·사용승인일은 여러 공개 레퍼런스로 교차 확인된 필드라 신뢰도가
 * 높다. 반면 용도지역/지구/구역, 위반건축물여부, 주차·하수처리 정보는 정확한 필드명을
 * 문서로 확정하지 못했다(정부 API 문서가 동적 렌더링이라 자동으로 읽어올 수 없었음).
 * → 이 값들은 일단 시도해보되, 실제 응답을 1건 받아 BuildingLookupController의
 *   rawResponse 필드와 대조해 잘못된 필드명은 정정해야 한다. 잘못 매핑된 경우 대부분
 *   null/false로 떨어지므로 "REVIEW로 안전하게 수렴"하는 기존 판정 규칙 특성상 즉시
 *   잘못된 승인(OK) 판정으로 이어지지는 않지만, 위반건축물 이력만은 예외이니 특히 주의.
 *
 * hadPastViolation(과거 위반→해제 이력)은 변동사항 이력에서 나오는 값인데, 이 두 오퍼레이션
 * (표제부/층별개요)에는 변동사항이 포함되지 않는다. 별도의 이력 조회 오퍼레이션이 필요하므로
 * 지금은 항상 false로 매핑하고, ViolationRule이 false/false → OK로 확정 판정하지 않도록
 * 화면/리포트에서 "이력 조회 미구현"이라는 문구를 함께 노출하는 것을 권장한다(추후 작업).
 */
object BuildingHubProfileMapper {

    private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")

    fun toBuildingProfile(
        parcel: GeocodedParcel,
        title: BrTitleInfoItem,
        floors: List<BrFlrOulnInfoItem>,
    ): BuildingProfile {
        return BuildingProfile(
            buildingRegistryNo = title.buildingRegistryNo ?: "UNKNOWN-${parcel.sigunguCd}${parcel.bjdongCd}${parcel.bun}${parcel.ji}",
            jibunAddress = title.jibunAddress ?: parcel.jibunAddress,
            parcelNumber = formatParcelNumber(parcel),
            roadAddress = title.roadAddress ?: parcel.roadAddress,
            siteAreaSqm = title.siteAreaSqm?.toBigDecimalOrNull(),
            buildingAreaSqm = title.buildingAreaSqm?.toBigDecimalOrNull(),
            totalFloorAreaSqm = title.totalFloorAreaSqm?.toBigDecimalOrNull(),
            landUseZone = title.landUseZone, // TODO 검증
            landUseDistrict = title.landUseDistrict, // TODO 검증
            landUseArea = title.landUseArea, // TODO 검증
            mainStructure = title.mainStructure,
            mainUsageText = title.mainUsageText,
            floorsBelowGround = title.floorsBelowGround ?: 0,
            floorsAboveGround = title.floorsAboveGround ?: 0,
            hasRooftopFloor = floors.any { it.floorNoName?.contains("옥탑") == true },
            approvalDate = title.approvalDate?.toLocalDateOrNull(),
            isViolatingBuilding = title.violationValue?.let { it == "Y" || it == "1" } ?: false, // TODO 검증
            hadPastViolation = false, // TODO: 변동사항 이력 오퍼레이션 연동 전까지 항상 false — 위 클래스 주석 참고
            sewageFacilityType = null, // TODO: 필드 미확정 — 3.3 범위에서는 미매핑, REVIEW로 안전하게 수렴
            sewageFacilityCapacityM3 = null, // TODO: 위와 동일
            parkingIndoorCount = null, // TODO: 필드 미확정
            parkingOutdoorCount = null, // TODO: 필드 미확정
            floors = floors.mapNotNull { toBuildingFloor(it) },
            changeHistory = emptyList(), // TODO: 변동사항 이력 오퍼레이션 미연동
        )
    }

    private fun toBuildingFloor(item: BrFlrOulnInfoItem): BuildingFloor? {
        val area = item.areaSqm?.toBigDecimalOrNull() ?: return null
        val usage = item.usageText ?: return null
        return BuildingFloor(
            buildingGroup = item.buildingGroup ?: "주1",
            floorLabel = normalizeFloorLabel(item),
            structureType = item.structureType,
            usageText = usage,
            areaSqm = area,
        )
    }

    /**
     * 대장 표기 관행(지1/1층/옥탑1층)에 맞춰 floorDivisionName+floorNoName을 정규화한다.
     * TODO 검증: 실제 flrGbCdNm/flrNoNm 값 조합을 받아본 뒤 이 로직을 다시 맞춰야 한다 —
     * 지금은 "지하" 포함 시 "지{번호}", "옥탑" 포함 시 "옥탑{번호}층", 그 외에는 flrNoNm을
     * 그대로 쓰는 합리적 추정으로 구현했다.
     */
    private fun normalizeFloorLabel(item: BrFlrOulnInfoItem): String {
        val division = item.floorDivisionName.orEmpty()
        val no = item.floorNo?.toString() ?: item.floorNoName.orEmpty().filter { it.isDigit() }
        return when {
            item.floorNoName?.contains("지하") == true || division.contains("지하") -> "지$no"
            item.floorNoName?.contains("옥탑") == true || division.contains("옥탑") -> "옥탑${no}층"
            !item.floorNoName.isNullOrBlank() -> item.floorNoName
            else -> "${no}층"
        }
    }

    private fun formatParcelNumber(parcel: GeocodedParcel): String {
        val bun = parcel.bun.trimStart('0').ifEmpty { "0" }
        val ji = parcel.ji.trimStart('0')
        return if (ji.isEmpty() || ji == "0") bun else "$bun-$ji"
    }

    private fun String.toBigDecimalOrNull(): BigDecimal? =
        this.trim().takeIf { it.isNotEmpty() }?.let { runCatching { BigDecimal(it) }.getOrNull() }

    private fun String.toLocalDateOrNull(): LocalDate? =
        this.trim().takeIf { it.length == 8 }
            ?.let { runCatching { LocalDate.parse(it, DATE_FORMAT) }.getOrNull() }
}
