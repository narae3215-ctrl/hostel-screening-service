'use client';

import { useState } from 'react';
import { ApiError, runScreeningPreview } from '@/lib/api';
import type { ScreeningPreviewRequest, ScreeningPreviewResponse } from '@/lib/types';
import ScreeningChecklist from './ScreeningChecklist';

/**
 * WBS 6.4 데모 — Phase 3(공공데이터 연동)가 아직 없어 주소 검색으로 실제 건축물대장을
 * 불러올 수 없으므로, 실제 검토 사례(부산 중구 남포동5가 58-1)를 고정 입력으로 사용해
 * 판정 체크리스트 UI 자체를 시연한다. Phase 3 완료 후 AddressSearchForm의 결과를
 * 그대로 이 컴포넌트에 넘기는 흐름으로 교체하면 된다.
 */
const NAMPODONG_SAMPLE: ScreeningPreviewRequest = {
  buildingRegistryNo: '2611014000-1-00580001',
  jibunAddress: '부산광역시 중구 남포동5가',
  parcelNumber: '58-1',
  roadAddress: '부산광역시 중구 자갈치로59번길 5-20(남포동5가)',
  buildingAreaSqm: 92.13,
  totalFloorAreaSqm: 373.81,
  landUseZone: '일반상업',
  landUseDistrict: '방화지구 외 1',
  mainStructure: '철근콘크리트',
  mainUsageText: '근린생활시설, 숙박시설, 위락시설',
  floorsBelowGround: 1,
  floorsAboveGround: 3,
  hasRooftopFloor: true,
  approvalDate: '1977-11-19',
  isViolatingBuilding: false,
  hadPastViolation: true,
  floors: [
    { buildingGroup: '주1', floorLabel: '지1', structureType: '철근콘크리트조', usageText: '위락시설', areaSqm: 96.56 },
    { buildingGroup: '주1', floorLabel: '1층', structureType: '철근콘크리트조', usageText: '근린생활시설', areaSqm: 92.13 },
    { buildingGroup: '주1', floorLabel: '2층', structureType: '철근콘크리트스라브', usageText: '단독주택', areaSqm: 92.13 },
    { buildingGroup: '주1', floorLabel: '3층', structureType: '철근콘크리트스라브', usageText: '단독주택', areaSqm: 85.55 },
    { buildingGroup: '주1', floorLabel: '옥탑1층', structureType: '철근콘크리트스라브', usageText: '단독주택', areaSqm: 7.44 },
  ],
  targetFloorLabels: ['2층', '3층'],
  hasKitchenFacility: true,
};

export default function ScreeningPreviewDemo() {
  const [status, setStatus] = useState<'idle' | 'loading' | 'error' | 'success'>('idle');
  const [errorMessage, setErrorMessage] = useState('');
  const [result, setResult] = useState<ScreeningPreviewResponse | null>(null);

  async function handleRun() {
    setStatus('loading');
    setErrorMessage('');
    try {
      const response = await runScreeningPreview(NAMPODONG_SAMPLE);
      setResult(response);
      setStatus('success');
    } catch (error) {
      const message = error instanceof ApiError ? error.message : '알 수 없는 오류가 발생했습니다.';
      setErrorMessage(message);
      setStatus('error');
    }
  }

  return (
    <div className="flex w-full max-w-2xl flex-col items-center gap-4">
      <div className="w-full rounded border border-slate-300 bg-white p-4 text-center">
        <p className="text-sm font-medium text-ink">판정 결과 체크리스트 미리보기</p>
        <p className="mt-1 text-xs text-review">
          Phase 3(공공데이터 연동) 완료 전까지는 실제 사례(부산 중구 남포동5가 58-1) 데이터로
          8개 판정 항목 체크리스트를 시연합니다.
        </p>
        <button
          type="button"
          onClick={handleRun}
          disabled={status === 'loading'}
          className="mt-3 rounded bg-accent-strong px-4 py-2 text-sm font-medium text-white transition hover:opacity-90 disabled:opacity-50"
        >
          {status === 'loading' ? '판정 실행 중...' : '샘플 데이터로 판정 실행'}
        </button>
      </div>

      {status === 'error' && (
        <p role="alert" className="w-full rounded border border-danger/40 bg-danger/5 px-4 py-3 text-sm text-danger">
          {errorMessage}
        </p>
      )}

      {status === 'success' && result && (
        <ScreeningChecklist
          result={result}
          jibunAddress={NAMPODONG_SAMPLE.jibunAddress}
          parcelNumber={NAMPODONG_SAMPLE.parcelNumber}
        />
      )}
    </div>
  );
}
