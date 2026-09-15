package com.hostelscreening.domain.screening.rules

import com.hostelscreening.domain.building.BuildingFloor
import com.hostelscreening.domain.building.BuildingProfile
import com.hostelscreening.domain.screening.ScreeningContext
import com.hostelscreening.domain.screening.ScreeningItemCode
import com.hostelscreening.domain.screening.ScreeningStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Phase 4.3~4.9 판정 모듈 회귀 테스트.
 *
 * 실제 검토 사례(2026.09.10, 부산 중구 남포동5가 58-1: 일반상업·방화지구,
 * 전환 대상 2층+3층=177.68㎡, 현재 용도 단독주택, 위반이력 있음(해제), 주차/하수 정보 공란)와
 * hostel-use-change-screening 스킬 SKILL.md의 예시 케이스들을 기준으로 한다.
 */
class ScreeningRulesTest {

    private fun namPodongBuilding() = BuildingProfile(
        buildingRegistryNo = "2611014000-1-00580001",
        jibunAddress = "부산광역시 중구 남포동5가",
        parcelNumber = "58-1",
        roadAddress = "부산광역시 중구 자갈치로59번길 5-20(남포동5가)",
        siteAreaSqm = null,
        buildingAreaSqm = BigDecimal("92.13"),
        totalFloorAreaSqm = BigDecimal("373.81"),
        landUseZone = "일반상업",
        landUseDistrict = "방화지구 외 1",
        landUseArea = null,
        mainStructure = "철근콘크리트",
        mainUsageText = "근린생활시설, 숙박시설, 위락시설",
        floorsBelowGround = 1,
        floorsAboveGround = 3,
        hasRooftopFloor = true,
        approvalDate = LocalDate.of(1977, 11, 19),
        isViolatingBuilding = false,
        hadPastViolation = true,
        sewageFacilityType = null,
        sewageFacilityCapacityM3 = null,
        parkingIndoorCount = null,
        parkingOutdoorCount = null,
        floors = listOf(
            BuildingFloor("주1", "지1", "철근콘크리트조", "위락시설", BigDecimal("96.56")),
            BuildingFloor("주1", "1층", "철근콘크리트조", "근린생활시설", BigDecimal("92.13")),
            BuildingFloor("주1", "2층", "철근콘크리트스라브", "단독주택", BigDecimal("92.13")),
            BuildingFloor("주1", "3층", "철근콘크리트스라브", "단독주택", BigDecimal("85.55")),
            BuildingFloor("주1", "옥탑1층", "철근콘크리트스라브", "단독주택", BigDecimal("7.44")),
        ),
        changeHistory = emptyList(),
    )

    private fun namPodongContext(hasKitchen: Boolean = true) = ScreeningContext(
        building = namPodongBuilding(),
        targetFloorLabels = setOf("2층", "3층"),
        hasKitchenFacility = hasKitchen,
    )

    @Test
    fun `과거 위반이력이 있고 현재 해제된 경우 REVIEW로 남긴다`() {
        val result = ViolationRule().evaluate(namPodongContext())
        assertEquals(ScreeningStatus.REVIEW, result.status)
    }

    @Test
    fun `현재 위반건축물이면 NG다`() {
        val building = namPodongBuilding().copy(isViolatingBuilding = true)
        val context = ScreeningContext(building, setOf("2층", "3층"))
        val result = ViolationRule().evaluate(context)
        assertEquals(ScreeningStatus.NG, result.status)
    }

    @Test
    fun `위반 이력이 전혀 없으면 OK다`() {
        val building = namPodongBuilding().copy(isViolatingBuilding = false, hadPastViolation = false)
        val context = ScreeningContext(building, setOf("2층", "3층"))
        val result = ViolationRule().evaluate(context)
        assertEquals(ScreeningStatus.OK, result.status)
    }

    @Test
    fun `방화지구 외 1로 표기되면 방화지구 REVIEW다`() {
        val result = FireDistrictRule().evaluate(namPodongContext())
        assertEquals(ScreeningStatus.REVIEW, result.status)
    }

    @Test
    fun `방화지구 지정이 없으면 OK다`() {
        val building = namPodongBuilding().copy(landUseDistrict = "해당없음")
        val context = ScreeningContext(building, setOf("2층", "3층"))
        val result = FireDistrictRule().evaluate(context)
        assertEquals(ScreeningStatus.OK, result.status)
    }

    @Test
    fun `전환 대상 층이 단독주택이면 현재용도는 REVIEW다`() {
        val result = CurrentUseRule().evaluate(namPodongContext())
        assertEquals(ScreeningStatus.REVIEW, result.status)
    }

    @Test
    fun `전환 대상 층이 근린생활시설 등 주택이 아니면 OK다`() {
        val building = namPodongBuilding()
        val context = ScreeningContext(building, setOf("1층")) // 근린생활시설
        val result = CurrentUseRule().evaluate(context)
        assertEquals(ScreeningStatus.OK, result.status)
    }

