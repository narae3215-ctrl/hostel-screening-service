package com.hostelscreening.domain.screening.rules

import com.hostelscreening.domain.screening.ScreeningContext
import com.hostelscreening.domain.screening.ScreeningItemCode
import com.hostelscreening.domain.screening.ScreeningItemResult
import com.hostelscreening.domain.screening.ScreeningRule
import com.hostelscreening.domain.screening.ScreeningStatus

/**
 * 판정 항목 ④ 현재 용도 — hostel-use-change-screening 스킬 1단계 항목 4.
 *
 * 현재 용도가 주택(단독·다가구·공동주택 등)이면, 숙박시설로의 용도변경은 건축법상
 * "시설군 변경 허가" 대상이라 신고가 아닌 허가 절차로 무거워질 수 있다.
 *
 * 판정 기준은 항상 "전환 대상 층"의 실제 용도(BuildingFloor.usageText)를 우선한다 —
 * 표제부 주용도(mainUsageText)는 층별 현황과 어긋날 수 있는 참고용 원문일 뿐이라는
 * BuildingProfile의 설계 원칙을 그대로 따른다.
 */
class CurrentUseRule : ScreeningRule {

    companion object {
        private val HOUSING_KEYWORDS = listOf("단독주택", "다가구", "다세대", "공동주택", "아파트", "연립주택", "주택")
    }

    override fun evaluate(context: ScreeningContext): ScreeningItemResult {
        val targetFloors = context.building.floorsIn(context.targetFloorLabels)
        val targetUsageTexts = targetFloors.map { it.usageText }

        val isHousingByTargetFloors = targetUsageTexts.any { usage -> HOUSING_KEYWORDS.any { it in usage } }
        val isHousingByMainUsage = context.building.mainUsageText?.let { main -> HOUSING_KEYWORDS.any { it in main } } ?: false

        return if (isHousingByTargetFloors || isHousingByMainUsage) {
            ScreeningItemResult(
                itemCode = ScreeningItemCode.CURRENT_USE,
                status = ScreeningStatus.REVIEW,
                legalBasis = "건축법 제19조(용도변경) 및 시행령 별표1 시설군 — 주택(단독/다가구/공동주택 등)에서 숙박시설로의 변경은 시설군 변경 허가 대상",
                requiredAction = "현재 용도가 주택 계열 — 신고가 아닌 '허가' 절차 대상이 될 가능성이 높음. 관할 건축과에 시설군 변경 허가 요건 확인 필요",
                computedValues = mapOf(
                    "targetFloorUsageTexts" to targetUsageTexts,
                    "mainUsageText" to (context.building.mainUsageText ?: "미상"),
                ),
            )
        } else {
            ScreeningItemResult(
                itemCode = ScreeningItemCode.CURRENT_USE,
                status = ScreeningStatus.OK,
                legalBasis = "건축법 제19조 및 시행령 별표1 시설군 — 현재 용도가 주택 계열이 아님",
                requiredAction = "특이사항 없음 (다만 실제 시설군 간 이동 여부는 최종 인허가 단계에서 재확인 권장)",
                computedValues = mapOf(
                    "targetFloorUsageTexts" to targetUsageTexts,
                    "mainUsageText" to (context.building.mainUsageText ?: "미상"),
                ),
            )
        }
    }
}
