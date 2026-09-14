# hostel-screening-frontend

Next.js(App Router) + TypeScript + Tailwind. `docs/wireframe.html`의 SHEET 01(주소 검색)을
실제 컴포넌트(`src/components/AddressSearchForm.tsx`)로 옮긴 첫 화면이 포함되어 있다.

## 검증 상태 (2.3 스캐폴딩 시점 기준)

- **`npm install`은 이 샌드박스에서 실행하지 못했다.** 조직 네트워크 정책이 `registry.npmjs.org`를
  차단하고 있어(`curl` 테스트 결과 403 "Host not in allowlist") `create-next-app`은 물론 어떤 npm
  패키지도 새로 받을 수 없었다. 같은 이유로 `pypi.org`도 막혀 있어, 이 환경은 npm/PyPI/Maven
  생태계 전부에 대해 매우 제한적인 아웃바운드 정책을 쓰고 있는 것으로 보인다 — 백엔드 쪽
  Gradle/Maven Central 차단과 동일한 제약이다.
- 대신 이 샌드박스에 이미 설치되어 있던 전역 TypeScript(`tsc`, v6.0.3)로 `src/` 아래 모든
  `.ts`/`.tsx` 파일을 엄격 모드(strict)로 타입체크했다. React/Next 타입 패키지가 없어 최소
  앰비언트 shim을 임시로 만들어 대입했는데, 그 상태에서도 실제 코드 오류가 2건 발견되어
  즉시 고쳤다 (`onChange` 이벤트 파라미터 암시적 `any` 등). 남은 오류 2건(`globals.css` 사이드
  이펙트 임포트, `process.env`)은 각각 `next dev`가 자동 생성하는 `next-env.d.ts`와
  `@types/node`(devDependencies에 이미 포함됨)가 해결해주는, npm install 전에는 항상 나타나는
  정상적인 경고일 뿐 실제 코드 결함이 아니다.
- **실제 개발 환경에서 가장 먼저 할 일**:
  ```bash
  npm install
  npm run typecheck   # tsc --noEmit
  npm run lint
  npm run dev          # http://localhost:3000
  ```

## 구조

- `src/app/page.tsx` — 랜딩(주소 검색) 화면
- `src/components/AddressSearchForm.tsx` — 주소 입력 → `GET /buildings/search` 호출 → 결과 카드
- `src/lib/api.ts` — 백엔드 API 클라이언트 (`docs/openapi.yaml` 계약 기준)
- `src/lib/types.ts` — OpenAPI 스키마와 1:1 대응하는 TS 타입
- `tailwind.config.ts` — `docs/wireframe.html`의 블루프린트 팔레트를 디자인 토큰으로 이식
