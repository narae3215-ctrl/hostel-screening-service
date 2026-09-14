package com.hostelscreening.domain.screening

import com.hostelscreening.domain.building.BuildingProfile
import java.math.BigDecimal

/** hostel-use-change-screening 스킬의 8개 판정 항목. 순서가 곧 판정 순서다 (용도지역이 관문이므로 항상 첫 항목). */
enum class ScreeningItemCode(val displayName: String) {
    LAND_USE_ZONE("용도지역"),
    VIOLATION("위반건축물"),
    FIRE_DISTRICT("방화지구"),
    CURRENT_USE("현재용도"),
    DIRECT_STAIR("직통계단"),
    FIRE_SAFETY("소방시설"),
    PARKING("부설주차장"),
    SEWAGE("오수/하수처리"),
}

enum class ScreeningStatus { OK, REQUIRED, REVIEW, NG }

enum class OverallVerdict { PENDING_LANDUSE, CONDITIONAL_OK, NEEDS_REVIEW, DIFFICULT }

/** 판정 요청 컨텍스트 — 스킬 1단계의 "전환하려는 층" 입력에 해당 */
data class ScreeningContext(
    val building: BuildingProfile,
    val targetFloorLabels: Set<String>,
    val hasKitchenFacility: Boolean = true, // 호스텔 기본값: 취사시설 있음 (오수계수 0.067)
) {
    val targetAreaSqm: BigDecimal
        get() = building.floorsIn(targetFloorLabels)
            .fold(BigDecimal.ZERO) { acc, f -> acc + f.areaSqm }
}

data class ScreeningItemResult(
    val itemCode: ScreeningItemCode,
    val status: ScreeningStatus,
    val legalBasis: String,
    val requiredAction: String,
    /** 계산값 — 추정치인 경우 반드시 isEstimate=true로 표시한다 (스킬 원칙: 추정 수치는 추정임을 밝힐 것) */
    val computedValues: Map<String, Any> = emptyMap(),
)

/**
 * 하나의 판정 항목을 계산하는 규칙. 모든 구현체는 외부 의존성(DB, HTTP) 없이
 * ScreeningContext만으로 순수하게 계산해야 한다 — 그래야 법령 개정 시 이 계층만
 * 단위테스트로 검증하고 배포할 수 있다 (01-architecture.md 설계원칙 참고).
 */
fun interface ScreeningRule {
    fun evaluate(context: ScreeningContext): ScreeningItemResult
}
