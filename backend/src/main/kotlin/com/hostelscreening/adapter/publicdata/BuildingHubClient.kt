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
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * 건축HUB(BldRgstHubService) getBrTitleInfo(표제부)/getBrFlrOulnInfo(층별개요) 호출.
 * 이 서비스는 주소 문자열이 아니라 법정동코드+지번(sigunguCd/bjdongCd/bun/ji)을 요구하므로
 * 호출 전 KakaoAddressClient로 먼저 지오코딩해야 한다 (BuildingHubLookupAdapter 참고).
 *
 * serviceKey는 data.go.kr에서 이미 URL 인코딩된 형태("Encoding" 키)로 발급되는 경우가 많다.
 * WebClient의 queryParam()은 값을 자동으로 한 번 더 인코딩하므로, 그대로 넘기면 이중 인코딩되어
 * 서비스키 인증에 실패한다(빈 결과가 조용히 반환됨). 그래서 사용 전에 한 번 디코딩해서 넘긴다 —
 * 이렇게 하면 .env에 Encoding 키/Decoding 키 어느 쪽이 들어있어도 안전하다.
 */
@Component
class BuildingHubClient(
    private val buildingHubWebClient: WebClient,
    private val publicDataProperties: PublicDataProperties,
) {

    private fun decodedServiceKey(raw: String): String =
        try {
            URLDecoder.decode(raw, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            raw
        }

    fun fetchTitleInfo(parcel: GeocodedParcel): BrTitleInfoItem? {
        val envelope = buildingHubWebClient.get()
            .uri { builder ->
                builder.path("/getBrTitleInfo")
                    .queryParam("serviceKey", decodedServiceKey(publicDataProperties.buildingHub.serviceKey))
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
                    .queryParam("serviceKey", decodedServiceKey(publicDataProperties.buildingHub.serviceKey))
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
