import type { BuildingProfile, ScreeningPreviewRequest, ScreeningPreviewResponse } from './types';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080/api/v1';

export class ApiError extends Error {
  constructor(
    message: string,
    public status?: number,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

/** GET /buildings/search — docs/openapi.yaml 계약과 동일 */
export async function searchBuildingByAddress(address: string): Promise<BuildingProfile> {
  const url = `${API_BASE_URL}/buildings/search?address=${encodeURIComponent(address)}`;

  let response: Response;
  try {
    response = await fetch(url, { method: 'GET' });
  } catch (cause) {
    throw new ApiError('백엔드 API에 연결할 수 없습니다. 서버가 실행 중인지 확인해주세요.');
  }

  if (response.status === 404) {
    throw new ApiError('건축물대장에서 해당 주소의 건물을 찾을 수 없습니다.', 404);
  }
  if (!response.ok) {
    throw new ApiError('공공데이터 조회 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.', response.status);
  }

  return (await response.json()) as BuildingProfile;
}

/**
 * POST /screenings/preview — Phase 4 판정 엔진을 시연하기 위한 임시 엔드포인트.
 * Phase 3(공공데이터 연동)에서 buildingId 기반 저장/조회가 완성되면 실제 조회 결과를
 * 이 함수 대신 buildingId로 넘기는 정식 엔드포인트로 교체한다.
 */
export async function runScreeningPreview(
  payload: ScreeningPreviewRequest,
): Promise<ScreeningPreviewResponse> {
  const url = `${API_BASE_URL}/screenings/preview`;

  let response: Response;
  try {
    response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
  } catch (cause) {
    throw new ApiError('백엔드 API에 연결할 수 없습니다. 서버가 실행 중인지 확인해주세요.');
  }

  if (!response.ok) {
    throw new ApiError('판정 엔진 호출 중 오류가 발생했습니다. 입력값을 확인해주세요.', response.status);
  }

  return (await response.json()) as ScreeningPreviewResponse;
}
