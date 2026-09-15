package com.hostelscreening.domain.screening.rules

import com.hostelscreening.domain.screening.ScreeningContext
import com.hostelscreening.domain.screening.ScreeningItemCode
import com.hostelscreening.domain.screening.ScreeningItemResult
import com.hostelscreening.domain.screening.ScreeningRule
import com.hostelscreening.domain.screening.ScreeningStatus
import java.math.BigDecimal

/**
 * 판정 항목 ⑤ 직통계단 — hostel-use-change-screening 스킬 1단계 항목 5 (건축법 시행령 제34조).
 *
 * 전환 대상 층 중 거실 바닥면적 200㎡ 이상인 층이 하나라도 있으면 그 층은 직통계단 2개소가
 * 필요하다(개소 기준 미충족 가능성 → REQUIRED). 모두 200㎡ 미만이면 개소 기준은 충족하지만,
 * 보행거리(내화구조 50m / 비내화구조 30m)와 피난계단 형식(제35조)은 평면도가 있어야 판단
 * 가능한 사안이므로 이 경우도 OK로 단정하지 않고 REVIEW로 남긴다 (스킬의 핵심 원칙).
 */
class DirectStairRule : ScreeningRule {

    private val areaThreshold = BigDecimal("200")

    override fun evaluate(context: ScreeningContext): ScreeningItemResult {
        val targetFloors = context.building.floorsIn(context.targetFloorLabels)
        val largeFloors = targetFloors.filter { it.areaSqm >= areaThreshold }

        return if (largeFloors.isNotEmpty()) {
            ScreeningItemResult(
                itemCode = ScreeningItemCode.DIRECT_STAIR,
                status = ScreeningStatus.REQUIRED,
                legalBasis = "건축법 시행령 제34조(직통계단의 설치) — 거실 바닥면적 200㎡ 이상인 층은 직통계단 2개소 이상 필요",
                requiredAction = "전환 대상 층 중 200㎡ 이상 층 존재(${largeFloors.joinToString { it.floorLabel }}) → 직통계단 2개소 확보 필요. " +
                    "보행거리(내화구조 50m/비내화구조 30m) 및 피난계단 형식(제35조)은 평면도 확인 필요",
                computedValues = mapOf(
                    "largeFloors" to largeFloors.map { it.floorLabel },
                    "requiredStairCount" to 2,
                ),
            )
        } else {
            ScreeningItemResult(
                itemCode = ScreeningItemCode.DIRECT_STAIR,
                status = ScreeningStatus.REVIEW,
                legalBasis = "건축법 시행령 제34조·제35조",
                requiredAction = "전환 대상 층 전부 200㎡ 미만 — 직통계단 개소 기준은 충족. " +
                    "다만 보행거리(내화구조 50m/비내화구조 30m) 및 피난계단 형식은 평면도가 있어야 판단 가능하여 REVIEW로 남김",
                computedValues = mapOf(
                    "largeFloors" to emptyList<String>(),
                    "requiredStairCount" to 1,
                ),
            )
        }
    }
}
