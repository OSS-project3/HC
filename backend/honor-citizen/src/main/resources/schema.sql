CREATE SEQUENCE IF NOT EXISTS application_seq START WITH 1 INCREMENT BY 1;

-- 4-D: 학생증 CardDesign.designNumber 채번용. MAX(designNumber)+1은 서로 다른 두 학교가 동시에
-- 신규 등록하면 경쟁이 생길 수 있어(unique 제약 위반) 시퀀스로 대체한다(application_seq와 같은 이유).
-- STUDENT 외 카드종류는 이 시퀀스를 안 타므로(비STUDENT designNumber는 여전히 시더가 고정 배정) 영향 없음.
CREATE SEQUENCE IF NOT EXISTS student_card_design_seq START WITH 1 INCREMENT BY 1;

-- 4-D: "같은 schoolId+orientation의 활성 디자인은 1개만" 불변조건의 DB 레벨 최종 방어선. 정상 경로는
-- SchoolCardTemplateService의 조회-후-UPSERT 분기가 지키지만, 이 인덱스는 그 방어가 뚫렸을 때(동시
-- 요청 등)를 대비한 것이다.
--
-- ⚠️ 정정(2026-09-01, 구현 중 발견): 원래 partial index(WHERE active = true)로 설계했으나 테스트에
-- 쓰는 H2 2.4.240이 CREATE INDEX ... WHERE 구문 자체를 지원하지 않는다(실측 확인 — Postgres 전용
-- 기능). 그래서 조건 없는 일반 UNIQUE 인덱스로 바꿨다. school_id가 null인 비STUDENT 디자인끼리는
-- SQL 표준상 UNIQUE 제약에서 NULL을 서로 다른 값으로 취급하므로(H2·Postgres 둘 다 이 동작) 여전히
-- 안 걸린다 — 문제는 "비활성(deactivate) STUDENT 디자인 + 재등록"처럼 같은 (card_type_id, school_id,
-- orientation)에 비활성 row가 남아있는 채로 새 활성 row를 만드는 경우인데, 지금 코드베이스엔 STUDENT
-- CardDesign을 비활성화하는 경로 자체가 없다(deactivate() 호출부는 CardDesignSeeder의 비STUDENT
-- 시딩뿐, grep으로 확인). 그런 경로가 생기면 이 인덱스도 partial 버전으로 다시 바꿔야 한다.
CREATE UNIQUE INDEX IF NOT EXISTS card_designs_school_orientation_idx
    ON card_designs (card_type_id, school_id, orientation);
