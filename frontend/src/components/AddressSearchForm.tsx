'use client';

import { useState, type ChangeEvent, type FormEvent } from 'react';
import { ApiError, searchBuildingByAddress } from '@/lib/api';
import type { BuildingProfile } from '@/lib/types';

const EXAMPLE_ADDRESSES = ['부산광역시 중구 남포동5가 58-1', '서울 강남구 삼성로149길 30-1'];

export default function AddressSearchForm() {
  const [address, setAddress] = useState('');
  const [status, setStatus] = useState<'idle' | 'loading' | 'error' | 'success'>('idle');
  const [errorMessage, setErrorMessage] = useState('');
  const [building, setBuilding] = useState<BuildingProfile | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!address.trim()) return;

    setStatus('loading');
    setErrorMessage('');
    try {
      const result = await searchBuildingByAddress(address.trim());
      setBuilding(result);
      setStatus('success');
    } catch (error) {
      const message = error instanceof ApiError ? error.message : '알 수 없는 오류가 발생했습니다.';
      setErrorMessage(message);
      setStatus('error');
    }
  }

  return (
    <div className="flex w-full max-w-xl flex-col gap-4">
      <form onSubmit={handleSubmit} className="flex flex-col gap-3">
        <label htmlFor="address-input" className="text-sm font-medium text-ink">
          지번 또는 도로명 주소
        </label>
        <input
          id="address-input"
          name="address"
          type="text"
          value={address}
          onChange={(event: ChangeEvent<HTMLInputElement>) => setAddress(event.target.value)}
          placeholder="예: 부산광역시 중구 남포동5가 58-1"
          className="rounded border border-slate-300 bg-white px-4 py-3 text-base outline-none focus:border-accent focus:ring-2 focus:ring-accent/30"
        />
        <button
          type="submit"
          disabled={status === 'loading'}
          className="rounded bg-accent-strong px-4 py-3 font-medium text-white transition hover:opacity-90 disabled:opacity-50"
        >
          {status === 'loading' ? '건축물대장 조회 중...' : '용도변경 가능성 스크리닝'}
        </button>
      </form>

      <div className="flex flex-wrap gap-2 text-xs text-review">
        {EXAMPLE_ADDRESSES.map((example) => (
          <button
            key={example}
            type="button"
            onClick={() => setAddress(example)}
            className="rounded-full border border-slate-300 px-3 py-1 hover:border-accent hover:text-accent"
          >
            {example}
          </button>
        ))}
      </div>

      {status === 'error' && (
        <p role="alert" className="rounded border border-danger/40 bg-danger/5 px-4 py-3 text-sm text-danger">
          {errorMessage}
        </p>
      )}

      {status === 'success' && building && (
        <div className="rounded border border-slate-300 bg-white p-4 text-sm">
          <p className="font-mono text-xs text-review">{building.buildingRegistryNo}</p>
          <p className="mt-1 text-base font-semibold">{building.jibunAddress}</p>
          <p className="text-review">{building.roadAddress}</p>
          <dl className="mt-3 grid grid-cols-2 gap-2 font-mono text-xs">
            <div>
              <dt className="text-review">용도지역</dt>
              <dd>{building.landUseZone}</dd>
            </div>
            <div>
              <dt className="text-review">연면적</dt>
              <dd>{building.totalFloorAreaSqm ?? '-'}㎡</dd>
            </div>
          </dl>
        </div>
      )}
    </div>
  );
}
