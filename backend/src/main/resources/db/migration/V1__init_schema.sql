-- 1.4 DB 스키마 설계
-- 호스텔 용도변경 1차 스크리닝 서비스 — 초기 스키마 (Flyway V1)
-- 대상: PostgreSQL 15+ / PostGIS 3.3+
--
-- 설계 근거:
--  - buildings/building_floors는 실제 건축물대장(표제부/을 건축물현황)의 구조를 그대로 반영한다.
--    (2026.09.10 남포동5가 58-1 대장 분석 사례: 표제부 1건 + 층별현황 N건 + 변동사항 N건 구조)
--  - screening_items는 hostel-use-change-screening 스킬의 8개 판정 항목
--    (용도지역/위반건축물/방화지구/현재용도/직통계단/소방시설/주차/오수하수)을 그대로 테이블화한다.
--    항목이 늘어나도 스키마 변경 없이 행을 추가하면 되도록 EAV형으로 설계했다.
--  - 모든 면적/수량 컬럼은 NUMERIC으로 두고, 대장에 값이 없으면 NULL을 허용한다.
--    (NULL = "REVIEW 대상"이라는 도메인 신호이지 오류가 아니다 — 01-architecture.md 설계원칙 참고)

CREATE EXTENSION IF NOT EXISTS postgis;

