package com.hostelscreening.adapter.geocoding.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 카카오 로컬(Local) API "주소 검색" (GET /v2/local/search/address.json) 응답.
 * 필드명은 카카오 공식 문서(다음 키워드로 오래 안정적으로 유지되어 온 스키마)를 따른다:
 * documents[].address.b_code(법정동코드 10자리), main_address_no(지번 본번), sub_address_no(부번).
 * 건축HUB API의 sigunguCd(5)+bjdongCd(5)는 이 b_code를 앞 5자리/뒤 5자리로 나눈 값이다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class KakaoAddressSearchResponse(
    val documents: List<KakaoAddressDocument> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KakaoAddressDocument(
    @JsonProperty("address_name") val addressName: String? = null,
    val address: KakaoAddress? = null,
    @JsonProperty("road_address") val roadAddress: KakaoRoadAddress? = null,
    val x: String? = null, // 경도
    val y: String? = null, // 위도
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KakaoAddress(
    @JsonProperty("address_name") val addressName: String? = null,
    @JsonProperty("region_1depth_name") val region1: String? = null,
    @JsonProperty("region_2depth_name") val region2: String? = null,
    @JsonProperty("region_3depth_name") val region3: String? = null,
    @JsonProperty("b_code") val bCode: String? = null, // 법정동코드 10자리
    @JsonProperty("main_address_no") val mainAddressNo: String? = null, // 지번 본번
    @JsonProperty("sub_address_no") val subAddressNo: String? = null, // 지번 부번 (없으면 "")
    @JsonProperty("mountain_yn") val mountainYn: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KakaoRoadAddress(
    @JsonProperty("address_name") val addressName: String? = null,
)

/** 건축HUB API 호출에 필요한 형태로 정리한 지오코딩 결과. */
data class GeocodedParcel(
    val sigunguCd: String, // 법정동코드 앞 5자리
    val bjdongCd: String,  // 법정동코드 뒤 5자리
    val bun: String,       // 지번 본번 4자리(0-padded)
    val ji: String,        // 지번 부번 4자리(0-padded, 없으면 "0000")
    val jibunAddress: String,
    val roadAddress: String?,
    val longitude: Double?,
    val latitude: Double?,
    val isMountain: Boolean = false, // 카카오 mountain_yn — 산지번 여부, PNU 생성에 필요
) {
    /**
     * WBS 3.5 토지이용계획 API(LandUsePlanService)는 sigunguCd/bjdongCd/bun/ji가 아니라
     * PNU(필지고유번호, 19자리 = 법정동코드10 + 산/일반구분1 + 본번4 + 부번4)를 요구한다.
     * 산/일반구분: 산지번이면 "2", 일반(대지)이면 "1" — 지적행정 표준 표기를 따른다.
     */
    val pnu: String
        get() = "$sigunguCd$bjdongCd${if (isMountain) "2" else "1"}$bun$ji"
}
