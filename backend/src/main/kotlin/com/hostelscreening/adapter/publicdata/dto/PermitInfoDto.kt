package com.hostelscreening.adapter.publicdata.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 건축HUB(국토교통부_건축HUB_건축인허가정보 서비스, ArchPmsHubService) getApBasisOulnInfo
 * (기본개요) 응답. archPmsDay/useAprDay/stcnsSchedDay/stcnsDelayDay/realStcnsDay/crtnDay는
 * 공개 레퍼런스(PublicDataReader)로 확인된 필드명이다. 이 서비스는 아직 화면 판정에 쓰이지
 * 않고, Phase 5(경쟁현황) 및 인허가 이력 조회를 위한 클라이언트만 미리 준비해 둔 것이다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ApBasisOulnInfoItem(
    @JsonProperty("platPlc") val jibunAddress: String? = null,
    @JsonProperty("bldNm") val buildingName: String? = null,
    @JsonProperty("archPmsDay") val permitDate: String? = null,       // 건축허가일 (yyyyMMdd)
    @JsonProperty("useAprDay") val approvalDate: String? = null,      // 사용승인일 (yyyyMMdd)
    @JsonProperty("stcnsSchedDay") val constructionScheduledDate: String? = null, // 착공예정일
    @JsonProperty("stcnsDelayDay") val constructionDelayedDate: String? = null,   // 착공연기일
    @JsonProperty("realStcnsDay") val actualConstructionStartDate: String? = null, // 실제착공일
    @JsonProperty("crtnDay") val createdDate: String? = null,         // 데이터 생성일자
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApBasisOulnInfoBody(val items: ApBasisOulnInfoItems? = null, val totalCount: Int? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApBasisOulnInfoItems(val item: List<ApBasisOulnInfoItem> = emptyList())

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApBasisOulnInfoResponse(val header: BuildingHubHeader? = null, val body: ApBasisOulnInfoBody? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApBasisOulnInfoEnvelope(val response: ApBasisOulnInfoResponse? = null)
