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
