package com.hostelscreening.adapter.publicdata

import com.hostelscreening.adapter.geocoding.KakaoAddressClient
import com.hostelscreening.domain.building.BuildingLookupPort
import com.hostelscreening.domain.building.BuildingLookupResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * BuildingLookupPort 구현체 — "주소 → 카카오 지오코딩 → 건축HUB 표제부/층별개요 조회 →
 * BuildingProfile 매핑" 전체 흐름을 담당한다. 캐싱(Redis, 3.7)은 아직 붙지 않았으므로
 * 매 호출마다 실제 API를 때린다 — 트래픽이 늘면 반드시 캐시를 앞에 둘 것.
 */
@Component
class BuildingHubLookupAdapter(
    private val kakaoAddressClient: KakaoAddressClient,
    private val buildingHubClient: BuildingHubClient,
) : BuildingLookupPort {

    private val log = LoggerFactory.getLogger(BuildingHubLookupAdapter::class.java)

    override fun lookup(address: String): BuildingLookupResult {
        val parcel = kakaoAddressClient.geocode(address)
            ?: return BuildingLookupResult.UpstreamError("주소를 좌표/지번으로 변환하지 못했습니다: $address")

        val title = try {
            buildingHubClient.fetchTitleInfo(parcel)
        } catch (e: Exception) {
            log.warn("건축HUB 표제부 조회 실패: {}", e.message)
            return BuildingLookupResult.UpstreamError("건축물대장 조회 중 오류가 발생했습니다: ${e.message}")
        } ?: return BuildingLookupResult.NotFound

        val floors = try {
            buildingHubClient.fetchFloorOutline(parcel)
        } catch (e: Exception) {
            log.warn("건축HUB 층별개요 조회 실패: {}", e.message)
            emptyList()
        }

        val profile = BuildingHubProfileMapper.toBuildingProfile(parcel, title, floors)
        return BuildingLookupResult.Found(profile)
    }
}
