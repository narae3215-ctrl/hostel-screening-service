package com.hostelscreening.adapter.publicdata

import com.hostelscreening.adapter.geocoding.dto.GeocodedParcel
import com.hostelscreening.adapter.publicdata.dto.LandUseAttrEnvelope
import com.hostelscreening.adapter.publicdata.dto.LandUseAttrItem
import com.hostelscreening.config.PublicDataProperties
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * WBS 3.5 — 토지이용계획정보 서비스(LandUsePlanService) getLandUseAttr(지역지구구역 조회) 호출.
 * PNU(필지고유번호)를 파라미터로 쓴다 — GeocodedParcel.pnu 참고. 건축HUB와 달리 이 오퍼레이션은
 * 아직 실제 응답으로 검증하지 못했으니, 배포 후 lookup-preview로 실 데이터를 받아 필드명을
 * 확정해야 한다 (BuildingHubClient 때와 동일한 검증 절차).
 */
@Component
class LandUsePlanClient(
    private val landUsePlanWebClient: WebClient,
    private val publicDataProperties: PublicDataProperties,
) {

    private fun decodedServiceKey(raw: String): String =
        try {
            URLDecoder.decode(raw, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            raw
        }

    fun fetchLandUseAttr(parcel: GeocodedParcel): List<LandUseAttrItem> {
        val envelope = landUsePlanWebClient.get()
            .uri { builder ->
                builder.path("/getLandUseAttr")
                    .queryParam("serviceKey", decodedServiceKey(publicDataProperties.landUse.serviceKey))
                    .queryParam("pnu", parcel.pnu)
                    .queryParam("_type", "json")
                    .queryParam("numOfRows", 50)
                    .queryParam("pageNo", 1)
                    .build()
            }
            .retrieve()
            .bodyToMono(LandUseAttrEnvelope::class.java)
            .onErrorResume { Mono.empty() }
            .block()

        return envelope?.response?.body?.items?.item ?: emptyList()
    }
}
