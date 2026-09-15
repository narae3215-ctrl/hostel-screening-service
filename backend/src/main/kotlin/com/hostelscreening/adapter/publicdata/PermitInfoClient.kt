package com.hostelscreening.adapter.publicdata

import com.hostelscreening.adapter.geocoding.dto.GeocodedParcel
import com.hostelscreening.adapter.publicdata.dto.ApBasisOulnInfoEnvelope
import com.hostelscreening.adapter.publicdata.dto.ApBasisOulnInfoItem
import com.hostelscreening.config.PublicDataProperties
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

/**
 * WBS 3.4 — 건축인허가정보 서비스(ArchPmsHubService) getApBasisOulnInfo(기본개요) 호출.
 * 건축물대장(3.3)과 파라미터 체계(sigunguCd/bjdongCd/bun/ji)는 동일하다. 현재는 화면 판정에
 * 쓰이지 않으며, Phase 5(경쟁현황/인허가 이력)에서 소비할 예정이라 클라이언트만 준비해 둔다.
 */
@Component
class PermitInfoClient(
    private val permitInfoWebClient: WebClient,
    private val publicDataProperties: PublicDataProperties,
) {

    fun fetchBasisOutline(parcel: GeocodedParcel): List<ApBasisOulnInfoItem> {
        val envelope = permitInfoWebClient.get()
            .uri { builder ->
                builder.path("/getApBasisOulnInfo")
                    .queryParam("serviceKey", publicDataProperties.permitInfo.serviceKey)
                    .queryParam("sigunguCd", parcel.sigunguCd)
                    .queryParam("bjdongCd", parcel.bjdongCd)
                    .queryParam("bun", parcel.bun)
                    .queryParam("ji", parcel.ji)
                    .queryParam("_type", "json")
                    .queryParam("numOfRows", 50)
                    .build()
            }
            .retrieve()
            .bodyToMono(ApBasisOulnInfoEnvelope::class.java)
            .onErrorResume { Mono.empty() }
            .block()

        return envelope?.response?.body?.items?.item ?: emptyList()
    }
}
