package com.hostelscreening.domain.screening

import com.hostelscreening.domain.building.BuildingFloor
import com.hostelscreening.domain.building.BuildingProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

/**
 * WBS 7.1 — hostel-use-change-screening 스킬 "참고: 판정 로직 검증용 예시 케이스" 절의
 * 6개 시나리오를 구체적인 대장 데이터로 재구성한 회귀 테스트.
 *
 * 스킬 문서의 해당 절은 "유사한 입력이 들어왔을 때 판정 근거를 교차 확인하는 용도로 참고"하라는
 * 목적의 느슨한 서술(예: "직통계단 OK", "REVIEW")이며, 실제 대장 원문이 아니다. 이 테스트는
 * 각 시나리오의 핵심 조건(용도지역/방화지구/층수/면적/하수처리방식 등)만 그대로 가져오고,
 * 상태값은 이미 단위테스트로 검증된 8개 규칙의 공식 판정 기준(ViolationRule 등 rules/ 패키지의
 * 상세 로직)을 그대로 실행한 실제 결과를 기준으로 한다. 서술과 실제 로직이 달라지는 지점은
 * 각 테스트 하단에 근거와 함께 명시했다 — 스킬의 요약 문구보다 공식 판정 기준(면적 임계값,
 * 정화조 처리상한 등)이 우선한다는 설계 원칙(01-architecture.md)에 따른 것이다.
 */
class ScreeningScenarioRegressionTest {

    private fun baseBuilding(
        registryNo: String,
        address: String,
        parcel: String,
        landUseZone: String,
        landUseDistrict: String,
        mainStructure: String,
        mainUsageText: String,
        floorsBelowGround: Int,
        floorsAboveGround: Int,
        isViolatingBuilding: Boolean,
        hadPastViolation: Boolean,
        sewageFacilityType: String?,
        sewageFacilityCapacityM3: BigDecimal?,
        parkingIndoorCount: Int?,
        parkingOutdoorCount: Int?,
        floors: List<BuildingFloor>,
    ) = BuildingProfile(
        buildingRegistryNo = registryNo,
        jibunAddress = address,
        parcelNumber = parcel,
        roadAddress = null,
        siteAreaSqm = null,
        buildingAreaSqm = floors.first().areaSqm,
        totalFloorAreaSqm = floors.fold(BigDecimal.ZERO) { acc, f -> acc + f.areaSqm },
        landUseZone = landUseZone,
        landUseDistrict = landUseDistrict,
        landUseArea = null,
        mainStructure = mainStructure,
        mainUsageText = mainUsageText,
        floorsBelowGround = floorsBelowGround,
        floorsAboveGround = floorsAboveGround,
        hasRooftopFloor = false,
        approvalDate = LocalDate.of(1990, 1, 1),
        isViolatingBuilding = isViolatingBuilding,
        hadPastViolation = hadPastViolation,
        sewageFacilityType = sewageFacilityType,
        sewageFacilityCapacityM3 = sewageFacilityCapacityM3,
        parkingIndoorCount = parkingIndoorCount,
        parkingOutdoorCount = parkingOutdoorCount,
        floors = floors,
        changeHistory = emptyList(),
    )

    private fun statusOf(results: List<ScreeningItemResult>, code: ScreeningItemCode) =
        results.first { it.itemCode == code }.status

