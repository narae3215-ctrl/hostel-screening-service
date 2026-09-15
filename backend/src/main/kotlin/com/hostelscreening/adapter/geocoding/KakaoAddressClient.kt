package com.hostelscreening.adapter.geocoding

import com.hostelscreening.adapter.geocoding.dto.GeocodedParcel
import com.hostelscreening.adapter.geocoding.dto.KakaoAddressDocument
import com.hostelscreening.adapter.geocoding.dto.KakaoAddressSearchResponse
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

/**
 * 카카오 로컬 API로 자유 텍스트 주소를 건축HUB가 요구하는 법정동코드(시군구+법정동)/지번으로
 * 변환한다. 건축HUB(getBrTitleInfo 등)는 주소 문자열이 아니라 이 코드들을 파라미터로 받기
 * 때문에, "주소 검색 → 건축물대장 조회" 흐름에서 이 어댑터가 반드시 선행되어야 한다.
 */
@Component
class KakaoAddressClient(private val kakaoLocalWebClient: WebClient) {

    /** 지번주소를 우선 사용한다 — 건축HUB 조회 및 토지이음 조회 모두 지번 기준이어야 한다. */
    fun geocode(address: String): GeocodedParcel? {
        val response = kakaoLocalWebClient.get()
            .uri { builder ->
                builder.path("/search/address.json")
                    .queryParam("query", address)
                    .build()
            }
            .retrieve()
            .bodyToMono(KakaoAddressSearchResponse::class.java)
            .onErrorResume { Mono.empty() }
            .block()

        val document = response?.documents?.firstOrNull() ?: return null
        return toParcel(document)
    }

    private fun toParcel(document: KakaoAddressDocument): GeocodedParcel? {
        val addr = document.address ?: return null
        val bCode = addr.bCode?.takeIf { it.length == 10 } ?: return null

        // TODO(검증 필요): b_code(10자리) = 시군구코드(5) + 법정동코드(5) 분할 규칙과
        // main_address_no/sub_address_no → bun/ji 4자리 0-padding 변환은 표준 관례를 따른
        // 것으로, 실제 응답 1건을 받아본 뒤 sigunguCd/bjdongCd/bun/ji 값이 건축HUB 호출에서
        // 실제로 데이터를 찾아오는지(404가 아닌지) 반드시 확인해야 한다.
        val sigunguCd = bCode.substring(0, 5)
        val bjdongCd = bCode.substring(5, 10)
        val bun = (addr.mainAddressNo?.takeIf { it.isNotBlank() } ?: "0").padStart(4, '0')
        val ji = (addr.subAddressNo?.takeIf { it.isNotBlank() } ?: "0").padStart(4, '0')

        return GeocodedParcel(
            sigunguCd = sigunguCd,
            bjdongCd = bjdongCd,
            bun = bun,
            ji = ji,
            jibunAddress = addr.addressName ?: document.addressName ?: "",
            roadAddress = document.roadAddress?.addressName,
            longitude = document.x?.toDoubleOrNull(),
            latitude = document.y?.toDoubleOrNull(),
        )
    }
}
