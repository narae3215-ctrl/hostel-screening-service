package com.hostelscreening.domain.screening.rules

import com.hostelscreening.domain.screening.ScreeningContext
import com.hostelscreening.domain.screening.ScreeningItemCode
import com.hostelscreening.domain.screening.ScreeningItemResult
import com.hostelscreening.domain.screening.ScreeningRule
import com.hostelscreening.domain.screening.ScreeningStatus
import java.math.BigDecimal

/**
 * 판정 항목 ⑥ 소방시설 — hostel-use-change-screening 스킬 1단계 항목 6
 * (소방시설법 시행령 별표4, 기준일 2025.11.25.).
 *
 * 숙박 바닥면적 합계는 "전환 대상 층"의 면적만 합산한다(건물 전체 연면적과 혼동 금지).
 * 기존 건축물의 용도변경이므로 "6층 이상 전층 설치"(신축 대상 규정)는 적용하지 않고
 * 스프링클러는 면적 기준으로만 판정하되, 6층 이상 건물에는 소방서 사전협의 권장 단서를 덧붙인다.
 */
class FireSafetyRule : ScreeningRule {

    override fun evaluate(context: ScreeningContext): ScreeningItemResult {
        val building = context.building
        val totalFloorArea = building.totalFloorAreaSqm ?: BigDecimal.ZERO
        val targetArea = context.targetAreaSqm

        val findings = mutableListOf<String>()
        var status = ScreeningStatus.OK

        fun require(text: String) {
            findings += "[필요] $text"
            status = ScreeningStatus.REQUIRED
        }
        fun review(text: String) {
            findings += "[검토] $text"
            if (status == ScreeningStatus.OK) status = ScreeningStatus.REVIEW
        }

        // 소화기구: 연면적 33㎡ 이상
        if (totalFloorArea >= BigDecimal("33")) {
            require("소화기구 (연면적 ${totalFloorArea}㎡ ≥ 33㎡)")
        }

        // 옥내소화전: 연면적 3,000㎡ 이상, 또는 숙박연면적 1,500㎡ 이상, 또는 4층 이상 300㎡ 이상 층 존재
        val largeUpperFloorExists = building.floors.any { floor ->
            parseGroundFloorNumber(floor.floorLabel)?.let { it >= 4 } == true && floor.areaSqm >= BigDecimal("300")
        }
        if (totalFloorArea >= BigDecimal("3000") || targetArea >= BigDecimal("1500") || largeUpperFloorExists) {
            require("옥내소화전 (연면적/숙박연면적/4층이상 300㎡ 이상 층 기준 충족)")
            review("지하·무창층 300㎡ 이상 여부는 무창층 판정이 필요해 별도 확인 권장")
        }

        // 스프링클러 / 간이스프링클러: 숙박 바닥면적 합계 기준
        when {
            targetArea >= BigDecimal("600") -> require("스프링클러설비 (숙박 바닥면적 합계 ${targetArea}㎡ ≥ 600㎡)")
            targetArea >= BigDecimal("300") -> require("간이스프링클러설비 (숙박 바닥면적 합계 ${targetArea}㎡, 300~600㎡ 미만)")
            else -> findings += "[해당없음] 스프링클러류 (숙박 바닥면적 합계 ${targetArea}㎡ < 300㎡)"
        }
        if (building.floorsAboveGround >= 6) {
            review("지상 6층 이상 건물 — 면적 기준으로 판정했으나 6층 이상은 소방서 해석에 따라 달라질 수 있어 사전협의 권장")
        }

        // 자동화재탐지설비 등: 숙박시설이면 항상 필요
        require("자동화재탐지설비·시각경보기·유도등·휴대용비상조명등 (숙박시설은 항상 대상)")

        // 그 외 설비: 조건 불명확 시 REVIEW
        review("비상방송설비·연결송수관·비상콘센트·제연설비·상수도소화용수설비·인명구조기구 등은 " +
            "연면적·층수·지하 여부·관광호텔 해당 여부에 따라 달라져 개별 확인 필요")

        return ScreeningItemResult(
            itemCode = ScreeningItemCode.FIRE_SAFETY,
            status = status,
            legalBasis = "소방시설법 시행령 별표4 (기준일 2025.11.25. — 법령 개정 여부 최신 확인 권장)",
            requiredAction = findings.joinToString(" / "),
            computedValues = mapOf(
                "totalFloorAreaSqm" to totalFloorArea,
                "targetAreaSqm" to targetArea,
                "floorsAboveGround" to building.floorsAboveGround,
                "largeUpperFloorExists" to largeUpperFloorExists,
            ),
        )
    }

    /** "4층" → 4, "지1"·"옥탑1층" 등은 null (지하/옥탑은 지상 층수 기준 판정에서 제외) */
    private fun parseGroundFloorNumber(label: String): Int? =
        Regex("^(\\d+)층$").matchEntire(label)?.groupValues?.get(1)?.toIntOrNull()
}