    // ── 시나리오 1: 상업지역·방화지구, 7층, 전환 대상 전층 200㎡ 미만, 자체 정화조(소량) ──
    @Test
    fun `시나리오1 - 상업 방화지구 7층 전층200미만 자체하수`() {
        val building = baseBuilding(
            registryNo = "SCN-01", address = "부산광역시 중구 시나리오1가", parcel = "1-1",
            landUseZone = "일반상업", landUseDistrict = "방화지구",
            mainStructure = "철근콘크리트", mainUsageText = "근린생활시설",
            floorsBelowGround = 1, floorsAboveGround = 7,
            isViolatingBuilding = false, hadPastViolation = false,
            sewageFacilityType = "정화조", sewageFacilityCapacityM3 = BigDecimal("1.0"),
            parkingIndoorCount = null, parkingOutdoorCount = null,
            floors = (1..7).map { BuildingFloor("주1", "${it}층", "철근콘크리트", "근린생활시설", BigDecimal("180")) },
        )
        val results = ScreeningEngine().evaluate(ScreeningContext(building, setOf("2층", "3층")))

        assertEquals(ScreeningStatus.OK, statusOf(results, ScreeningItemCode.LAND_USE_ZONE))
        assertEquals(ScreeningStatus.REVIEW, statusOf(results, ScreeningItemCode.FIRE_DISTRICT))
        // 전환 대상 층(2,3층)이 모두 180㎡<200㎡이므로 개소 기준은 충족하나 보행거리 미확인 → REVIEW
        // (스킬 요약문의 "직통계단 OK"는 개소 기준만 가리키는 축약 표현이며, 공식 규칙은 보행거리
        // 미확인 사유로 REVIEW를 유지한다)
        assertEquals(ScreeningStatus.REVIEW, statusOf(results, ScreeningItemCode.DIRECT_STAIR))
        // 전환면적 360㎡(300~600 구간) → 간이스프링클러 REQUIRED, 7층이므로 6층이상 단서도 포함되어
        // FireSafety 전체 상태는 REQUIRED
        assertEquals(ScreeningStatus.REQUIRED, statusOf(results, ScreeningItemCode.FIRE_SAFETY))
        // 오수 산정량(360㎡×0.067→25인×200L=5.0㎥)이 정화조 처리상한(2.0㎥)을 이미 초과하므로,
        // "자체처리 용량이 산정치보다 작다"는 서술보다 강한 REQUIRED(별도 처리시설 필요)로 귀결된다
        assertEquals(ScreeningStatus.REQUIRED, statusOf(results, ScreeningItemCode.SEWAGE))
        assertEquals(OverallVerdict.PENDING_LANDUSE, ScreeningEngine().overallVerdict(results))
    }

    // ── 시나리오 2: 상업지역·방화지구, 지상5층·지하2층, 숙박전환 900㎡대, 공공하수 연결 ──
    @Test
    fun `시나리오2 - 상업 방화지구 5층2지하 숙박900대 공공하수`() {
        val building = baseBuilding(
            registryNo = "SCN-02", address = "부산광역시 중구 시나리오2가", parcel = "2-2",
            landUseZone = "일반상업", landUseDistrict = "방화지구",
            mainStructure = "철근콘크리트", mainUsageText = "근린생활시설",
            floorsBelowGround = 2, floorsAboveGround = 5,
            isViolatingBuilding = false, hadPastViolation = false,
            sewageFacilityType = "공공하수 연결", sewageFacilityCapacityM3 = null,
            parkingIndoorCount = null, parkingOutdoorCount = null,
            floors = listOf(
                BuildingFloor("주1", "1층", "철근콘크리트", "근린생활시설", BigDecimal("270")),
                BuildingFloor("주1", "2층", "철근콘크리트", "근린생활시설", BigDecimal("270")),
                BuildingFloor("주1", "3층", "철근콘크리트", "근린생활시설", BigDecimal("270")),
                BuildingFloor("주1", "4층", "철근콘크리트", "근린생활시설", BigDecimal("370")),
                BuildingFloor("주1", "5층", "철근콘크리트", "근린생활시설", BigDecimal("270")),
                BuildingFloor("주1", "지1", "철근콘크리트", "주차장", BigDecimal("300")),
                BuildingFloor("주1", "지2", "철근콘크리트", "주차장", BigDecimal("300")),
            ),
        )
        val results = ScreeningEngine().evaluate(ScreeningContext(building, setOf("2층", "3층", "4층")))

        // 전환면적 910㎡ ≥600 → 스프링클러(정식) REQUIRED, 4층(370㎡≥300)이 있어 옥내소화전도
        // REQUIRED → FireSafety 전체 REQUIRED
        assertEquals(ScreeningStatus.REQUIRED, statusOf(results, ScreeningItemCode.FIRE_SAFETY))
        // 전환 대상 층이 모두 200㎡ 이상이므로 직통계단 2개소 REQUIRED
        // (스킬 요약의 "숙박면적 900㎡대"만으로는 개소 기준을 명시하지 않았으나, 층별 면적이
        // 200㎡를 넘는 구성이라면 공식 규칙상 REQUIRED가 맞다)
        assertEquals(ScreeningStatus.REQUIRED, statusOf(results, ScreeningItemCode.DIRECT_STAIR))
        assertEquals(ScreeningStatus.REVIEW, statusOf(results, ScreeningItemCode.SEWAGE)) // 공공하수 연결 → 원인자부담금 확인 필요
        assertEquals(ScreeningStatus.REVIEW, statusOf(results, ScreeningItemCode.FIRE_DISTRICT))
    }

