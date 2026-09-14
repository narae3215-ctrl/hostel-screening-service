package com.hostelscreening.domain.screening

import com.hostelscreening.domain.screening.rules.CurrentUseRule
import com.hostelscreening.domain.screening.rules.DirectStairRule
import com.hostelscreening.domain.screening.rules.FireDistrictRule
import com.hostelscreening.domain.screening.rules.FireSafetyRule
import com.hostelscreening.domain.screening.rules.LandUseZoneRule
import com.hostelscreening.domain.screening.rules.ParkingRule
import com.hostelscreening.domain.screening.rules.SewageRule
import com.hostelscreening.domain.screening.rules.ViolationRule

/**
 * 8개 판정 항목을 스킬에 정의된 순서(용도지역이 항상 첫 항목)대로 실행하고 종합판정을 산출한다.
 * 이 클래스는 의도적으로 Spring에 의존하지 않는 순수 도메인 코어다 — Spring 빈 등록은
 * config/ScreeningConfig.kt의 @Bean 메서드에서 담당한다 (도메인 계층은 프레임워크를 모른다는 설계원칙).
 */
class ScreeningEngine(private val rules: List<ScreeningRule> = defaultRules()) {

    fun evaluate(context: ScreeningContext): List<ScreeningItemResult> =
        rules.map { it.evaluate(context) }

    fun overallVerdict(results: List<ScreeningItemResult>): OverallVerdict {
        val landUse = results.firstOrNull { it.itemCode == ScreeningItemCode.LAND_USE_ZONE }
            ?: error("LAND_USE_ZONE 판정 결과가 없습니다 — 용도지역은 관문 항목이라 항상 포함되어야 합니다")

        // 용도지역이 OK라도 2단계 토지이음 조회 전에는 최종 확정하지 않는다 (스킬 2단계 인계 규칙).
        return when (landUse.status) {
            ScreeningStatus.OK -> OverallVerdict.PENDING_LANDUSE
            ScreeningStatus.REVIEW -> OverallVerdict.PENDING_LANDUSE
            ScreeningStatus.NG -> OverallVerdict.DIFFICULT
            ScreeningStatus.REQUIRED -> OverallVerdict.NEEDS_REVIEW
        }
    }

    companion object {
        fun defaultRules(): List<ScreeningRule> = listOf(
            LandUseZoneRule(),
            ViolationRule(),
            FireDistrictRule(),
            CurrentUseRule(),
            DirectStairRule(),
            FireSafetyRule(),
            ParkingRule(),
            SewageRule(),
        )
    }
}
