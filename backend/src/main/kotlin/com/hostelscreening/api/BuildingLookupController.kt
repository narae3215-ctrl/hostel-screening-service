package com.hostelscreening.api

import com.hostelscreening.domain.building.BuildingLookupPort
import com.hostelscreening.domain.building.BuildingLookupResult
import com.hostelscreening.domain.building.BuildingProfile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * WBS 3.3 진단용 임시 엔드포인트 — 카카오 지오코딩 + 건축HUB 클라이언트가 실제로 정상 동작하는지
 * 확인하기 위한 것이다. docs/openapi.yaml의 정식 GET /buildings/search(캐시+DB 저장)는 아직
 * 아니다 — 그건 3.7(캐싱)과 buildings 테이블 저장 로직이 붙어야 완성된다.
 *
 * 특히 BuildingHubProfileMapper에 TODO로 남긴 필드(용도지역/지구/구역, 위반건축물여부, 주차,
 * 하수처리)가 실제 정부 API 응답에서 어떤 키로 오는지 확정하지 못한 상태라, 이 엔드포인트로
 * 실제 주소를 조회해 결과를 확인하고 필요하면 매핑을 정정해야 한다.
 */
@RestController
class BuildingLookupController(private val buildingLookupPort: BuildingLookupPort) {

    @GetMapping("/api/v1/buildings/lookup-preview")
    fun lookupPreview(@RequestParam address: String): ResponseEntity<Any> {
        return when (val result = buildingLookupPort.lookup(address)) {
            is BuildingLookupResult.Found -> ResponseEntity.ok(toResponse(result.profile))
            is BuildingLookupResult.NotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "건축물대장에서 해당 주소의 건물을 찾을 수 없습니다."))
            is BuildingLookupResult.UpstreamError ->
                ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(mapOf("message" to result.message))
        }
    }

    private fun toResponse(profile: BuildingProfile) = mapOf(
        "profile" to profile,
        "note" to "이 값 중 landUseZone/landUseDistrict/landUseArea/isViolatingBuilding/" +
            "parkingIndoorCount/parkingOutdoorCount/sewageFacilityType/sewageFacilityCapacityM3는 " +
            "필드명이 검증되지 않은 상태입니다. 실제 주소로 조회해본 뒤 기대값과 다르면 스크린샷/JSON을 " +
            "공유해주세요 — BuildingHubProfileMapper의 매핑을 바로 정정하겠습니다.",
    )
}