    // ── 시나리오 3: 저층 위락시설 + 상부 1개 층만 숙박 전환 ──
    @Test
    fun `시나리오3 - 위락시설 저층부 상부1개층 전환`() {
        val building = baseBuilding(
            registryNo = "SCN-03", address = "부산광역시 중구 시나리오3가", parcel = "3-3",
            landUseZone = "일반상업", landUseDistrict = "방화지구",
            mainStructure = "철근콘크리트", mainUsageText = "위락시설, 근린생활시설",
            floorsBelowGround = 0, floorsAboveGround = 5,
            isViolatingBuilding = false, hadPastViolation = false,
            sewageFacilityType = null, sewageFacilityCapacityM3 = null,
            parkingIndoorCount = 2, parkingOutdoorCount = 0,
            floors = listOf(
                BuildingFloor("주1", "1층", "철근콘크리트", "위락시설", BigDecimal("150")),
                BuildingFloor("주1", "2층", "철근콘크리트", "위락시설", BigDecimal("150")),
                BuildingFloor("주1", "3층", "철근콘크리트", "근린생활시설", BigDecimal("150")),
                BuildingFloor("주1", "4층", "철근콘크리트", "근린생활시설", BigDecimal("150")),
                BuildingFloor("주1", "5층", "철근콘크리트", "근린생활시설", BigDecimal("150")),
            ),
        )
        // 상부 1개 층(5층, 150㎡)만 숙박 전환 — 면적 자체는 소규모라 스프링클러 등은 완화되지만
        // (아래 확인) 위락시설 상부 숙박 구조에 대한 별도 방화구획·피난 REVIEW는 현재 규칙셋에
        // 모델링되어 있지 않다 — 8개 항목 표준 판정 외의 추가 검토 필요 사항으로, 4.x 규칙만으로는
        // 커버되지 않는 한계를 이 테스트로 기록해 둔다.
        val results = ScreeningEngine().evaluate(ScreeningContext(building, setOf("5층")))

        assertEquals(ScreeningStatus.OK, statusOf(results, ScreeningItemCode.PARKING)) // 필요 1대(150/200 올림) ≤ 기존 2대
        assertEquals(ScreeningStatus.REVIEW, statusOf(results, ScreeningItemCode.DIRECT_STAIR)) // 150㎡<200㎡ → 보행거리 미확인
        assertEquals(ScreeningStatus.OK, statusOf(results, ScreeningItemCode.CURRENT_USE)) // 5층은 근린생활시설(주택 아님)
        assertEquals(ScreeningStatus.REQUIRED, statusOf(results, ScreeningItemCode.SEWAGE)) // 정화조 정보 없음+150㎡ 기준 산정량이 상한 초과
    }

    // ── 시나리오 4: 주거지역, 4층 근생, 위반건축물 해제 이력 ──
    @Test
    fun `시나리오4 - 주거지역 위반해제이력`() {
        val building = baseBuilding(
            registryNo = "SCN-04", address = "부산광역시 중구 시나리오4가", parcel = "4-4",
            landUseZone = "제2종일반주거지역", landUseDistrict = "해당없음",
            mainStructure = "철근콘크리트", mainUsageText = "근린생활시설",
            floorsBelowGround = 0, floorsAboveGround = 4,
            isViolatingBuilding = false, hadPastViolation = true,
            sewageFacilityType = null, sewageFacilityCapacityM3 = null,
            parkingIndoorCount = null, parkingOutdoorCount = null,
            floors = (1..4).map { BuildingFloor("주1", "${it}층", "철근콘크리트", "근린생활시설", BigDecimal("120")) },
        )
        val results = ScreeningEngine().evaluate(ScreeningContext(building, setOf("2층", "3층")))

        // 주거지역 → 용도지역 관문 REVIEW ("원칙 불허에 가까움" — 스킬 1단계 항목1)
        assertEquals(ScreeningStatus.REVIEW, statusOf(results, ScreeningItemCode.LAND_USE_ZONE))
        assertEquals(ScreeningStatus.REVIEW, statusOf(results, ScreeningItemCode.VIOLATION)) // 과거 위반 해제 이력 → 재확인 REVIEW
        // 용도지역이 REVIEW라도 현재 엔진은 OK/REVIEW를 동일하게 "2단계 토지이음 확인 전 보류"로
        // 처리한다(ScreeningEngine.overallVerdict) — 스킬 서술의 "사실상 어려움"이라는 정성적 표현을
        // NG 수준으로 격상하지 않고, 2단계로 넘겨 최종 확정한다는 기존 설계(4.10)를 그대로 따른다.
        assertEquals(OverallVerdict.PENDING_LANDUSE, ScreeningEngine().overallVerdict(results))
    }

