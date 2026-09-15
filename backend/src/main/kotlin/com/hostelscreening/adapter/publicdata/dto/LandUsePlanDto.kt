package com.hostelscreening.adapter.publicdata.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * WBS 3.5 — 토지이용계획정보 서비스(LandUsePlanService, data.go.kr 1611000) getLandUseAttr 응답.
 *
 * ⚠️ 필드명 미검증 — 건축HUB(3.3/3.4)와 달리 이 API는 배포 후 실제 응답을 아직 받아보지 못했다.
 * 토지이용계획 열람 결과는 보통 "국토의 계획 및 이용에 관한 법률"에 따른 용도지역(행마다 1건,
 * 예: 일반상업지역)과 그 외 개별법(건축법 등)에 따른 지역·지구·구역(행마다 여러 건, 예: 방화지구,
 * 지구단위계획구역)이 각각 별도 row로 내려오는 구조가 일반적이라 그 가정으로 설계했다.
 * prposAreaDstrcCdNm/lawNm 필드명은 추정치이므로, 배포 후 BuildingLookupController의
 * lookup-preview로 실제 응답을 받아 BuildingHub 때와 동일한 방식으로 검증/정정해야 한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class LandUseAttrItem(
    @JsonProperty("pnu") val pnu: String? = null,
    @JsonProperty("reprAddr") val representativeAddress: String? = null,   // 대표지번주소 — TODO 검증
    @JsonProperty("prposAreaDstrcCdNm") val designationName: String? = null, // 지역지구구역명 — TODO 검증
    @JsonProperty("lawNm") val lawName: String? = null, // 근거법령명 — TODO 검증. "국토의 계획 및 이용에
    // 관한 법률"이면 용도지역(landUseZone), 그 외(건축법 등)면 지구/구역(landUseDistrict)으로 분류한다.
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LandUseAttrBody(
    val items: LandUseAttrItems? = null,
    val totalCount: Int? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LandUseAttrItems(val item: List<LandUseAttrItem> = emptyList())

@JsonIgnoreProperties(ignoreUnknown = true)
data class LandUseAttrResponse(val header: BuildingHubHeader? = null, val body: LandUseAttrBody? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LandUseAttrEnvelope(val response: LandUseAttrResponse? = null)