    @Test
    fun `전환 대상 층이 모두 200제곱미터 미만이면 직통계단은 REVIEW다 (개소는 충족, 보행거리는 미확인)`() {
        val result = DirectStairRule().evaluate(namPodongContext())
        assertEquals(ScreeningStatus.REVIEW, result.status)
    }

    @Test
    fun `전환 대상 층에 200제곱미터 이상이 있으면 직통계단은 REQUIRED다`() {
        val building = namPodongBuilding().copy(
            floors = namPodongBuilding().floors + BuildingFloor("주1", "4층", "철근콘크리트", "숙박시설", BigDecimal("250")),
        )
        val context = ScreeningContext(building, setOf("4층"))
        val result = DirectStairRule().evaluate(context)
        assertEquals(ScreeningStatus.REQUIRED, result.status)
    }

    @Test
    fun `연면적 33제곱미터 이상이면 소방시설 항목은 최소 REQUIRED다 (소화기구+자탐 대상)`() {
        val result = FireSafetyRule().evaluate(namPodongContext())
        assertEquals(ScreeningStatus.REQUIRED, result.status)
    }

    @Test
    fun `숙박 바닥면적 합계 600제곱미터 이상이면 스프링클러 문구가 포함된다`() {
        val building = namPodongBuilding().copy(
            floors = listOf(
                BuildingFloor("주1", "2층", "철근콘크리트", "숙박시설", BigDecimal("350")),
                BuildingFloor("주1", "3층", "철근콘크리트", "숙박시설", BigDecimal("350")),
            ),
        )
        val context = ScreeningContext(building, setOf("2층", "3층"))
        val result = FireSafetyRule().evaluate(context)
        assert(result.requiredAction.contains("스프링클러설비")) { result.requiredAction }
    }

    @Test
    fun `주차대수 정보가 없으면 부설주차장은 REVIEW다`() {
        val result = ParkingRule().evaluate(namPodongContext())
        assertEquals(ScreeningStatus.REVIEW, result.status)
    }

    @Test
    fun `기존 주차대수가 필요 대수 이상이면 OK다`() {
        // 177.68 / 200 -> 올림 1대 필요
        val building = namPodongBuilding().copy(parkingIndoorCount = 1, parkingOutdoorCount = 0)
        val context = ScreeningContext(building, setOf("2층", "3층"))
        val result = ParkingRule().evaluate(context)
        assertEquals(ScreeningStatus.OK, result.status)
    }

    @Test
    fun `기존 주차대수가 필요 대수보다 적으면 REQUIRED다`() {
        val building = namPodongBuilding().copy(parkingIndoorCount = 0, parkingOutdoorCount = 0)
        val context = ScreeningContext(building, setOf("2층", "3층"))
        val result = ParkingRule().evaluate(context)
        assertEquals(ScreeningStatus.REQUIRED, result.status)
    }

    @Test
    fun `정화조 처리상한 2세제곱미터 초과 시 오수는 REQUIRED다`() {
        // 177.68 * 0.067 = 11.9046 -> 올림 12인 -> 12*200L = 2.4㎥ > 2.0㎥ 상한
        val result = SewageRule().evaluate(namPodongContext())
        assertEquals(ScreeningStatus.REQUIRED, result.status)
    }

    @Test
    fun `공공하수 연결이면 오수는 REVIEW다 (원인자부담금 확인 필요)`() {
        val building = namPodongBuilding().copy(sewageFacilityType = "공공하수 연결")
        val context = ScreeningContext(building, setOf("2층", "3층"))
        val result = SewageRule().evaluate(context)
        assertEquals(ScreeningStatus.REVIEW, result.status)
    }

    @Test
    fun `기존 정화조 용량이 산정량 이상이면 오수는 OK다`() {
        val building = namPodongBuilding().copy(sewageFacilityCapacityM3 = BigDecimal("5.0"))
        val context = ScreeningContext(building, setOf("2층", "3층"))
        val result = SewageRule().evaluate(context)
        // daily(2.4) > septicTankCapM3(2.0) 이므로 정화조 상한 초과 분기가 우선 적용되어 REQUIRED임에 유의
        assertEquals(ScreeningStatus.REQUIRED, result.status)
    }

    @Test
    fun `취사시설이 없으면 오수 계수 0_080이 적용된다`() {
        val result = SewageRule().evaluate(namPodongContext(hasKitchen = false))
        assertEquals(BigDecimal("0.080"), result.computedValues["coefficientApplied"])
    }

    @Test
    fun `종합 8개 항목 모두 결과가 생성된다`() {
        val engine = com.hostelscreening.domain.screening.ScreeningEngine()
        val results = engine.evaluate(namPodongContext())
        assertEquals(ScreeningItemCode.entries.size, results.size)
        assertEquals(ScreeningItemCode.entries.toSet(), results.map { it.itemCode }.toSet())
    }
}
