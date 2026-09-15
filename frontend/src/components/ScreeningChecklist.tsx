'use client';

import { Fragment, useState } from 'react';
import type { ScreeningItem, ScreeningPreviewResponse, ScreeningStatus } from '@/lib/types';
import { SCREENING_ITEM_META } from '@/lib/types';

const STATUS_LABEL: Record<ScreeningStatus, string> = {
  OK: '허용/충족',
  REQUIRED: '설치·확보 필요',
  REVIEW: '도면·관청 확인 필요',
  NG: '불허 근접',
};

const STATUS_STYLE: Record<ScreeningStatus, string> = {
  OK: 'text-ok border-ok',
  REQUIRED: 'text-warn border-warn',
  REVIEW: 'text-review border-review',
  NG: 'text-danger border-danger',
};

const VERDICT_LABEL: Record<ScreeningPreviewResponse['overallVerdict'], string> = {
  PENDING_LANDUSE: '용도지역 관문 확인 대기',
  CONDITIONAL_OK: '조건부 가능',
  NEEDS_REVIEW: '관청 확인 필요',
  DIFFICULT: '전환 어려움',
};

function StatusPill({ status }: { status: ScreeningStatus }) {
  return (
    <span
      className={`inline-block whitespace-nowrap rounded-full border px-2 py-0.5 font-mono text-[0.66rem] font-semibold ${STATUS_STYLE[status]}`}
    >
      {status}
    </span>
  );
}

function sortedItems(items: ScreeningItem[]): ScreeningItem[] {
  return [...items].sort((a, b) => SCREENING_ITEM_META[a.itemCode].order - SCREENING_ITEM_META[b.itemCode].order);
}

/** hostel-use-change-screening 스킬 2단계 — 토지이음(eum.go.kr) 조회 지시문 그대로 생성 */
function buildLandUseLookupPrompt(jibunAddress: string, parcelNumber: string): string {
  return `토지이음(eum.go.kr)에서 아래 지번의 토지이용계획을 조회하고, 결과를 지정한 형식으로 정리해줘.

[조회 지번] ${jibunAddress} ${parcelNumber}

[확인할 항목]
1. 용도지역·지구·구역 전체
2. "숙박시설" 행위제한 내용 — 가능 / 제한 / 불가 중 무엇인지.
   ※ 호스텔은 건축법 별표1의 숙박시설(제15호)에 해당하므로 '숙박시설' 항목의 건축 가능 여부를 확인할 것.
3. 지구단위계획 지정 여부 — 지정 시 명칭과, 해당 획지 허용/불허 용도에 숙박시설이 포함되는지.
4. 그 밖에 숙박시설 입지에 영향을 주는 지역·지구·구역 지정 여부.

[답변 형식]
- 용도지역:
- 지구/구역:
- 숙박시설 행위제한: (가능/제한/불가) + 근거 문구 원문
- 지구단위계획: (지정 여부 / 명칭 / 숙박시설 허용 여부)`;
}

interface ScreeningChecklistProps {
  result: ScreeningPreviewResponse;
  jibunAddress: string;
  parcelNumber: string;
}

