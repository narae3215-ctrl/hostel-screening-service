# hostel-screening-backend

Spring Boot(Kotlin) 백엔드. `docs/01-architecture.md`의 레이어 구조를 그대로 따른다.

## 검증 상태 (2.2 스캐폴딩 시점 기준)

- **도메인 코어(`domain/building`, `domain/screening`, `domain/screening/rules`)**: 이 샌드박스에 번들된
  Kotlin 컴파일러(Gradle 8.14.3 내장, `kotlin-compiler-embeddable-2.0.21.jar`)로 직접 컴파일하고,
  남포동5가 58-1 실사례 데이터로 `ScreeningEngine` 전체 흐름(용도지역 판정 → 종합판정)을 수동 실행해
  4개 검증 항목이 모두 통과함을 확인했다 — 대상면적 177.68㎡, LAND_USE_ZONE=OK, 종합판정=PENDING_LANDUSE.
- **Gradle 정식 빌드(`gradle build`)는 이 샌드박스에서 실행하지 못했다.** 이 환경의 네트워크 정책이
  `plugins.gradle.org`, `repo.maven.apache.org`, `services.gradle.org`로의 접속을 차단하고 있어
  Spring Boot 플러그인/의존성을 내려받을 수 없기 때문이다 (`curl` 테스트 결과 403). 같은 이유로
  `gradlew` 래퍼 스크립트도 아직 생성하지 못했다.
- **실제 개발 환경(로컬 PC 또는 사내 CI)에서 가장 먼저 할 일**:
  1. `gradle wrapper --gradle-version 8.14.3` 실행 → `gradlew`/`gradlew.bat` 생성
  2. `./gradlew build` 실행 → Spring Boot/Kotlin/JPA/Batch 의존성 전체 다운로드 및 컴파일 확인
  3. `./gradlew test` 실행 → `ScreeningEngineTest`(남포동5가 58-1 사례 기준 3개 테스트) 통과 확인

## 로컬 실행 (DB/Redis 준비 후)

```bash
# PostgreSQL(+PostGIS) 기동 후 스키마 적용은 Flyway가 자동 처리 (V1__init_schema.sql)
export DB_USERNAME=hostel_screening
export DB_PASSWORD=changeme
export PUBLIC_DATA_SERVICE_KEY=<공공데이터포털 발급키>
./gradlew bootRun
```

## 패키지 구조

`docs/01-architecture.md` §4 참고. 요약:

- `domain/screening` — 8개 판정 항목 규칙 + 판정 엔진 (프레임워크 비의존 순수 코어)
- `domain/building` — BuildingProfile 등 대장 매핑 모델
- `api` — REST 컨트롤러 (`docs/openapi.yaml` 계약 구현)
- `adapter/publicdata`, `adapter/cache` — 외부 연동 (Phase 3에서 구현)
- `infra/persistence`, `infra/batch` — JPA/배치 (Phase 3~4에서 구현)
