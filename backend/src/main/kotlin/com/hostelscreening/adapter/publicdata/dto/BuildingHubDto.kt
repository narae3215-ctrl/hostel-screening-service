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
 * 일관되게 확인된 필드명이다.
 *
 * 2026-09-15 실제 응답(부산 중구 남포동5가 58-1)으로 확인한 결과, jiyukCdNm/jiguCdNm/guyukCdNm
 * (용도지역/지구/구역)과 violationValue 필드는 이 오퍼레이션 응답에 아예 존재하지 않는다 —
 * 용도지역/지구/구역은 별도의 토지이용계획 API(LandUsePlanService, WBS 3.5 미구현)에서 가져와야
 * 하는 데이터였다. 대신 실제 응답에는 regstrKindCdNm("일반건축물"/"위반건축물")이 있었고, 이게
 * 위반건축물여부를 나타내는 진짜 필드다 — BuildingHubProfileMapper에서 이 필드로 판정한다.
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
    // 2026-09-15 실제 응답으로 확인: 표제부 자체엔 위반건축물여부를 나타내는 이 필드가 있다.
    @JsonProperty("regstrKindCdNm") val registryKindName: String? = null, // "일반건축물" / "위반건축물"
    // 용도지역/지구/구역은 표제부 응답에 없음이 확인됨 — LandUsePlanService(3.5) 연동 전까지 항상 null.
    val landUseZone: String? = null,
    val landUseDistrict: String? = null,
    val landUseArea: String? = null,
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
    // 2026-09-15 확인: 이 오퍼레이션은 totalCount가 실제 층 수보다 훨씬 많이 나온다(과거 이력
    // 개정판이 층별로 여러 건 쌓여있음). crtnDay(데이터 생성일자)로 최신 레코드만 골라 중복 제거한다.
    @JsonProperty("crtnDay") val createdDate: String? = null,
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