-- =========================================================
-- 1. 건물 기본정보 (건축물대장 표제부 매핑)
-- =========================================================
CREATE TABLE buildings (
    id                      BIGSERIAL PRIMARY KEY,
    building_registry_no    VARCHAR(30)  NOT NULL UNIQUE,   -- 고유번호 (예: 2611014000-1-00580001)
    building_id_gov         VARCHAR(30),                    -- 건물ID (정부24 기준)
    jibun_address           VARCHAR(200) NOT NULL,          -- 대지위치 (예: 부산광역시 중구 남포동5가)
    parcel_number           VARCHAR(30)  NOT NULL,          -- 지번 (예: 58-1)  ※ 토지이음 조회는 이 값 기준
    road_address             VARCHAR(200),                   -- 도로명주소
    site_area_sqm           NUMERIC(10,2),                  -- 대지면적(㎡) — 총괄표제부에만 있는 경우 NULL 가능
    building_area_sqm       NUMERIC(10,2),                  -- 건축면적(㎡)
    total_floor_area_sqm    NUMERIC(10,2),                  -- 연면적(㎡)
    floor_area_ratio_base_sqm NUMERIC(10,2),                -- 용적률산정용연면적(㎡)
    land_use_zone           VARCHAR(50),                    -- 용도지역 (예: 일반상업)
    land_use_district       VARCHAR(100),                   -- 지구 (예: 방화지구 외 1)
    land_use_area            VARCHAR(100),                   -- 구역
    main_structure           VARCHAR(50),                    -- 주구조 (예: 철근콘크리트)
    main_usage_text          VARCHAR(200),                   -- 주용도 (대장 표제부 문구 원문 — 층별현황과 불일치할 수 있음에 유의)
    floors_below_ground      SMALLINT DEFAULT 0,
    floors_above_ground      SMALLINT DEFAULT 0,
    has_rooftop_floor         BOOLEAN DEFAULT FALSE,
    approval_date             DATE,                           -- 사용승인일
    permit_date               DATE,                           -- 허가일
    is_violating_building     BOOLEAN DEFAULT FALSE,          -- 현재 위반건축물 표기 여부
    had_past_violation        BOOLEAN DEFAULT FALSE,          -- 과거 위반→해제 이력 존재 여부 (변동사항에서 파생)
    sewage_facility_type      VARCHAR(50),                    -- 하수처리시설 형식 (대장에 공란인 경우 많음 → NULL 허용)
    sewage_facility_capacity_m3 NUMERIC(10,2),                -- 하수처리시설 용량(㎥)
    parking_indoor_count       SMALLINT,
    parking_outdoor_count      SMALLINT,
    parking_nearby_count       SMALLINT,
    geom                       geometry(Point, 4326),          -- 좌표 (카카오 지오코딩 결과)
    raw_registry_json          JSONB,                          -- 원본 API 응답 보관 (재판정/디버깅용)
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_buildings_geom ON buildings USING GIST (geom);
CREATE INDEX idx_buildings_parcel ON buildings (parcel_number);

COMMENT ON COLUMN buildings.main_usage_text IS
  '대장 표제부 주용도 필드는 층별현황 갱신 시 동기화가 안 되는 경우가 있음(실사례 확인됨). '
  '판정 로직은 이 컬럼이 아니라 building_floors의 실제 층별 용도를 기준으로 삼아야 한다.';

-- =========================================================
-- 2. 층별 건축물현황 (건축물대장 을 - 건축물현황)
-- =========================================================
CREATE TABLE building_floors (
    id              BIGSERIAL PRIMARY KEY,
    building_id     BIGINT NOT NULL REFERENCES buildings(id) ON DELETE CASCADE,
    building_group  VARCHAR(10)  NOT NULL,   -- 구분 (예: 주1)
    floor_label     VARCHAR(20)  NOT NULL,   -- 층별 (예: 지1, 1층, 2층, 옥탑1층)
    floor_order     SMALLINT NOT NULL,       -- 정렬/비교용 (지하는 음수, 옥탑은 지상층수+1 이상으로 부여)
    structure_type  VARCHAR(50),             -- 구조 (예: 철근콘크리트조)
    usage_text      VARCHAR(100) NOT NULL,   -- 용도 (예: 단독주택, 위락시설, 근린생활시설)
    area_sqm        NUMERIC(10,2) NOT NULL,  -- 면적(㎡)
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_building_floors_building ON building_floors (building_id);

-- =========================================================
-- 3. 변동사항 이력 (위반건축물 이력 등 REVIEW 판정의 근거)
-- =========================================================
CREATE TABLE building_change_history (
    id              BIGSERIAL PRIMARY KEY,
    building_id     BIGINT NOT NULL REFERENCES buildings(id) ON DELETE CASCADE,
    changed_at      DATE NOT NULL,
    description     TEXT NOT NULL,           -- 변동내용 및 원인 원문
    is_violation_related BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_change_history_building ON building_change_history (building_id);

-- =========================================================
-- 4. 토지이용계획 조회 결과 (2단계: 토지이음/공공API)
-- =========================================================
CREATE TABLE landuse_lookups (
    id                    BIGSERIAL PRIMARY KEY,
    building_id            BIGINT NOT NULL REFERENCES buildings(id) ON DELETE CASCADE,
    zone_detail             VARCHAR(200),      -- 용도지역/지구/구역 전체 텍스트
    lodging_restriction     VARCHAR(20),       -- 가능/제한/불가
    lodging_restriction_basis TEXT,            -- 근거 문구 원문
    district_plan_exists     BOOLEAN,
    district_plan_name       VARCHAR(200),
    district_plan_allows_lodging BOOLEAN,
    notes                    TEXT,
    source                   VARCHAR(30) NOT NULL DEFAULT 'PUBLIC_API', -- PUBLIC_API | MANUAL_ASTRA_INPUT
    queried_at                TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_landuse_lookups_building ON landuse_lookups (building_id);

-- =========================================================
-- 5. 판정 요청 및 결과
-- =========================================================
CREATE TABLE screening_requests (
    id                  BIGSERIAL PRIMARY KEY,
    building_id          BIGINT NOT NULL REFERENCES buildings(id),
    target_floor_labels   TEXT[] NOT NULL,     -- 전환 대상 층 (예: {'2층','3층'})
    target_area_sqm        NUMERIC(10,2) NOT NULL, -- 전환 대상 합계 면적
    has_kitchen_facility    BOOLEAN NOT NULL DEFAULT TRUE, -- 오수 계수 선택 (호스텔 기본값 취사시설 있음)
    requested_by            VARCHAR(100),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE screening_results (
    id                    BIGSERIAL PRIMARY KEY,
    screening_request_id   BIGINT NOT NULL REFERENCES screening_requests(id) ON DELETE CASCADE,
    overall_verdict          VARCHAR(20) NOT NULL,  -- PENDING_LANDUSE | CONDITIONAL_OK | NEEDS_REVIEW | DIFFICULT
    summary                   TEXT,                   -- 핵심 사유 요약 (3줄 이내 권장)
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 8개 판정 항목 (EAV 구조 — 스킬의 판정 항목이 늘어나도 스키마 불변)
CREATE TABLE screening_items (
    id                    BIGSERIAL PRIMARY KEY,
    screening_result_id    BIGINT NOT NULL REFERENCES screening_results(id) ON DELETE CASCADE,
    item_code               VARCHAR(30) NOT NULL,   -- LAND_USE_ZONE | VIOLATION | FIRE_DISTRICT | CURRENT_USE |
                                                      -- DIRECT_STAIR | FIRE_SAFETY | PARKING | SEWAGE
    status                  VARCHAR(10) NOT NULL,   -- OK | REQUIRED | REVIEW | NG
    legal_basis              TEXT,                   -- 근거 법령
    required_action           TEXT,                   -- 필요 조치
    computed_value_json        JSONB,                  -- 계산값 보관 (예: 오수인원 N, 필요주차대수 등 — 추정치 표시 포함)
    display_order              SMALLINT NOT NULL
);

CREATE INDEX idx_screening_items_result ON screening_items (screening_result_id);
CREATE UNIQUE INDEX uq_screening_item_code ON screening_items (screening_result_id, item_code);

-- =========================================================
-- 6. 경쟁 숙박업소 (인허가 데이터 배치 적재 대상)
-- =========================================================
CREATE TABLE competitor_lodgings (
    id                BIGSERIAL PRIMARY KEY,
    business_name      VARCHAR(200) NOT NULL,
    category            VARCHAR(30) NOT NULL,  -- PENSION | MOTEL | GUESTHOUSE | LIVING_LODGING | HOTEL | HOSTEL | HANOK
    permit_date          DATE,
    address               VARCHAR(200),
    geom                  geometry(Point, 4326) NOT NULL,
    data_source            VARCHAR(30) NOT NULL,  -- 지방행정인허가데이터 등
    ingested_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_competitor_geom ON competitor_lodgings USING GIST (geom);
CREATE INDEX idx_competitor_category ON competitor_lodgings (category);

-- =========================================================
-- 7. 갱신 트리거 (updated_at 자동 갱신)
-- =========================================================
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_buildings_updated_at
    BEFORE UPDATE ON buildings
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
