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
 * ⚠️ 필드 신뢰도가 균일하지 않다 — 2026-09-15 실제 응답(부산 중구 남포동5가 58-1)으로
 * 주소·면적·구조·주용도·층수·사용승인일·위반건축물여부(regstrKindCdNm)까지는 검증 완료했다.
 * 반면 용도지역/지구/구역은 표제부 응답에 아예 없는 필드로 확인됐다 — 별도의 토지이용계획
 * API(LandUsePlanService, WBS 3.5 미구현) 연동 전까지는 항상 null이다. 주차·하수처리 정보도
 * 여전히 필드명 미확정(TODO).
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
            landUseZone = title.landUseZone, // LandUsePlanService(3.5) 연동 전까지 항상 null — 확인됨
            landUseDistrict = title.landUseDistrict, // 위와 동일
            landUseArea = title.landUseArea, // 위와 동일
            mainStructure = title.mainStructure,
            mainUsageText = title.mainUsageText,
            floorsBelowGround = title.floorsBelowGround ?: 0,
            floorsAboveGround = title.floorsAboveGround ?: 0,
            hasRooftopFloor = floors.any { it.floorNoName?.contains("옥탑") == true },
            approvalDate = title.approvalDate?.toLocalDateOrNull(),
            isViolatingBuilding = title.registryKindName?.contains("위반") == true, // 2026-09-15 검증됨
            hadPastViolation = false, // TODO: 변동사항 이력 오퍼레이션 연동 전까지 항상 false — 위 클래스 주석 참고
            sewageFacilityType = null, // TODO: 필드 미확정 — 3.3 범위에서는 미매핑, REVIEW로 안전하게 수렴
            sewageFacilityCapacityM3 = null, // TODO: 위와 동일
            parkingIndoorCount = null, // TODO: 필드 미확정
            parkingOutdoorCount = null, // TODO: 필드 미확정
            floors = dedupeFloors(floors).mapNotNull { toBuildingFloor(it) },
            changeHistory = emptyList(), // TODO: 변동사항 이력 오퍼레이션 미연동
        )
    }

    /**
     * 2026-09-15 확인: getBrFlrOulnInfo는 같은 층에 대해 과거 이력 개정판이 여러 건 쌓여있다
     * (남포동5가 58-1은 실제 층 5개인데 totalCount=12). (floorDivisionName, floorNo) 조합이
     * 같은 레코드 중 crtnDay(데이터 생성일자)가 가장 최신인 것만 남긴다.
     */
    private fun dedupeFloors(floors: List<BrFlrOulnInfoItem>): List<BrFlrOulnInfoItem> =
        floors
            .sortedByDescending { it.createdDate.orEmpty() }
            .distinctBy { "${it.floorDivisionName}-${it.floorNo}" }
            .sortedWith(compareBy({ divisionRank(it.floorDivisionName) }, { it.floorNo }))

    // 지하 → 지상 → 옥탑 순으로 표시하기 위한 정렬 키.
    private fun divisionRank(division: String?): Int = when (division) {
        "지하" -> 0
        "지상" -> 1
        "옥탑" -> 2
        else -> 3
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