    // ── 시나리오 5: 주거지역, 비내화구조(연와조) 노후 건축물 ──
    @Test
    fun `시나리오5 - 주거지역 비내화구조 노후건축물`() {
        val building = baseBuilding(
            registryNo = "SCN-05", address = "부산광역시 중구 시나리오5가", parcel = "5-5",
            landUseZone = "제1종일반주거지역", landUseDistrict = "해당없음",
            mainStructure = "연와조", mainUsageText = "단독주택",
            floorsBelowGround = 0, floorsAboveGround = 3,
            isViolatingBuilding = false, hadPastViolation = false,
            sewageFacilityType = null, sewageFacilityCapacityM3 = null,
            parkingIndoorCount = null, parkingOutdoorCount = null,
            floors = (1..3).map { BuildingFloor("주1", "${it}층", "연와조", "단독주택", BigDecimal("90")) },
        )
        val results = ScreeningEngine().evaluate(ScreeningContext(building, setOf("2층", "3층")))

        assertEquals(ScreeningStatus.REVIEW, statusOf(results, ScreeningItemCode.LAND_USE_ZONE))
        // 현재 용도가 단독주택 → 시설군 변경 허가 대상 REVIEW (구조 보강 부담은 직통계단/소방
        // 항목의 REVIEW·REQUIRED로 함께 드러난다 — "비내화구조 보강 부담"을 별도 항목으로
        // 모델링하지는 않음, 8개 표준 항목 범위 밖)
        assertEquals(ScreeningStatus.REVIEW, statusOf(results, ScreeningItemCode.CURRENT_USE))
    }

    // ── 시나리오 6: 상업지역(방화지구 아님), 4층 다가구주택, 옥내주차 여유, 공공하수, 일부 층만 전환 ──
    @Test
    fun `시나리오6 - 상업 방화지구아님 다가구 주차여유 공공하수 부분전환`() {
        val building = baseBuilding(
            registryNo = "SCN-06", address = "부산광역시 중구 시나리오6가", parcel = "6-6",
            landUseZone = "일반상업", landUseDistrict = "해당없음",
            mainStructure = "철근콘크리트", mainUsageText = "다가구주택",
            floorsBelowGround = 0, floorsAboveGround = 4,
            isViolatingBuilding = false, hadPastViolation = false,
            sewageFacilityType = "공공하수 연결", sewageFacilityCapacityM3 = null,
            parkingIndoorCount = 4, parkingOutdoorCount = 0,
            floors = (1..4).map { BuildingFloor("주1", "${it}층", "철근콘크리트", "다가구주택", BigDecimal("140")) },
        )
        // 전환 범위를 좁게(1개 층, 140㎡) 잡은 케이스 — 스킬 서술대로 "전환 범위가 넓어지면 간이
        // 스프링클러 REQUIRED로 전환"되는지는 별도로 넓은 범위(예: 2~4층)를 추가 평가해 대조한다.
        val narrowResults = ScreeningEngine().evaluate(ScreeningContext(building, setOf("2층")))
        assertEquals(ScreeningStatus.OK, statusOf(narrowResults, ScreeningItemCode.LAND_USE_ZONE)) // 상업지역
        assertEquals(ScreeningStatus.OK, statusOf(narrowResults, ScreeningItemCode.FIRE_DISTRICT)) // 방화지구 아님
        assertEquals(ScreeningStatus.REVIEW, statusOf(narrowResults, ScreeningItemCode.CURRENT_USE)) // 다가구주택 → 시설군 변경 REVIEW
        assertEquals(ScreeningStatus.OK, statusOf(narrowResults, ScreeningItemCode.PARKING)) // 필요 1대 ≤ 기존 4대(여유)

        val widerResults = ScreeningEngine().evaluate(ScreeningContext(building, setOf("2층", "3층", "4층")))
        // 전환면적 420㎡(300~600 구간)로 넓어지면 간이스프링클러가 걸려 FireSafety가 REQUIRED로
        // 유지/강화되고, 필요 주차대수도 ceil(420/200)=3대로 늘어 기존 4대 대비 아직 여유(OK)
        assertEquals(ScreeningStatus.REQUIRED, statusOf(widerResults, ScreeningItemCode.FIRE_SAFETY))
        assertEquals(ScreeningStatus.OK, statusOf(widerResults, ScreeningItemCode.PARKING))
    }
}
