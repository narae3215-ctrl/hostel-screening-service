package com.hostelscreening.domain.screening.rules

import com.hostelscreening.domain.screening.ScreeningContext
import com.hostelscreening.domain.screening.ScreeningItemCode
import com.hostelscreening.domain.screening.ScreeningItemResult
import com.hostelscreening.domain.screening.ScreeningRule
import com.hostelscreening.domain.screening.ScreeningStatus
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 판정 항목 ⑦ 부설주차장 — hostel-use-change-screening 스킬 1단계 항목 7.
 *
 * 필요 대수 = 숙박 전환 면적 ÷ 200㎡ (올림 처리). 기존 주차대수와 비교해 여유/부족을 표시한다.
 * 주차 단위 기준(㎡당 대수)은 지자체 조례마다 다르므로 항상 "관할 조례 확인 필요"를 덧붙인다.
 */
class ParkingRule : ScreeningRule {

    private val unitAreaSqm = BigDecimal("200")

    override fun evaluate(context: ScreeningContext): ScreeningItemResult {
        val building = context.building
        val targetArea = context.targetAreaSqm

        val requiredSpaces = if (targetArea > BigDecimal.ZERO) {
            targetArea.divide(unitAreaSqm, 0, RoundingMode.UP).toInt()
        } else {
            0
        }

        val indoor = building.parkingIndoorCount
        val outdoor = building.parkingOutdoorCount

        if (indoor == null && outdoor == null) {
            return ScreeningItemResult(
                itemCode = ScreeningItemCode.PARKING,
                status = ScreeningStatus.REVIEW,
                legalBasis = "주차장법 제19조 및 지자체 부설주차장 설치기준 조례",
                requiredAction = "기존 주차대수 정보가 대장에 없어 확인 불가 — 관할 관청에 기존 주차대수 확인 필요. " +
                    "필요 대수(추정) ${requiredSpaces}대 (전환면적 ${targetArea}㎡ ÷ 200㎡, 올림). 단위 기준은 관할 조례 확인 필요",
                computedValues = mapOf("requiredParkingSpaces" to requiredSpaces, "targetAreaSqm" to targetArea),
            )
        }

        val existing = (indoor ?: 0) + (outdoor ?: 0)
        val status = if (existing >= requiredSpaces) ScreeningStatus.OK else ScreeningStatus.REQUIRED
        val diff = existing - requiredSpaces
        val diffText = if (diff >= 0) "여유 ${diff}대" else "부족 ${-diff}대 — 추가 확보 필요"

        return ScreeningItemResult(
            itemCode = ScreeningItemCode.PARKING,
            status = status,
            legalBasis = "주차장법 제19조 및 지자체 부설주차장 설치기준 조례",
            requiredAction = "필요 대수(추정) ${requiredSpaces}대 (전환면적 ${targetArea}㎡ ÷ 200㎡, 올림), " +
                "기존 ${existing}대 → $diffText. 단위(㎡당 대수) 기준은 관할 조례 확인 필요",
            computedValues = mapOf(
                "requiredParkingSpaces" to requiredSpaces,
                "existingParkingSpaces" to existing,
                "targetAreaSqm" to targetArea,
            ),
        )
    }
}
