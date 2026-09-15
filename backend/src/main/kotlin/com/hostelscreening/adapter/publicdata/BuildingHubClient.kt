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

    /**
     * 2026-09-15 실제 호출로 확인: 이 오퍼레이션은 totalCount가 실제 층 수보다 훨씬 크게 나온다
     * (과거 이력 개정판이 층별로 쌓여있어 남포동5가 58-1은 실제 층 5개인데 totalCount=12).
     * 한 번에 numOfRows를 크게 줘도 정상 응답하는 편이지만 안전하게 페이지네이션으로 전부 모은다
     * (최대 5페이지=최대 100건까지, 그 이상은 비정상 데이터로 보고 중단). 중복 제거는
     * BuildingHubProfileMapper에서 crtnDay 기준 최신 레코드만 남기는 방식으로 처리한다.
     */
    fun fetchFloorOutline(parcel: GeocodedParcel): List<BrFlrOulnInfoItem> {
        val pageSize = 20
        val collected = mutableListOf<BrFlrOulnInfoItem>()
        var pageNo = 1
        var totalCount = Int.MAX_VALUE

        while (collected.size < totalCount && pageNo <= 5) {
            val envelope = buildingHubWebClient.get()
                .uri { builder ->
                    builder.path("/getBrFlrOulnInfo")
                        .queryParam("serviceKey", decodedServiceKey(publicDataProperties.buildingHub.serviceKey))
                        .queryParam("sigunguCd", parcel.sigunguCd)
                        .queryParam("bjdongCd", parcel.bjdongCd)
                        .queryParam("bun", parcel.bun)
                        .queryParam("ji", parcel.ji)
                        .queryParam("_type", "json")
                        .queryParam("numOfRows", pageSize)
                        .queryParam("pageNo", pageNo)
                        .build()
                }
                .retrieve()
                .bodyToMono(BrFlrOulnInfoEnvelope::class.java)
                .onErrorResume { Mono.empty() }
                .block()

            val body = envelope?.response?.body
            val items = body?.items?.item ?: emptyList()
            if (items.isEmpty()) break

            collected += items
            totalCount = body?.totalCount ?: collected.size
            pageNo++
        }

        return collected
    }
}
