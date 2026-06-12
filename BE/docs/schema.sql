-- =====================================================================
-- 관통 여행 — DB 스키마 스냅샷 (enjoytrip / 현재 DB명: trip_chat)
-- ddl-auto: update 환경의 참조용 단일 소스. 엔티티 변경 시 이 파일도 갱신할 것.
-- 발표 전 ddl-auto: validate 전환 시 이 스냅샷과 실 DB를 대조한다.
-- =====================================================================

-- ── 기존: users (com.trip.user.entity.User) ──
CREATE TABLE users (
  id                BIGINT PRIMARY KEY AUTO_INCREMENT,
  name              VARCHAR(50) NULL,
  gender            VARCHAR(255) NULL,            -- Gender enum (STRING)
  birth_date        DATE NULL,
  nickname          VARCHAR(20) NOT NULL,
  email             VARCHAR(50) NOT NULL UNIQUE,  -- M0에서 UNIQUE 추가
  password          VARCHAR(60) NOT NULL,
  user_role         VARCHAR(255) NOT NULL,        -- ADMIN/VIP/USER
  profile_image_url VARCHAR(255) NOT NULL,
  status            BIT NOT NULL,
  inactive_date     DATETIME NULL,
  created_at        DATETIME NULL,
  updated_at        DATETIME NULL
);

-- ── 기존: festivals (com.trip.festival.entity.Festival) ──
CREATE TABLE festivals (
  content_id   VARCHAR(255) PRIMARY KEY,
  title        VARCHAR(255) NOT NULL,
  address      VARCHAR(255) NULL,
  tel          VARCHAR(255) NULL,
  image_url    VARCHAR(500) NULL,
  start_date   DATE NULL,
  end_date     DATE NULL,
  latitude     DOUBLE NULL,
  longitude    DOUBLE NULL,
  area_code    VARCHAR(255) NULL,
  sigungu_code VARCHAR(255) NULL,
  status       VARCHAR(255) NULL,                 -- FestivalStatus enum
  synced_at    DATETIME NULL
);

-- ── 기존: chat_room / chat_room_membership (JPA), chat_messages는 MongoDB ──
-- (com.trip.chat.entity 참조 — MVP 범위 외)

-- ── 기존: user_analysis_data (com.trip.preprocessing — feat/datacollection) ──
-- (UserAnalysisData 엔티티 참조)

-- ── Spring Batch 메타 테이블: initialize-schema: always 로 자동 생성 ──

-- =====================================================================
-- 신규 (설계 v0.3 — 각 마일스톤에서 엔티티 구현 시 생성됨)
-- =====================================================================

-- M1: 관광지 스냅샷 (검색은 TourAPI 실시간 프록시, 계획에 담긴 장소만 영속화)
CREATE TABLE attractions (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  content_id    VARCHAR(20) NOT NULL,
  content_type  INT NOT NULL,
  title         VARCHAR(200) NOT NULL,
  addr          VARCHAR(300) NULL,
  area_code     VARCHAR(10) NULL,
  sigungu_code  VARCHAR(10) NULL,
  latitude      DOUBLE NULL,
  longitude     DOUBLE NULL,
  image_url     VARCHAR(500) NULL,
  tel           VARCHAR(50) NULL,
  overview      TEXT NULL,
  fetched_at    DATETIME NOT NULL,
  UNIQUE KEY uk_attr_content (content_id, content_type),
  KEY idx_attractions_area (area_code, content_type)
);

-- M3: 여행 계획 (TripPlan → TripDay → TripPlace, cascade=ALL + orphanRemoval)
CREATE TABLE trip_plans (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT NOT NULL,
  title       VARCHAR(100) NOT NULL,
  start_date  DATE NOT NULL,
  end_date    DATE NOT NULL,
  companions  VARCHAR(20) NULL,                   -- SOLO/COUPLE/FAMILY/FRIENDS
  budget      INT NULL,
  origin      VARCHAR(10) NOT NULL DEFAULT 'MANUAL',
  version     BIGINT NOT NULL DEFAULT 0,          -- @Version: plan 하위 모든 변경 시 증가
  created_at  DATETIME NULL,
  updated_at  DATETIME NULL,
  KEY idx_plans_user (user_id, updated_at DESC),
  CONSTRAINT fk_plan_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE trip_days (
  id      BIGINT PRIMARY KEY AUTO_INCREMENT,
  plan_id BIGINT NOT NULL,
  day_no  INT NOT NULL,
  memo    VARCHAR(300) NULL,
  UNIQUE KEY uk_day (plan_id, day_no),
  CONSTRAINT fk_day_plan FOREIGN KEY (plan_id) REFERENCES trip_plans(id) ON DELETE CASCADE
);

CREATE TABLE trip_places (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  day_id        BIGINT NOT NULL,
  attraction_id BIGINT NOT NULL,
  seq           INT NOT NULL,
  visit_time    TIME NULL,
  memo          VARCHAR(300) NULL,
  UNIQUE KEY uk_place_seq (day_id, seq),
  UNIQUE KEY uk_place_attr (day_id, attraction_id),  -- 같은 일자 내 같은 장소 중복 금지
  CONSTRAINT fk_place_day FOREIGN KEY (day_id) REFERENCES trip_days(id) ON DELETE CASCADE,
  CONSTRAINT fk_place_attr FOREIGN KEY (attraction_id) REFERENCES attractions(id)
);

-- M4: AI 추천 이력 (실패 이력 포함, save-plan 멱등)
CREATE TABLE recommendations (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id       BIGINT NOT NULL,
  request_json  JSON NOT NULL,
  request_hash  VARCHAR(64) NOT NULL,
  result_json   JSON NULL,
  model         VARCHAR(50) NOT NULL,
  status        VARCHAR(10) NOT NULL,              -- SUCCESS/PARTIAL/FAILED
  saved_plan_id BIGINT NULL,                       -- 1추천 1계획 멱등
  error_code    VARCHAR(30) NULL,
  error_message VARCHAR(300) NULL,
  latency_ms    INT NULL,
  created_at    DATETIME NULL,
  KEY idx_reco_user (user_id, created_at DESC),
  CONSTRAINT fk_reco_user FOREIGN KEY (user_id) REFERENCES users(id)
);