export default function ScreeningChecklist({ result, jibunAddress, parcelNumber }: ScreeningChecklistProps) {
  const [expandedCode, setExpandedCode] = useState<string | null>(null);
  const [copyState, setCopyState] = useState<'idle' | 'copied' | 'error'>('idle');

  const items = sortedItems(result.items);
  const attentionItems = items.filter((item) => item.status !== 'OK');

  async function handleCopyLookupPrompt() {
    const text = buildLandUseLookupPrompt(jibunAddress, parcelNumber);
    try {
      await navigator.clipboard.writeText(text);
      setCopyState('copied');
    } catch {
      setCopyState('error');
    }
    window.setTimeout(() => setCopyState('idle'), 2000);
  }

  return (
    <div className="flex w-full max-w-2xl flex-col gap-4">
      {/* 상단 고정 배너 — 스크롤해도 항상 노출 */}
      <div className="sticky top-0 z-10 rounded border border-warn/40 bg-warn/10 px-4 py-3 text-xs font-medium text-warn">
        {result.disclaimer}
      </div>

      <div className="rounded border border-slate-300 bg-white p-4">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-base font-semibold text-ink">판정 결과 체크리스트</h2>
          <span className="rounded-full border border-accent px-3 py-1 font-mono text-xs font-semibold text-accent">
            종합판정: {VERDICT_LABEL[result.overallVerdict]}
          </span>
        </div>
        <p className="mt-1 text-xs text-review">
          전환 대상 층: {result.targetFloorLabels.join(', ')} (합계 {result.targetAreaSqm}㎡)
        </p>

        {/* 범례 */}
        <div className="mt-3 flex flex-wrap gap-x-5 gap-y-2 rounded border border-slate-200 bg-slate-50 px-3 py-2 font-mono text-xs">
          {(Object.keys(STATUS_LABEL) as ScreeningStatus[]).map((status) => (
            <div key={status} className="flex items-center gap-1.5">
              <StatusPill status={status} />
              <span className="text-review">{STATUS_LABEL[status]}</span>
            </div>
          ))}
        </div>

        {/* 체크리스트 테이블 */}
        <table className="mt-3 w-full border-collapse text-sm">
          <thead>
            <tr className="bg-slate-100 text-left text-xs font-semibold tracking-wide text-ink">
              <th className="border border-slate-200 px-2 py-2">항목</th>
              <th className="border border-slate-200 px-2 py-2">상태</th>
              <th className="border border-slate-200 px-2 py-2">근거 법령(요약)</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => {
              const meta = SCREENING_ITEM_META[item.itemCode];
              const isExpanded = expandedCode === item.itemCode;
              return (
                <Fragment key={item.itemCode}>
                  <tr
                    onClick={() => setExpandedCode(isExpanded ? null : item.itemCode)}
                    className="cursor-pointer align-top hover:bg-accent/5"
                  >
                    <td className="border border-slate-200 px-2 py-2 font-medium text-ink">
                      {meta.order}. {meta.label}
                    </td>
                    <td className="border border-slate-200 px-2 py-2">
                      <StatusPill status={item.status} />
                    </td>
                    <td className="border border-slate-200 px-2 py-2 text-xs text-review">{item.legalBasis}</td>
                  </tr>
                  {isExpanded && (
                    <tr>
                      <td colSpan={3} className="border border-slate-200 bg-slate-50 px-3 py-3 text-xs">
                        <p className="font-semibold text-ink">필요 조치</p>
                        <p className="mt-1 whitespace-pre-wrap text-review">{item.requiredAction}</p>
                        {item.computedValue && Object.keys(item.computedValue).length > 0 && (
                          <p className="mt-2 font-mono text-[0.7rem] text-slate-400">
                            {Object.entries(item.computedValue)
                              .map(([key, value]) => `${key}=${value}`)
                              .join(' · ')}
                          </p>
                        )}
                      </td>
                    </tr>
                  )}
                </Fragment>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* 관청 확인 필수 항목 */}
      <div className="rounded border border-slate-300 bg-white p-4">
        <h3 className="text-sm font-semibold text-ink">관청 확인 필수 항목 ({attentionItems.length}건)</h3>
        {attentionItems.length === 0 ? (
          <p className="mt-2 text-xs text-review">모든 항목이 OK입니다. 다만 최종 판단은 관할 관청 확인 후 내려주세요.</p>
        ) : (
          <ul className="mt-2 flex flex-col gap-1.5 text-xs">
            {attentionItems.map((item) => (
              <li key={item.itemCode} className="flex items-start gap-2">
                <StatusPill status={item.status} />
                <span className="text-ink">
                  {SCREENING_ITEM_META[item.itemCode].label} — {item.requiredAction}
                </span>
              </li>
            ))}
          </ul>
        )}

        <button
          type="button"
          onClick={handleCopyLookupPrompt}
          className="mt-4 w-full rounded border border-accent px-4 py-2 text-sm font-medium text-accent transition hover:bg-accent/10"
        >
          {copyState === 'copied'
            ? '복사되었습니다 — 토지이음 조회에 붙여넣어 사용하세요'
            : copyState === 'error'
              ? '복사 실패 — 아래 텍스트를 직접 선택해 복사해주세요'
              : '용도지역 관문 확정용 토지이음(eum.go.kr) 조회 지시문 복사'}
        </button>
      </div>
    </div>
  );
}
