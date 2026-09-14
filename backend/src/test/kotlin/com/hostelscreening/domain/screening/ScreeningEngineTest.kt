package com.hostelscreening.domain.screening

import com.hostelscreening.domain.building.BuildingFloor
import com.hostelscreening.domain.building.BuildingProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 실제 검토 사례(2026.09.10, 부산 중구 남포동5가 58-1) 데이터를 그대로 사용한 회귀 테스트.
 * hostel-use-change-screening 스킬 대화에서 사람이 직접 검증한 판정 결과와 일치해야 한다:
 *   용도지역=일반상업 → OK, 종합판정은 2단계(토지이음) 확정 전까지 PENDING_LANDUSE.
 */
class ScreeningEngineTest {

    private fun namPodongBuilding() = BuildingProfile(
        buildingRegistryNo = "2611014000-1-00580001",
        jibunAddress = "부산광역시 중구 남포동5가",
        parcelNumber = "58-1",
        roadAddress = "부산광역시 중구 자갈치로59번길 5-20(남포동5가)",
        siteAreaSqm = null, // 대장 표제부에 공란으로 기재된 실사례 그대로 반영
        buildingAreaSqm = BigDecimal("92.13"),
        totalFloorAreaSqm = BigDecimal("373.81"),
        landUseZone = "일반상업",
        landUseDistrict = "방화지구 외 1",
        landUseArea = null,
        mainStructure = "철근콘크리트",
        mainUsageText = "근린생활시설, 숙박시설, 위락시설", // 층별현황과 불일치하는 대장 원문 그대로
        floorsBelowGround = 1,
        floorsAboveGround = 3,
        hasRooftopFloor = true,
        approvalDate = LocalDate.of(1977, 11, 19),
        isViolatingBuilding = false,
        hadPastViolation = true, // 2013.7.17 위반 적발 → 2013.10.14 해제 이력
        sewageFacilityType = null, // 대장에 공란
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

    @Test
    fun `일반상업지역은 용도지역 항목이 OK로 판정된다`() {
        val context = ScreeningContext(
            building = namPodongBuilding(),
            targetFloorLabels = setOf("2층", "3층"),
        )
        val engine = ScreeningEngine()

        val results = engine.evaluate(context)
        val landUseResult = results.first { it.itemCode == ScreeningItemCode.LAND_USE_ZONE }

        assertEquals(ScreeningStatus.OK, landUseResult.status)
    }

    @Test
    fun `전환 대상 면적은 지정한 층의 합계와 일치한다 (2층+3층 = 177_68)`() {
        val context = ScreeningContext(
            building = namPodongBuilding(),
            targetFloorLabels = setOf("2층", "3층"),
        )

        assertEquals(0, BigDecimal("177.68").compareTo(context.targetAreaSqm))
    }

    @Test
    fun `용도지역이 OK여도 종합판정은 2단계 토지이음 확인 전까지 PENDING_LANDUSE다`() {
        val context = ScreeningContext(
            building = namPodongBuilding(),
            targetFloorLabels = setOf("2층", "3층"),
        )
        val engine = ScreeningEngine()

        val results = engine.evaluate(context)
        val verdict = engine.overallVerdict(results)

        assertEquals(OverallVerdict.PENDING_LANDUSE, verdict)
    }
}
