package com.hostelscreening.domain.screening.rules

import com.hostelscreening.domain.screening.ScreeningContext
import com.hostelscreening.domain.screening.ScreeningItemCode
import com.hostelscreening.domain.screening.ScreeningItemResult
import com.hostelscreening.domain.screening.ScreeningRule
import com.hostelscreening.domain.screening.ScreeningStatus

/**
 * 판정 항목 ② 위반건축물 — hostel-use-change-screening 스킬 1단계 항목 2.
 *
 * 원칙: 현재 위반건축물로 등재되어 있으면 시정 전까지 사실상 인허가가 막히므로 NG에 가깝게 다룬다.
 * 과거 위반 이력이 있고 현재는 해제된 경우에도 자동으로 OK 단정하지 않고 REVIEW로 둔다 —
 * 해제 처리의 완결성(등재 말소 여부)은 대장 문구만으로 100% 확정하기 어렵기 때문이다.
 */
class ViolationRule : ScreeningRule {
    override fun evaluate(context: ScreeningContext): ScreeningItemResult {
        val building = context.building

        return when {
            building.isViolatingBuilding -> ScreeningItemResult(
                itemCode = ScreeningItemCode.VIOLATION,
                status = ScreeningStatus.NG,
                legalBasis = "건축법 제79조(위반 건축물 등에 대한 조치) — 위반건축물 표기 상태에서는 원칙적으로 용도변경 허가가 제한됨",
                requiredAction = "현재 위반건축물로 등재되어 있음 — 시정(해제) 완료 후 재검토 필요. 관할 건축과에 시정 절차 확인 필수",
                computedValues = mapOf("isViolatingBuilding" to true),
            )
            building.hadPastViolation -> ScreeningItemResult(
                itemCode = ScreeningItemCode.VIOLATION,
                status = ScreeningStatus.REVIEW,
                legalBasis = "건축법 제79조 — 과거 위반 이력이 있었던 건축물",
                requiredAction = "대장상 현재는 위반 표기가 해제된 상태이나, 해제 처리의 완결성은 관할 건축과에서 재확인 필요",
                computedValues = mapOf("isViolatingBuilding" to false, "hadPastViolation" to true),
            )
            else -> ScreeningItemResult(
                itemCode = ScreeningItemCode.VIOLATION,
                status = ScreeningStatus.OK,
                legalBasis = "건축법 제79조 — 위반건축물 이력 없음",
                requiredAction = "특이사항 없음",
                computedValues = mapOf("isViolatingBuilding" to false, "hadPastViolation" to false),
            )
        }
    }
}
