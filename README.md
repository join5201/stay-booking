# stay-booking

최초 작성: 2026-09-08
최종 갱신: 2026-09-23 (저장소 소개를 채웠다. 소개, 기능, 범위, 문서, 폴더. 이슈 195)

O2O 숙박 예약 서비스의 설계 문서와 구현을 담은 저장소다. 호스트가 숙소와 객실, 날짜별 재고와 요금을 올리고, 게스트가 숙소를 검색해 예약하고 결제하며, 운영자가 프로모션을 만든다. 도메인 주도 설계(DDD)로 설계 문서를 쓰고 Spring Boot 백엔드와 Next.js 화면으로 구현했으며, 로컬 개발과 검증까지 다룬다.

## 기능

| 역할 | 할 수 있는 일 | 명세 |
|---|---|---|
| 공개 | 숙소와 객실 조회, 숙소 검색, 객실 가용성과 예상 금액 조회, 적용 가능한 프로모션 조회 | [숙소](document/11-o2o-api-spec.md#숙소), [객실 타입](document/11-o2o-api-spec.md#객실-타입), [검색](document/11-o2o-api-spec.md#검색), [프로모션](document/11-o2o-api-spec.md#프로모션) |
| 호스트 | 본인 숙소와 객실 타입의 등록과 수정, 날짜별 재고와 요금 관리 | [숙소](document/11-o2o-api-spec.md#숙소), [객실 타입](document/11-o2o-api-spec.md#객실-타입), [재고](document/11-o2o-api-spec.md#재고), [요금](document/11-o2o-api-spec.md#요금) |
| 운영자 | 프로모션 등록, 수정, 관리 목록 조회 | [프로모션](document/11-o2o-api-spec.md#프로모션) |
| 게스트 | 본인 예약의 요청과 조회, Mock 결제 요청, 확정 예약 취소와 전액 Mock 환불 | [예약과 결제](document/11-o2o-api-spec.md#예약과-결제) |
| 시스템 | Mock 결제 결과 전달, 결제 승인 시 예약 확정, 선점 시한이 지나거나 결제가 세 번 실패한 예약의 만료, 만료 뒤 도착한 승인의 환불 | [내부 처리와 Mock 이벤트](document/11-o2o-api-spec.md#내부-처리와-mock-이벤트) |

## 범위

| 항목 | 들어 있는 것 | 없는 것 |
|---|---|---|
| 실행 환경 | 로컬 개발과 검증 | 배포와 운영 준비 |
| 인증 | 개발 프로파일에서 요청 헤더 `X-Dev-Actor-Id`로 역할과 소유자를 정한다 | 회원가입, 로그인, 토큰 |
| 결제 | Mock 결제 요청, Mock 결과 전달, Mock 환불 | 실제 결제 대행 연동 |
| 서비스 형태 | 날짜별 객실 재고를 선점하는 숙박 예약 | 배차, 배달, 채팅, 정산, 광고 |
| 검증 | 백엔드, 화면, E2E 자동 테스트 | 부하 측정 |

현재 상태(2026-09-23 기준): 명세의 서비스 API 32개와 로컬 Mock API 1개, 화면 열네 개(게스트 일곱, 호스트 다섯, 운영자 둘)가 main에 있다. 백엔드의 기능별 진행 표는 [backend/README.md](backend/README.md)에 있다.

## 문서

| 문서 | 무엇 |
|---|---|
| [설계 요약](document/06-6-o2o-design-digest.md) | 설계 전체의 요약. 컨텍스트 경계, 애그리거트, 책임 분담(CRC), 계약(DbC) |
| [API 명세](document/11-o2o-api-spec.md) | 서비스 API 32개와 로컬 Mock API 1개의 경로, 요청과 응답, 오류 코드, 처리 규칙 |
| [용어 사전](document/05-3-o2o-glossary.md) | 컨텍스트별 용어 정의 |
| [컨텍스트 맵](document/06-1-o2o-context-map.md) | 컨텍스트 사이의 관계 |
| [애그리거트](document/06-2-o2o-aggregates.md) | 애그리거트 일곱과 각각이 지키는 불변식. 근거는 [설명본](document/06-2-o2o-aggregates-explained.md) |
| [계약과 정책](document/06-4-o2o-contracts.md) | 도메인 규칙과 커맨드 계약(DbC), 정책. 근거는 [설명본](document/06-4-o2o-contracts-explained.md) |
| [설계 문서 전체 목록](harness/README.md#설계-문서) | 계획, 기능 목록, 이벤트 스토밍부터 검토 기록까지 |

## 폴더

| 폴더 | 무엇 |
|---|---|
| [document/](document/README.md) | 설계 문서, API 명세, 검토 기록 |
| [backend/](backend/README.md) | Spring Boot 백엔드와 테스트, 테스트용 MySQL 컨테이너 설정 |
| [frontend/](frontend/README.md) | Next.js 화면과 테스트 |
| [harness/](harness/README.md) | AI 개발 절차의 양식, 평가 기준, 검사 스크립트, 작업 기록과 그 근거 문서 |
| [.claude/](.claude) | Claude Code 권한 규칙과 훅 설정, 절차 스킬 넷 |
| [.github/](.github) | 이슈 양식 셋과 PR 양식 |
