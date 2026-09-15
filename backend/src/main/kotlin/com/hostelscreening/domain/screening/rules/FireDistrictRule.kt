package com.hostelscreening.domain.screening.rules

import com.hostelscreening.domain.screening.ScreeningContext
import com.hostelscreening.domain.screening.ScreeningItemCode
import com.hostelscreening.domain.screening.ScreeningItemResult
import com.hostelscreening.domain.screening.ScreeningRule
import com.hostelscreening.domain.screening.ScreeningStatus

/**
 * 판정 항목 ③ 방화지구 — hostel-use-change-screening 스킬 1단계 항목 3.
 *
 * 방화지구 여부는 대장(지구란)으로 확정 가능하지만, 내화구조 충족 여부(외벽 개구부 방화창·
 * 방화설비, 드렌처, 60분 방화문 등)는 도면·현장 확인이 필요한 사안이라 임의로 OK 단정하지 않는다.
 */
class FireDistrictRule : ScreeningRule {
    override fun evaluate(context: ScreeningContext): ScreeningItemResult {
        val building = context.building

        return if (building.isFireDistrict) {
            ScreeningItemResult(
                itemCode = ScreeningItemCode.FIRE_DISTRICT,
                status = ScreeningStatus.REVIEW,
                legalBasis = "건축법 제51조(방화지구 안의 건축물) — 방화지구 내 건축물은 주요구조부·지붕·외벽 등 내화구조 요건 적용",
                requiredAction = "방화지구로 지정된 구역 — 외벽 개구부 방화창·방화설비, 드렌처, 60분 방화문 등 내화구조 충족 여부는 " +
                    "도면·현장 확인 및 관할 건축과·설계사 확인 필요",
                computedValues = mapOf(
                    "isFireDistrict" to true,
                    "landUseDistrict" to (building.landUseDistrict ?: "미상"),
                ),
            )
        } else {
            ScreeningItemResult(
                itemCode = ScreeningItemCode.FIRE_DISTRICT,
                status = ScreeningStatus.OK,
                legalBasis = "건축법 제51조 — 방화지구 지정 없음",
                requiredAction = "특이사항 없음",
                computedValues = mapOf("isFireDistrict" to false),
            )
        }
    }
}
