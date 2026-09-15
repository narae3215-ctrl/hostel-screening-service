package com.hostelscreening.domain.screening.rules

import com.hostelscreening.domain.screening.ScreeningContext
import com.hostelscreening.domain.screening.ScreeningItemCode
import com.hostelscreening.domain.screening.ScreeningItemResult
import com.hostelscreening.domain.screening.ScreeningRule
import com.hostelscreening.domain.screening.ScreeningStatus
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 판정 항목 ⑧ 오수/하수처리 — hostel-use-change-screening 스킬 1단계 항목 8.
 *
 * 환경부고시 「건축물의 용도별 오수발생량 및 정화조 처리대상인원 산정기준」의 면적 계수를 적용해
 * 오수 인원(N)을 산정한다. 호스텔은 통상 취사시설이 있으므로 기본 계수는 0.067(취사 있음)이며,
 * hasKitchenFacility=false면 0.080(취사 없음)을 적용한다. 1일 오수량 = N × 200L.
 * 정화조 처리 상한은 2㎥/일(약 10인)이며 이를 초과하면 별도 오수처리시설이 필요하다.
 * 적용한 계수와 산출값은 모두 추정치임을 항상 명시한다.
 */
class SewageRule : ScreeningRule {

    private val litersPerPerson = BigDecimal("200")
    private val septicTankCapM3 = BigDecimal("2.0") // 정화조 처리 상한 (약 10인)

    override fun evaluate(context: ScreeningContext): ScreeningItemResult {
        val building = context.building
        val targetArea = context.targetAreaSqm
        val coefficient = if (context.hasKitchenFacility) BigDecimal("0.067") else BigDecimal("0.080")

        val personsEstimated = coefficient.multiply(targetArea).setScale(0, RoundingMode.UP)
        val dailyAmountM3 = personsEstimated.multiply(litersPerPerson)
            .divide(BigDecimal("1000"), 3, RoundingMode.UP) // L -> ㎥

        val estimateNote = "(계수 ${coefficient} 적용, 추정치)"
        val isPublicConnection = building.sewageFacilityType?.let { "공공" in it || "하수종말" in it } == true

        if (isPublicConnection) {
            return ScreeningItemResult(
                itemCode = ScreeningItemCode.SEWAGE,
                status = ScreeningStatus.REVIEW,
                legalBasis = "환경부고시 「건축물의 용도별 오수발생량 및 정화조 처리대상인원 산정기준」, 하수도법",
                requiredAction = "공공하수(하수종말처리장) 연결로 정화조 증설 부담은 없음. 다만 원인자부담금·사용료 발생 여부는 " +
                    "관할 관청 확인 필요. 산정 오수인원 ${personsEstimated}인, 1일 오수량 ${dailyAmountM3}㎥ $estimateNote",
                computedValues = mapOf(
                    "sewagePersonsEstimated" to personsEstimated,
                    "dailyAmountM3" to dailyAmountM3,
                    "coefficientApplied" to coefficient,
                    "isPublicConnection" to true,
                ),
            )
        }

        if (dailyAmountM3 > septicTankCapM3) {
            return ScreeningItemResult(
                itemCode = ScreeningItemCode.SEWAGE,
                status = ScreeningStatus.REQUIRED,
                legalBasis = "환경부고시 「건축물의 용도별 오수발생량 및 정화조 처리대상인원 산정기준」",
                requiredAction = "정화조 처리 상한(2㎥/일, 약 10인) 초과 → 별도 오수처리시설 필요. " +
                    "산정 오수인원 ${personsEstimated}인, 1일 오수량 ${dailyAmountM3}㎥ $estimateNote",
                computedValues = mapOf(
                    "sewagePersonsEstimated" to personsEstimated,
                    "dailyAmountM3" to dailyAmountM3,
                    "coefficientApplied" to coefficient,
                    "septicTankCapM3" to septicTankCapM3,
                ),
            )
        }

        val capacity = building.sewageFacilityCapacityM3
        return when {
            capacity == null -> ScreeningItemResult(
                itemCode = ScreeningItemCode.SEWAGE,
                status = ScreeningStatus.REVIEW,
                legalBasis = "환경부고시 「건축물의 용도별 오수발생량 및 정화조 처리대상인원 산정기준」",
                requiredAction = "기존 처리시설 용량 정보가 대장에 없어 확인 불가 — 관할 관청 확인 필요. " +
                    "산정 오수인원 ${personsEstimated}인, 1일 오수량 ${dailyAmountM3}㎥ $estimateNote",
                computedValues = mapOf(
                    "sewagePersonsEstimated" to personsEstimated,
                    "dailyAmountM3" to dailyAmountM3,
                    "coefficientApplied" to coefficient,
                ),
            )
            capacity >= dailyAmountM3 -> ScreeningItemResult(
                itemCode = ScreeningItemCode.SEWAGE,
                status = ScreeningStatus.OK,
                legalBasis = "환경부고시 「건축물의 용도별 오수발생량 및 정화조 처리대상인원 산정기준」",
                requiredAction = "기존 정화조 용량(${capacity}㎥)이 산정 오수량(${dailyAmountM3}㎥)을 충족 $estimateNote",
                computedValues = mapOf(
                    "sewagePersonsEstimated" to personsEstimated,
                    "dailyAmountM3" to dailyAmountM3,
                    "coefficientApplied" to coefficient,
                    "existingCapacityM3" to capacity,
                ),
            )
            else -> ScreeningItemResult(
                itemCode = ScreeningItemCode.SEWAGE,
                status = ScreeningStatus.REQUIRED,
                legalBasis = "환경부고시 「건축물의 용도별 오수발생량 및 정화조 처리대상인원 산정기준」",
                requiredAction = "기존 정화조 용량(${capacity}㎥) 부족 — 산정 오수량 ${dailyAmountM3}㎥, " +
                    "부족분 약 ${dailyAmountM3.subtract(capacity)}㎥ 증설 필요 $estimateNote",
                computedValues = mapOf(
                    "sewagePersonsEstimated" to personsEstimated,
                    "dailyAmountM3" to dailyAmountM3,
                    "coefficientApplied" to coefficient,
                    "existingCapacityM3" to capacity,
                ),
            )
        }
    }
}
