package com.hostelscreening.api

import com.hostelscreening.api.dto.ScreeningByAddressRequest
import com.hostelscreening.api.dto.ScreeningPreviewResponse
import com.hostelscreening.domain.building.BuildingLookupPort
import com.hostelscreening.domain.building.BuildingLookupResult
import com.hostelscreening.domain.screening.ScreeningContext
import com.hostelscreening.domain.screening.ScreeningEngine
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * WBS 4.11 — "주소 → 지오코딩 → 건축물대장 조회(3.3/3.4) → 판정(4.10)"을 한 번에 실행하는
 * 실사용에 가까운 엔드포인트. ScreeningController.preview()는 BuildingProfile을 직접 입력받는
 * Phase 4 검증용 임시 계약이고, 이 컨트롤러는 그 전 단계인 실제 데이터 조회까지 연결한다.
 *
 * 아직 없는 것: DB 저장(buildings 테이블)과 캐싱(3.7) — 매 호출마다 공공데이터 API를 다시
 * 때린다. landUseZone/landUseDistrict(3.5)는 현재 API 재신청 대기로 보류 상태라 null로 떨어질
 * 수 있는데, 이 경우 LandUseZoneRule이 REVIEW로 안전하게 수렴하므로 잘못된 OK 판정으로
 * 이어지지는 않는다(용도지역이 관문 항목이라 REVIEW/REQUIRED면 종합판정도 그에 따라 확정되지
 * 않음 — ScreeningEngine.overallVerdict 참고).
 */
@RestController
class ScreeningByAddressController(
    private val buildingLookupPort: BuildingLookupPort,
    private val screeningEngine: ScreeningEngine,
) {

    @PostMapping("/api/v1/screenings/by-address")
    fun screenByAddress(@Valid @RequestBody request: ScreeningByAddressRequest): ResponseEntity<Any> {
        return when (val result = buildingLookupPort.lookup(request.address)) {
            is BuildingLookupResult.NotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(mapOf("message" to "건축물대장에서 해당 주소의 건물을 찾을 수 없습니다."))

            is BuildingLookupResult.UpstreamError ->
                ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(mapOf("message" to result.message))

            is BuildingLookupResult.Found -> {
                val context = ScreeningContext(
                    building = result.profile,
                    targetFloorLabels = request.targetFloorLabels,
                    hasKitchenFacility = request.hasKitchenFacility,
                )
                val items = screeningEngine.evaluate(context)
                val verdict = screeningEngine.overallVerdict(items)
                ResponseEntity.ok(ScreeningPreviewResponse.of(context, items, verdict))
            }
        }
    }
}
