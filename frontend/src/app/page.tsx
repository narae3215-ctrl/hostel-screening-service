import AddressSearchForm from '@/components/AddressSearchForm';
import ScreeningPreviewDemo from '@/components/ScreeningPreviewDemo';

export default function HomePage() {
  return (
    <main className="flex min-h-screen flex-col items-center gap-10 px-4 py-16">
      <div className="flex max-w-xl flex-col items-center gap-2 text-center">
        <span className="text-xs font-medium uppercase tracking-wide text-accent">1차 스크리닝 도구</span>
        <h1 className="text-2xl font-bold text-ink">호스텔 용도변경 가능성 스크리닝</h1>
        <p className="text-sm text-review">
          주소를 입력하면 건축물대장을 자동 조회해 용도지역, 소방시설, 주차, 오수처리 등
          핵심 법적 쟁점을 1차로 정리합니다. 최종 인허가 판단이 아니며, 관할 관청 확인이 필요합니다.
        </p>
      </div>
      <AddressSearchForm />
      <div className="w-full max-w-2xl border-t border-slate-300 pt-8">
        <ScreeningPreviewDemo />
      </div>
    </main>
  );
}
