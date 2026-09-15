package com.hostelscreening.domain.building

/**
 * 주소로 건축물대장을 조회하는 포트 — 도메인/API 계층은 이 인터페이스만 알고,
 * 실제 구현(카카오 지오코딩 + 건축HUB 호출)은 adapter/publicdata가 담당한다
 * (01-architecture.md 포트-어댑터 원칙).
 */
interface BuildingLookupPort {
    fun lookup(address: String): BuildingLookupResult
}

sealed interface BuildingLookupResult {
    data class Found(val profile: BuildingProfile) : BuildingLookupResult
    data object NotFound : BuildingLookupResult
    data class UpstreamError(val message: String) : BuildingLookupResult
}
