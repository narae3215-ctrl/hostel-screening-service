package com.hostelscreening.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

/**
 * POST /api/v1/screenings/by-address 요청 바디 — ScreeningPreviewRequest(BuildingProfile 전체
 * 수동입력)와 달리 주소 하나만 받아 BuildingLookupPort(3.3/3.4)로 실제 건축물대장을 조회한 뒤
 * 판정까지 한 번에 수행한다.
 */
data class ScreeningByAddressRequest(
    @field:NotBlank val address: String,
    @field:NotEmpty val targetFloorLabels: Set<String>,
    val hasKitchenFacility: Boolean = true,
)
