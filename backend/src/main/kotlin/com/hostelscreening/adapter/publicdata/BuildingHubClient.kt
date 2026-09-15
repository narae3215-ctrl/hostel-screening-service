package com.hostelscreening.adapter.publicdata

import com.hostelscreening.adapter.geocoding.dto.GeocodedParcel
import com.hostelscreening.adapter.publicdata.dto.BrFlrOulnInfoEnvelope
import com.hostelscreening.adapter.publicdata.dto.BrFlrOulnInfoItem
import com.hostelscreening.adapter.publicdata.dto.BrTitleInfoEnvelope
import com.hostelscreening.adapter.publicdata.dto.BrTitleInfoItem
import com.hostelscreening.config.PublicDataProperties
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

/**
 * 건축HUB(BldRgstHubService) getBrTitleInfo(표제부)/getBrFlrOulnInfo(층별개요) 호출.
 * 이 서비스는 주소 문자열이 아니라 법정동코드+지번(sigunguCd/bjdongCd/bun/ji)을 요구하므로
 * 호출 전 KakaoAddressClient로 먼저 지오코딩해야 한다 (BuildingHubLookupAdapter 참고).
 */
@Component
class BuildingHubClient(
    private val buildingHubWebClient: WebClient,
    private val publicDataProperties: PublicDataProperties,
) {

    fun fetchTitleInfo(parcel: GeocodedParcel): BrTitleInfoItem? {
        val envelope = buildingHubWebClient.get()
            .uri { builder ->
                builder.path("/getBrTitleInfo")
                    .queryParam("serviceKey", publicDataProperties.buildingHub.serviceKey)
                    .queryParam("sigunguCd", parcel.sigunguCd)
                    .queryParam("bjdongCd", parcel.bjdongCd)
                    .queryParam("bun", parcel.bun)
                    .queryParam("ji", parcel.ji)
                    .queryParam("_type", "json")
                    .queryParam("numOfRows", 10)
                    .build()
            }
            .retrieve()
            .bodyToMono(BrTitleInfoEnvelope::class.java)
            .onErrorResume { Mono.empty() }
            .block()

        return envelope?.response?.body?.items?.item?.firstOrNull()
    }

    fun fetchFloorOutline(parcel: GeocodedParcel): List<BrFlrOulnInfoItem> {
        val envelope = buildingHubWebClient.get()
            .uri { builder ->
                builder.path("/getBrFlrOulnInfo")
                    .queryParam("serviceKey", publicDataProperties.buildingHub.serviceKey)
                    .queryParam("sigunguCd", parcel.sigunguCd)
                    .queryParam("bjdongCd", parcel.bjdongCd)
                    .queryParam("bun", parcel.bun)
                    .queryParam("ji", parcel.ji)
                    .queryParam("_type", "json")
                    .queryParam("numOfRows", 100) // 층 수가 많은 건물 대비 여유 있게 조회
                    .build()
            }
            .retrieve()
            .bodyToMono(BrFlrOulnInfoEnvelope::class.java)
            .onErrorResume { Mono.empty() }
            .block()

        return envelope?.response?.body?.items?.item ?: emptyList()
    }
}
