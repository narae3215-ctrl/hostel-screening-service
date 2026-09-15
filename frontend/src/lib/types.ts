// docs/openapi.yaml의 스키마를 그대로 반영한 타입. 백엔드 계약이 바뀌면 이 파일을 함께 갱신한다.

export interface BuildingFloor {
  floorLabel: string;
  structureType: string;
  usageText: string;
  areaSqm: number;
}

export interface BuildingProfile {
  id: number;
  buildingRegistryNo: string;
  jibunAddress: string;
  parcelNumber: string;
  roadAddress?: string;
  siteAreaSqm?: number | null;
  buildingAreaSqm?: number | null;
  totalFloorAreaSqm?: number | null;
  landUseZone: string;
  landUseDistrict: string;
  mainStructure: string;
  mainUsageText: string;
  floorsBelowGround: number;
  floorsAboveGround: number;
  hasRooftopFloor: boolean;
  approvalDate?: string | null;
  isViolatingBuilding: boolean;
  hadPastViolation: boolean;
  sewageFacilityType?: string | null;
  sewageFacilityCapacityM3?: number | null;
  parkingIndoorCount?: number | null;
  parkingOutdoorCount?: number | null;
  floors: BuildingFloor[];
}

export type ScreeningStatus = 'OK' | 'REQUIRED' | 'REVIEW' | 'NG';

export type ScreeningItemCode =
  | 'LAND_USE_ZONE'
  | 'VIOLATION'
  | 'FIRE_DISTRICT'
  | 'CURRENT_USE'
  | 'DIRECT_STAIR'
  | 'FIRE_SAFETY'
  | 'PARKING'
  | 'SEWAGE';

export interface ScreeningItem {
  itemCode: ScreeningItemCode;
  status: ScreeningStatus;
  legalBasis: string;
  requiredAction: string;
  computedValue?: Record<string, unknown>;
}

export type OverallVerdict = 'PENDING_LANDUSE' | 'CONDITIONAL_OK' | 'NEEDS_REVIEW' | 'DIFFICULT';

export interface ScreeningResult {
  id: number;
  buildingId: number;
  targetFloorLabels: string[];
  targetAreaSqm: number;
  overallVerdict: OverallVerdict;
  summary: string;
  items: ScreeningItem[];
  createdAt: string;
}

/**
 * 항목 표시 순서/한글명 — hostel-use-change-screening 스킬의 8개 판정 항목과 동일한 순서.
 * 백엔드 ScreeningItemCode enum(도메인 판정 순서)과 반드시 일치시킬 것.
 */
export const SCREENING_ITEM_META: Record<ScreeningItemCode, { order: number; label: string }> = {
  LAND_USE_ZONE: { order: 1, label: '용도지역' },
  VIOLATION: { order: 2, label: '위반건축물' },
  FIRE_DISTRICT: { order: 3, label: '방화지구' },
  CURRENT_USE: { order: 4, label: '현재용도' },
  DIRECT_STAIR: { order: 5, label: '직통계단' },
  FIRE_SAFETY: { order: 6, label: '소방시설' },
  PARKING: { order: 7, label: '부설주차장' },
  SEWAGE: { order: 8, label: '오수/하수처리' },
};

/**
 * POST /api/v1/screenings/preview 요청/응답 — 백엔드의 ScreeningPreviewRequest/Response(interim)와 동일.
 * Phase 3(공공데이터 연동)에서 buildingId 기반 정식 계약(ScreeningResult)으로 대체될 예정.
 */
export interface ScreeningPreviewRequest {
  buildingRegistryNo: string;
  jibunAddress: string;
  parcelNumber: string;
  roadAddress?: string | null;
  siteAreaSqm?: number | null;
  buildingAreaSqm?: number | null;
  totalFloorAreaSqm?: number | null;
  landUseZone?: string | null;
  landUseDistrict?: string | null;
  landUseArea?: string | null;
  mainStructure?: string | null;
  mainUsageText?: string | null;
  floorsBelowGround?: number;
  floorsAboveGround?: number;
  hasRooftopFloor?: boolean;
  approvalDate?: string | null;
  isViolatingBuilding?: boolean;
  hadPastViolation?: boolean;
  sewageFacilityType?: string | null;
  sewageFacilityCapacityM3?: number | null;
  parkingIndoorCount?: number | null;
  parkingOutdoorCount?: number | null;
  /** 백엔드 BuildingFloorDto는 buildingGroup(주1/주2 등 대장 동별 구분)을 필수로 요구한다. */
  floors?: (BuildingFloor & { buildingGroup: string })[];
  changeHistory?: { changedAt: string; description: string; isViolationRelated?: boolean }[];
  targetFloorLabels: string[];
  hasKitchenFacility?: boolean;
}

export interface ScreeningPreviewResponse {
  targetFloorLabels: string[];
  targetAreaSqm: number;
  overallVerdict: OverallVerdict;
  disclaimer: string;
  items: ScreeningItem[];
}
