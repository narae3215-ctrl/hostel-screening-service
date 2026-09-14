package com.hostelscreening.api

import com.hostelscreening.domain.screening.ScreeningEngine
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 1.5 OpenAPI 명세(docs/openapi.yaml)의 /screenings 경로를 구현할 컨트롤러의 시작점.
 * 전체 엔드포인트(POST /screenings, GET /screenings/{id})는 Phase 4(4.11)에서 완성된다.
 * 지금은 도메인 계층이 API 계층까지 올바르게 주입되는지 확인하는 헬스체크만 둔다.
 */
@RestController
class ScreeningController(private val screeningEngine: ScreeningEngine) {

    @GetMapping("/api/v1/screenings/_health")
    fun health(): Map<String, Any> = mapOf(
        "status" to "ok",
        "ruleCount" to ScreeningEngine.defaultRules().size,
    )
}
