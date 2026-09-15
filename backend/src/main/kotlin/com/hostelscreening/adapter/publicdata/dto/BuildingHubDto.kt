package com.hostelscreening.adapter.publicdata.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 건축HUB(국토교통부_건축물대장정보 서비스, BldRgstHubService) 응답 공통 봉투.
 * data.go.kr Open API 표준 포맷(response.header/body.items.item)을 그대로 따른다 — 이 봉투
 * 구조 자체는 정부 표준 규격이라 신뢰도가 높다. 개별 필드명은 각 DTO 주석을 참고할 것.
 *
 * WebClient.bodyToMono(Class)는 제네릭 타입 소거 때문에 파라미터화된 타입을 안전하게 역직렬화하지
 * 못하므로, 오퍼레이션별 전용 봉투 클래스(BrTitleInfoEnvelope/BrFlrOulnInfoEnvelope)를 따로 둔다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class BuildingHubHeader(
    val resultCode: String? = null,
    val resultMsg: String? = null,
)

/**
 * items.item은 결과가 1건이면 객체, 여러 건이면 배열로 내려오는 경우가 있어(XML→JSON 변환 특유의
 * 흔한 문제) Jackson이 자동으로 List로 매핑하지 못할 수 있다. 실제 응답을 받아보고 단건 응답에서
 * 파싱이 깨지면 커스텀 디시리얼라이저로 교체해야 한다 — 지금은 배열 응답을 기본으로 가정한다.
 */

/**
 * getBrTitleInfo(표제부) 응답 항목. platPlc/totArea/archArea/mainPurpsCdNm/useAprDay/
 * grndFlrCnt/ugrndFlrCnt/strctCdNm/platArea는 여러 공개 레퍼런스(PublicDataReader 등)에서
 * 일관되게 확인된 필드명이다. jiyukCdNm/jiguCdNm/guyukCdNm(용도지역/지구/구역)과
 * violation 관련 필드는 문서로 확정하지 못했으므로 TODO로 남겨둔다 — 실제 응답 1건을 받아
 * 정확한 키로 교체해야 한다(연동 후 디버그 엔드포인트로 확인 예정).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class BrTitleInfoItem(
    @JsonProperty("platPlc") val jibunAddress: String? = null,       // 대지위치
    @JsonProperty("newPlatPlc") val roadAddress: String? = null,     // 도로명대지위치
    @JsonProperty("bldNm") val buildingName: String? = null,
    @JsonProperty("mgmBldrgstPk") val buildingRegistryNo: String? = null, // 관리건축물대장PK — 대장 고유값
    @JsonProperty("platArea") val siteAreaSqm: String? = null,       // 대지면적(㎡) — TODO 검증
    @JsonProperty("archArea") val buildingAreaSqm: String? = null,   // 건축면적(㎡)
    @JsonProperty("totArea") val totalFloorAreaSqm: String? = null,  // 연면적(㎡)
    @JsonProperty("strctCdNm") val mainStructure: String? = null,
    @JsonProperty("mainPurpsCdNm") val mainUsageText: String? = null,
    @JsonProperty("grndFlrCnt") val floorsAboveGround: Int? = null,
    @JsonProperty("ugrndFlrCnt") val floorsBelowGround: Int? = null,
    @JsonProperty("rideUseElvtCnt") val elevatorCount: Int? = null,  // 참고용, 판정에 미사용
    @JsonProperty("useAprDay") val approvalDate: String? = null,     // yyyyMMdd
    @JsonProperty("pmsDay") val permitDate: String? = null,          // yyyyMMdd
    // 아래 3개 필드명은 미확정 — 실제 응답 확인 후 정정 예정 (docs/10 가이드 참고)
    @JsonProperty("jiyukCdNm") val landUseZone: String? = null,      // TODO 검증: 용도지역
    @JsonProperty("jiguCdNm") val landUseDistrict: String? = null,   // TODO 검증: 용도지구
    @JsonProperty("guyukCdNm") val landUseArea: String? = null,      // TODO 검증: 용도구역
    @JsonProperty("violationValue") val violationValue: String? = null, // TODO 검증: 위반건축물여부
)

/**
 * getBrFlrOulnInfo(층별개요) 응답 항목. flrGbCdNm(지상/지하 구분)과 flrNoNm(층번호명, 예: "2층")을
 * 조합해 도메인의 floorLabel("2층", "지1", "옥탑1층")을 만든다. 실제 flrNoNm 표기가 우리 기존
 * 표기(예: "지1")와 다를 수 있어(예: "지하 1층") BuildingHubProfileMapper에서 정규화한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class BrFlrOulnInfoItem(
    @JsonProperty("flrGbCdNm") val floorDivisionName: String? = null, // 지상/지하
    @JsonProperty("flrNo") val floorNo: Int? = null,
    @JsonProperty("flrNoNm") val floorNoName: String? = null,         // 예: "2층", "1층"
    @JsonProperty("area") val areaSqm: String? = null,
    @JsonProperty("mainPurpsCdNm") val usageText: String? = null,
    @JsonProperty("strctCdNm") val structureType: String? = null,
    @JsonProperty("mainAtchGbCdNm") val buildingGroup: String? = null, // 주/부속 구분 — TODO 검증
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BrTitleInfoBody(
    val items: BrTitleInfoItems? = null,
    val totalCount: Int? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BrTitleInfoItems(val item: List<BrTitleInfoItem> = emptyList())

@JsonIgnoreProperties(ignoreUnknown = true)
data class BrTitleInfoResponse(val header: BuildingHubHeader? = null, val body: BrTitleInfoBody? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BrTitleInfoEnvelope(val response: BrTitleInfoResponse? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BrFlrOulnInfoBody(
    val items: BrFlrOulnInfoItems? = null,
    val totalCount: Int? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BrFlrOulnInfoItems(val item: List<BrFlrOulnInfoItem> = emptyList())

@JsonIgnoreProperties(ignoreUnknown = true)
data class BrFlrOulnInfoResponse(val header: BuildingHubHeader? = null, val body: BrFlrOulnInfoBody? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BrFlrOulnInfoEnvelope(val response: BrFlrOulnInfoResponse? = null)
