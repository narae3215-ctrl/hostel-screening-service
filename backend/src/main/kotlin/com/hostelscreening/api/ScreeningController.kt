package com.hostelscreening.api

import com.hostelscreening.api.dto.ScreeningPreviewRequest
import com.hostelscreening.api.dto.ScreeningPreviewResponse
import com.hostelscreening.domain.screening.ScreeningContext
import com.hostelscreening.domain.screening.ScreeningEngine
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * docs/openapi.yaml의 /screenings 경로를 구현하는 컨트롤러.
 *
 * POST /screenings/preview 는 WBS Phase 4(판정 로직) 검증·시연을 위한 임시 엔드포인트다 —
 * openapi.yaml의 정식 POST /screenings(buildingId 기반)은 Phase 3(공공데이터 연동)에서
 * 건물 저장/조회가 구현된 뒤 완성된다. 판정 엔진 자체(ScreeningEngine, 8개 규칙)는
 * 이미 완전히 구현되어 있으므로, 이 엔드포인트는 입력 계약만 다를 뿐 실제 판정 로직은 동일하다.
 */
@RestController
class ScreeningController(private val screeningEngine: ScreeningEngine) {

    @GetMapping("/api/v1/screenings/_health")
    fun health(): Map<String, Any> = mapOf(
        "status" to "ok",
        "ruleCount" to ScreeningEngine.defaultRules().size,
    )

    @PostMapping("/api/v1/screenings/preview")
    fun preview(@Valid @RequestBody request: ScreeningPreviewRequest): ScreeningPreviewResponse {
        val context = ScreeningContext(
            building = request.toBuildingProfile(),
            targetFloorLabels = request.targetFloorLabels,
            hasKitchenFacility = request.hasKitchenFacility,
        )
        val items = screeningEngine.evaluate(context)
        val verdict = screeningEngine.overallVerdict(items)
        return ScreeningPreviewResponse.of(context, items, verdict)
    }
}
