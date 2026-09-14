package com.hostelscreening.domain.screening.rules

import com.hostelscreening.domain.screening.ScreeningContext
import com.hostelscreening.domain.screening.ScreeningItemCode
import com.hostelscreening.domain.screening.ScreeningItemResult
import com.hostelscreening.domain.screening.ScreeningRule
import com.hostelscreening.domain.screening.ScreeningStatus

/**
 * 판정 항목 ②~⑧의 자리표시자(placeholder) 구현.
 *
 * 이 파일은 WBS Phase 2(프로젝트 스캐폴딩) 산출물이며, 각 항목의 실제 판정 로직은
 * WBS Phase 4(백엔드 - 판정 로직)에서 LandUseZoneRule과 동일한 패턴으로 구현된다:
 *   4.3 위반건축물 · 4.4 방화지구 · 4.5 현재용도 · 4.6 직통계단 ·
 *   4.7 소방시설 · 4.8 부설주차장 · 4.9 오수/하수처리
 *
 * 지금은 ScreeningEngine이 8개 항목 전체에 대해 항상 응답 가능함을 보장하기 위해
 * 모두 REVIEW로 반환한다 (섣불리 OK로 단정하지 않는다는 스킬 원칙과도 합치한다).
 */
private fun placeholder(code: ScreeningItemCode): ScreeningRule = ScreeningRule {
    ScreeningItemResult(
        itemCode = code,
        status = ScreeningStatus.REVIEW,
        legalBasis = "TODO — WBS Phase 4에서 구현 예정",
        requiredAction = "판정 로직 미구현 (스캐폴딩 단계)",
    )
}

class ViolationRule : ScreeningRule by placeholder(ScreeningItemCode.VIOLATION)
class FireDistrictRule : ScreeningRule by placeholder(ScreeningItemCode.FIRE_DISTRICT)
class CurrentUseRule : ScreeningRule by placeholder(ScreeningItemCode.CURRENT_USE)
class DirectStairRule : ScreeningRule by placeholder(ScreeningItemCode.DIRECT_STAIR)
class FireSafetyRule : ScreeningRule by placeholder(ScreeningItemCode.FIRE_SAFETY)
class ParkingRule : ScreeningRule by placeholder(ScreeningItemCode.PARKING)
class SewageRule : ScreeningRule by placeholder(ScreeningItemCode.SEWAGE)
