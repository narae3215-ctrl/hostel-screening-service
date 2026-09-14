package com.hostelscreening.domain.screening.rules

import com.hostelscreening.domain.screening.ScreeningContext
import com.hostelscreening.domain.screening.ScreeningItemCode
import com.hostelscreening.domain.screening.ScreeningItemResult
import com.hostelscreening.domain.screening.ScreeningRule
import com.hostelscreening.domain.screening.ScreeningStatus

/**
 * 판정 항목 ① 용도지역 — 8개 항목 중 관문(gateway) 역할.
 * hostel-use-change-screening 스킬 1단계 판정 항목 1과 동일한 규칙을 그대로 코드화했다.
 *
 * 상업지역이면 원칙 허용(OK)이지만, 최종 확정은 항상 2단계 토지이음 조회 결과에 달려 있으므로
 * 이 규칙만으로 OverallVerdict를 CONDITIONAL_OK로 올리지 않는다 (ScreeningEngine에서 처리).
 */
class LandUseZoneRule : ScreeningRule {
    override fun evaluate(context: ScreeningContext): ScreeningItemResult {
        val zone = context.building.landUseZone

        val isCommercialZone = zone != null && "상업" in zone

        return if (isCommercialZone) {
            ScreeningItemResult(
                itemCode = ScreeningItemCode.LAND_USE_ZONE,
                status = ScreeningStatus.OK,
                legalBasis = "국토계획법 시행령 별표(용도지역 안에서의 건축제한) — 상업지역은 숙박시설 원칙 허용",
                requiredAction = "2단계 토지이음 조회로 지구단위계획·조례 예외 여부 최종 확인 필요",
                computedValues = mapOf("landUseZone" to (zone ?: "미상")),
            )
        } else {
            ScreeningItemResult(
                itemCode = ScreeningItemCode.LAND_USE_ZONE,
                status = ScreeningStatus.REVIEW,
                legalBasis = "국토계획법 시행령 별표(용도지역 안에서의 건축제한) — 주거지역 등은 숙박시설 원칙 불허",
                requiredAction = "지구단위계획·조례상 예외 여부를 2단계 토지이음 조회로 반드시 확인 (미해결 시 이하 항목 무의미)",
                computedValues = mapOf("landUseZone" to (zone ?: "미상")),
            )
        }
    }
}
