# O2O 숙박 예약 API 명세서

버전: v2  
기준일: 2026-09-07  
상태: 구현용 초안

Spring 기반 Java 백엔드, Next.js 프론트, MySQL을 사용하는 로컬 개발용 HTTP 계약이다. 판매 준비, 프로모션, 검색, 예약과 Hold 관리를 포함한다. 배포와 운영 준비는 제외한다.

서비스 API 32개와 로컬 Mock API 1개를 정의한다. 모든 요청과 응답 모델, 처리 규칙과 검증 기준은 이 문서 안에 있다. TTL과 할인 및 취소 정책의 제안값은 마지막 절에 모았다.

## 목차

1. [공통](#common)
2. [숙소](#properties)
3. [객실 타입](#room-types)
4. [재고](#inventories)
5. [요금](#rates)
6. [프로모션](#promotions)
7. [검색](#search)
8. [예약과 결제](#bookings)
9. [내부 처리와 Mock 이벤트](#internal)
10. [응답 모델](#models)
11. [검증 기준](#verification)
12. [검토할 정책](#proposals)

<a id="common"></a>

## 공통

### 요청과 응답 규칙

- 서비스 API 기본 경로는 /api/v1이다. 모든 엔드포인트 제목에 전체 경로를 표시한다.
- 요청과 응답은 application/json이다. 성공 응답은 별도 data 포장 없이 명세의 객체를 반환한다.
- ID는 최대 64자의 비어 있지 않은 문자열이다. 클라이언트는 숫자 연산이나 내부 구조 해석을 하지 않는다.
- 날짜는 YYYY-MM-DD이고 숙박과 캠페인 날짜는 Asia/Seoul을 기준으로 한다. 시각은 UTC의 YYYY-MM-DDTHH:mm:ss.SSSZ 형식이다.
- 금액은 소수 없는 JSON 정수이며 currency는 KRW다. 일일 단가는 1 이상 1,000,000,000 이하로 제한한다.
- 수량은 0 이상 100,000 이하의 정수다. maxOccupancy와 guestCount는 1 이상 100 이하의 정수다.
- JSON 필드명은 camelCase다. 요청의 정의되지 않은 필드, 잘못된 타입, 허용하지 않은 null은 400이다.
- 아래 스키마의 필드는 별도 선택 또는 nullable 표시가 없으면 필수다. 응답의 nullable 필드는 값이 없어도 null로 보낸다.
- POST 등록은 201과 Location 헤더를 반환한다. PATCH 수정과 GET은 200이다. 결제 시도 접수는 202다.
- PATCH는 전체 교체가 아니다. version과 한 개 이상의 변경 필드가 필요하다. ID, 소유자와 생성 시각은 수정할 수 없다.
- 수정 요청의 version은 마지막 조회에서 받은 값이다. 불일치하면 409 VERSION_CONFLICT이며 적용하지 않는다.
- version은 변경마다 증가하는 0 이상의 정수다. 재고의 Hold, 확정, 반환도 해당 재고 version을 증가시킨다.

### 인증과 접근 제어

| 주체 | 사용 범위 |
|---|---|
| 공개 | 숙소와 객실 조회, 검색, 가용성, 예상 금액, 적용 가능 프로모션 조회 |
| HOST | 본인 숙소와 객실의 등록과 수정, 날짜별 재고와 요금 관리 |
| OPERATOR | 프로모션 등록, 수정, 관리 조회 |
| GUEST | 본인 예약 생성, 조회, 결제 요청과 취소 |
| MOCK_SYSTEM | 로컬 Mock 결제 결과 전달. 서비스 API에서 사용하지 않는다 |

인증이 필요한 로컬 API는 X-Dev-Actor-Id로 개발 행위자를 전달한다. 예시 fixture는 host_001, operator_001, guest_001, mock_001이며 서버가 ID를 역할에 매핑한다. 공개 API는 헤더 없이 호출할 수 있다. 예시는 해당 fixture와 SEOUL 지역 코드가 준비된 환경을 전제로 한다.

요청 body의 guestId, hostId, role로 권한을 결정하지 않는다. 예약 guestId와 숙소 hostId는 서버의 행위자 정보에서 채운다. 헤더 없음 또는 미등록 ID는 401, 역할 불일치는 403, 다른 사용자 소유 자원은 404다. 중첩 경로의 부모와 자식 관계도 확인한다.

X-Dev-Actor-Id는 로컬 역할과 소유권 테스트용이며 신원 인증을 보장하지 않는다. 개발 프로파일 밖에서는 이 어댑터와 /internal 경로를 비활성화한다. AccessToken, RefreshToken, 회원가입과 로그인 API는 이번 범위에 없다.

### 공통 헤더

| 구분 | 헤더 | 필수 | 용도 |
|---|---|---|---|
| 요청 | Content-Type | Body가 있으면 O | application/json |
| 요청 | Accept | X | application/json |
| 요청 | X-Dev-Actor-Id | 인증 필요 API에서 O | 서버 fixture에 등록한 행위자 ID |
| 요청 | Idempotency-Key | 예약 생성, 결제 요청, 취소에서 O | 재전송 중복 방지 |
| 응답 | Content-Type | O | application/json |
| 응답 | Location | 201과 202에서 O | 생성 자원 또는 결과 조회 경로 |
| 응답 | Idempotency-Replayed | 성공 재전송에서 O | true |
| 응답 | Retry-After | REQUEST_IN_PROGRESS에서 O | 1초 |

JSON Body가 모두 선택 필드인 POST도 {}를 보낸다. GET은 Body를 받지 않는다. API에 표시하지 않은 Path, Query, Body 필드는 지원하지 않는다. 모든 응답 필드는 반환하며, /null 타입만 null을 허용한다.

### 목록과 날짜 범위

목록 요청은 page 기본 0, size 기본 20과 최대 100을 사용한다. 잘못된 범위는 400이다.

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

스키마 표의 `Page<T>`는 위 구조의 items가 T 배열이라는 뜻이다. 기본 정렬은 id 오름차순이다. 예약 목록은 createdAt 내림차순, 동률이면 id 내림차순이다. 검색도 숙소 id 오름차순으로 페이지를 나눈다. 페이지 범위를 넘으면 빈 items를 반환한다.

날짜별 관리 조회는 from 이상, to 미만이며 from < to가 필수다. 결과는 date 오름차순이다. 없는 날짜를 자동 생성하거나 0으로 대체하지 않는다. 조회 응답에 missingDates를 포함한다.

### 멱등 처리

Idempotency-Key는 예약 생성, 결제 시도, 취소에서 필수다. 8자 이상 128자 이하의 영문, 숫자, 하이픈과 밑줄을 허용한다.

1. 범위는 행위자 ID + HTTP 메서드 + 실제 자원 경로 + 키다. 서로 다른 예약의 결제 요청은 범위가 다르다.
2. body는 JSON 키 순서와 무관하게 비교한다. 같은 범위와 키인데 body가 다르면 409 IDEMPOTENCY_KEY_REUSED다.
3. 같은 요청이 완료됐다면 처음 저장한 성공 상태 코드, Location과 body를 재전송한다. Idempotency-Replayed: true를 붙인다.
4. 최초 요청이 처리 중이면 409 REQUEST_IN_PROGRESS와 Retry-After: 1을 반환한다. 새 작업을 시작하지 않는다.
5. 결제의 명시적인 다음 시도는 새 키를 사용한다. 네트워크 재전송에는 원래 키를 사용한다.
6. 멱등 기록과 도메인 변경은 함께 저장한다. 서버 오류로 성공 여부가 불명확하면 같은 키로 재시도한다.
7. body 검증이나 원자적 처리에서 거절돼 변경이 없는 4xx와 롤백된 5xx는 완료 응답으로 캐시하지 않는다. 진행 중 임시 기록은 변경이 없음을 확인한 뒤 해제한다.
8. 로컬 v1에서는 완료 기록을 자동 만료시키지 않는다. 성공 이후 다른 body로 키를 재사용할 수 없다.
9. 인증과 소유권 검사는 재전송에도 적용한다. 성공한 요청의 재전송 판정은 현재 상태의 전이 가능성 검사보다 먼저 한다.

만료된 HELD에 대한 결제 요청은 만료와 재고 반환을 저장한 뒤 409를 반환할 수 있다. 이 경우 결제 시도를 만들지 않으며 오류 발생을 이유로 이미 완료한 만료 처리를 롤백하지 않는다.

재전송 body는 최초 시점의 스냅샷이다. 예약이 이후 만료돼도 예약 생성 재전송은 최초 HELD 응답을 반환할 수 있다. 화면의 현재 상태는 반드시 GET 예약 상세로 갱신한다.

### 에러 응답

```json
{
  "code": "INVENTORY_UNAVAILABLE",
  "message": "선택한 기간에 예약 가능한 객실이 없습니다.",
  "traceId": "trace_001",
  "details": [{"field": "checkIn", "reason": "2026-10-11 재고 부족"}]
}
```

Error는 code:string, message:string, traceId:string, details:ErrorDetail[]이다. ErrorDetail은 field:string, reason:string이다. 필드가 특정되지 않으면 field는 빈 문자열이다. 프론트 분기는 message가 아닌 code를 사용한다. details가 없으면 빈 배열이다.

| HTTP | code | 의미 |
|---|---|---|
| 400 | INVALID_REQUEST | 타입, 필수값, 범위 또는 허용하지 않은 필드 오류 |
| 400 | INVALID_DATE_RANGE | 날짜 형식, 순서, 박수 제한 오류 |
| 400 | IDEMPOTENCY_KEY_REQUIRED | 필수 멱등키 누락 또는 형식 오류 |
| 401 | ACTOR_REQUIRED | 필요한 개발 행위자 없음 또는 미등록 |
| 403 | ACCESS_DENIED | 역할 불일치 |
| 404 | RESOURCE_NOT_FOUND | 자원 없음, 부모 불일치 또는 본인 소유 아님 |
| 409 | VERSION_CONFLICT | 수정 버전 불일치 |
| 409 | RESOURCE_ALREADY_EXISTS | 같은 객실 타입과 날짜의 재고 또는 요금 중복 등록 |
| 409 | INVENTORY_BELOW_COMMITTED | 총 재고를 heldCount + soldCount 아래로 수정 |
| 409 | INVENTORY_UNAVAILABLE | 하나 이상의 숙박 날짜에 선점 가능한 재고 없음 |
| 409 | INVENTORY_NOT_CONFIGURED | 하나 이상의 숙박 날짜에 재고 레코드 없음 |
| 409 | RATE_NOT_CONFIGURED | 하나 이상의 숙박 날짜에 요금 없음 |
| 409 | OCCUPANCY_EXCEEDED | 객실 최대 인원 초과 |
| 409 | PRICE_CHANGED | 예약 요청의 예상 총액과 서버 금액 불일치 |
| 409 | IDEMPOTENCY_KEY_REUSED | 같은 멱등키에 다른 body |
| 409 | REQUEST_IN_PROGRESS | 동일 멱등 요청 처리 중 |
| 409 | BOOKING_STATE_CONFLICT | 현재 예약 상태에서 허용되지 않은 동작 |
| 409 | BOOKING_EXPIRED | 결제 요청 시 TTL 만료 또는 이미 EXPIRED |
| 409 | PAYMENT_IN_PROGRESS | 완료되지 않은 결제 시도 존재 |
| 409 | PAYMENT_ATTEMPTS_EXHAUSTED | 결제 시도 한도 도달 |
| 409 | CANCELLATION_NOT_ALLOWED | 제안한 취소 가능 날짜를 지남 |
| 409 | MOCK_EVENT_CONFLICT | 처리된 거래의 다른 결과 또는 같은 이벤트 ID의 다른 내용 |
| 409 | PAYMENT_AMOUNT_MISMATCH | Mock 거래 결과의 금액 또는 통화 불일치 |
| 503 | TEMPORARY_FAILURE | 일시적 DB 실패 등으로 처리 완료를 보장할 수 없음 |
| 500 | INTERNAL_ERROR | 예상하지 못한 오류. 내부 예외 내용은 응답에 넣지 않음 |

모든 API에는 입력에 따른 400, 자원에 따른 404와 서버 오류가 공통 적용된다. 인증이 필요한 API에는 401과 403이 추가된다. API별 Error Responses에는 기능별 오류를 적는다. 공통 오류 형식과 상태 코드는 모든 API에 적용한다.

<a id="properties"></a>

## 숙소

<a id="cat-01"></a>

### POST /api/v1/properties : 숙소 등록

API ID: `CAT-01`  
인증: HOST

**Request Body**

```json
{
  "name": "서울 스테이",
  "regionCode": "SEOUL",
  "address": "서울특별시 종로구 예시로 10",
  "description": "숙박용 예시 숙소"
}
```

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `name` | string | O | 공백 제거 후 1~100자 |
| `regionCode` | string | O | 1~32자. 등록된 지역 코드 |
| `address` | string | O | 1~300자 |
| `description` | string | X | 최대 2,000자, 생략하면 빈 문자열 |

**Response `201 Created`**

```http
Location: /api/v1/properties/prop_001
```

응답 모델: [Property](#model-property)

```json
{
  "id": "prop_001",
  "hostId": "host_001",
  "name": "서울 스테이",
  "regionCode": "SEOUL",
  "address": "서울특별시 종로구 예시로 10",
  "description": "숙박용 예시 숙소",
  "version": 0,
  "createdAt": "2026-10-01T03:00:00.000Z",
  "updatedAt": "2026-10-01T03:00:00.000Z"
}
```

**처리 규칙**

- hostId는 개발 행위자 정보에서 채운다. body로 소유자를 받지 않는다.
- 등록된 지역 코드를 확인하고 저장한다. 지역 fixture와 프론트 선택 목록은 초기 세팅에서 준비한다.

검증 항목: [T01](#t01)

<a id="cat-02"></a>

### PATCH /api/v1/properties/{propertyId} : 숙소 수정

API ID: `CAT-02`  
인증: HOST, 대상 숙소의 소유자

**Path Variables**

| 변수 | 타입 | 설명 |
|---|---|---|
| propertyId | string | 대상 ID, 최대 64자 |

**Request Body**

```json
{
  "version": 0,
  "name": "서울 스테이 본관"
}
```

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `version` | integer | O | 0 이상. 마지막 조회 버전과 일치해야 함 |
| `name` | string | X | 공백 제거 후 1~100자. 생략하면 기존 값 유지 |
| `regionCode` | string | X | 1~32자. 등록된 지역 코드. 생략하면 기존 값 유지 |
| `address` | string | X | 1~300자. 생략하면 기존 값 유지 |
| `description` | string | X | 최대 2,000자. 생략하면 기존 값 유지 |

version 외 변경 필드가 한 개 이상 필요하다. 생략한 필드는 유지하며 null 허용 필드만 null로 해제할 수 있다.

**Response `200 OK`**

응답 모델: [Property](#model-property)

```json
{
  "id": "prop_001",
  "hostId": "host_001",
  "name": "서울 스테이 본관",
  "regionCode": "SEOUL",
  "address": "서울특별시 종로구 예시로 10",
  "description": "숙박용 예시 숙소",
  "version": 1,
  "createdAt": "2026-10-01T03:00:00.000Z",
  "updatedAt": "2026-10-01T03:01:00.000Z"
}
```

**Error Responses**

| 상태 | code | 조건 |
|---|---|---|
| 409 | VERSION_CONFLICT | 수정 버전 불일치 |

**처리 규칙**

- version 외 변경 필드가 한 개 이상 필요하다.
- ID, hostId와 createdAt을 변경할 수 없다.

검증 항목: [T01](#t01), [T02](#t02)

<a id="cat-03"></a>

### GET /api/v1/properties/{propertyId} : 숙소 상세 조회

API ID: `CAT-03`  
인증: 불필요

**Path Variables**

| 변수 | 타입 | 설명 |
|---|---|---|
| propertyId | string | 대상 ID, 최대 64자 |

**Response `200 OK`**

응답 모델: [Property](#model-property)

```json
{
  "id": "prop_001",
  "hostId": "host_001",
  "name": "서울 스테이",
  "regionCode": "SEOUL",
  "address": "서울특별시 종로구 예시로 10",
  "description": "숙박용 예시 숙소",
  "version": 0,
  "createdAt": "2026-10-01T03:00:00.000Z",
  "updatedAt": "2026-10-01T03:00:00.000Z"
}
```

검증 항목: [T01](#t01)

<a id="cat-04"></a>

### GET /api/v1/properties : 숙소 목록 조회

API ID: `CAT-04`  
인증: 불필요

**Query Parameters**

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `regionCode` | string | X | 1~32자. 등록된 지역 코드 |
| `page` | integer | X | 0 이상, 기본 0 |
| `size` | integer | X | 1~100, 기본 20 |

**Response `200 OK`**

응답 모델: [Page&lt;Property&gt;](#model-page-property)

```json
{
  "items": [
    {
      "id": "prop_001",
      "hostId": "host_001",
      "name": "서울 스테이",
      "regionCode": "SEOUL",
      "address": "서울특별시 종로구 예시로 10",
      "description": "숙박용 예시 숙소",
      "version": 0,
      "createdAt": "2026-10-01T03:00:00.000Z",
      "updatedAt": "2026-10-01T03:00:00.000Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

검증 항목: [T01](#t01)

<a id="cat-05"></a>

### GET /api/v1/host/properties : 본인 숙소 목록 조회

API ID: `CAT-05`  
인증: HOST

**Query Parameters**

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `page` | integer | X | 0 이상, 기본 0 |
| `size` | integer | X | 1~100, 기본 20 |

**Response `200 OK`**

응답 모델: [Page&lt;Property&gt;](#model-page-property)

```json
{
  "items": [
    {
      "id": "prop_001",
      "hostId": "host_001",
      "name": "서울 스테이",
      "regionCode": "SEOUL",
      "address": "서울특별시 종로구 예시로 10",
      "description": "숙박용 예시 숙소",
      "version": 0,
      "createdAt": "2026-10-01T03:00:00.000Z",
      "updatedAt": "2026-10-01T03:00:00.000Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

**처리 규칙**

- 행위자의 hostId로 범위를 제한한다. hostId 쿼리는 받지 않는다.

검증 항목: [T01](#t01), [T02](#t02)

<a id="room-types"></a>

## 객실 타입

<a id="cat-06"></a>

### POST /api/v1/properties/{propertyId}/room-types : 객실 타입 등록

API ID: `CAT-06`  
인증: HOST, 대상 숙소의 소유자

**Path Variables**

| 변수 | 타입 | 설명 |
|---|---|---|
| propertyId | string | 대상 ID, 최대 64자 |

**Request Body**

```json
{
  "name": "스탠다드 더블",
  "maxOccupancy": 2,
  "description": "2인 객실"
}
```

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `name` | string | O | 공백 제거 후 1~100자 |
| `maxOccupancy` | integer | O | 1~100 |
| `description` | string | X | 최대 2,000자, 생략하면 빈 문자열 |

**Response `201 Created`**

```http
Location: /api/v1/room-types/room_001
```

응답 모델: [RoomType](#model-roomtype)

```json
{
  "id": "room_001",
  "propertyId": "prop_001",
  "name": "스탠다드 더블",
  "maxOccupancy": 2,
  "description": "2인 객실",
  "version": 0,
  "createdAt": "2026-10-01T03:00:00.000Z",
  "updatedAt": "2026-10-01T03:00:00.000Z"
}
```

**처리 규칙**

- 부모 숙소의 소유자를 검사한다. propertyId는 경로에서 가져온다.

검증 항목: [T01](#t01), [T02](#t02)

<a id="cat-07"></a>

### PATCH /api/v1/room-types/{roomTypeId} : 객실 타입 수정

API ID: `CAT-07`  
인증: HOST, 대상 숙소의 소유자

**Path Variables**

| 변수 | 타입 | 설명 |
|---|---|---|
| roomTypeId | string | 대상 ID, 최대 64자 |

**Request Body**

```json
{
  "version": 0,
  "name": "스탠다드 더블 A"
}
```

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `version` | integer | O | 0 이상. 마지막 조회 버전과 일치해야 함 |
| `name` | string | X | 공백 제거 후 1~100자. 생략하면 기존 값 유지 |
| `maxOccupancy` | integer | X | 1~100. 생략하면 기존 값 유지 |
| `description` | string | X | 최대 2,000자. 생략하면 기존 값 유지 |

version 외 변경 필드가 한 개 이상 필요하다. 생략한 필드는 유지하며 null 허용 필드만 null로 해제할 수 있다.

**Response `200 OK`**

응답 모델: [RoomType](#model-roomtype)

```json
{
  "id": "room_001",
  "propertyId": "prop_001",
  "name": "스탠다드 더블 A",
  "maxOccupancy": 2,
  "description": "2인 객실",
  "version": 1,
  "createdAt": "2026-10-01T03:00:00.000Z",
  "updatedAt": "2026-10-01T03:01:00.000Z"
}
```

**Error Responses**

| 상태 | code | 조건 |
|---|---|---|
| 409 | VERSION_CONFLICT | 수정 버전 불일치 |

**처리 규칙**

- propertyId를 변경하지 않는다.
- 최대 인원 수정은 신규 예약에 적용한다. 기존 예약의 guestCount는 바꾸지 않는다.

검증 항목: [T01](#t01), [T02](#t02)

<a id="cat-08"></a>

### GET /api/v1/room-types/{roomTypeId} : 객실 타입 상세 조회

API ID: `CAT-08`  
인증: 불필요

**Path Variables**

| 변수 | 타입 | 설명 |
|---|---|---|
| roomTypeId | string | 대상 ID, 최대 64자 |

**Response `200 OK`**

응답 모델: [RoomType](#model-roomtype)

```json
{
  "id": "room_001",
  "propertyId": "prop_001",
  "name": "스탠다드 더블",
  "maxOccupancy": 2,
  "description": "2인 객실",
  "version": 0,
  "createdAt": "2026-10-01T03:00:00.000Z",
  "updatedAt": "2026-10-01T03:00:00.000Z"
}
```

검증 항목: [T01](#t01)

<a id="cat-09"></a>

### GET /api/v1/properties/{propertyId}/room-types : 숙소의 객실 타입 목록 조회

API ID: `CAT-09`  
인증: 불필요

**Path Variables**

| 변수 | 타입 | 설명 |
|---|---|---|
| propertyId | string | 대상 ID, 최대 64자 |

**Query Parameters**

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `page` | integer | X | 0 이상, 기본 0 |
| `size` | integer | X | 1~100, 기본 20 |

**Response `200 OK`**

응답 모델: [Page&lt;RoomType&gt;](#model-page-roomtype)

```json
{
  "items": [
    {
      "id": "room_001",
      "propertyId": "prop_001",
      "name": "스탠다드 더블",
      "maxOccupancy": 2,
      "description": "2인 객실",
      "version": 0,
      "createdAt": "2026-10-01T03:00:00.000Z",
      "updatedAt": "2026-10-01T03:00:00.000Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

검증 항목: [T01](#t01)

<a id="inventories"></a>

## 재고

<a id="inv-01"></a>

### POST /api/v1/room-types/{roomTypeId}/inventories : 날짜별 재고 등록

API ID: `INV-01`  
인증: HOST, 대상 숙소의 소유자

**Path Variables**

| 변수 | 타입 | 설명 |
|---|---|---|
| roomTypeId | string | 대상 ID, 최대 64자 |

**Request Body**

```json
{
  "date": "2026-10-10",
  "totalCount": 5
}
```

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `date` | date | O | 등록일은 서버의 오늘 이상 |
| `totalCount` | integer | O | 0~100,000. 증감량이 아닌 총 수량 |

**Response `201 Created`**

```http
Location: /api/v1/room-types/room_001/inventories/2026-10-10
```

응답 모델: [DailyInventory](#model-dailyinventory)

```json
{
  "roomTypeId": "room_001",
  "date": "2026-10-10",
  "totalCount": 5,
  "heldCount": 0,
  "soldCount": 0,
  "availableCount": 5,
  "version": 0
}
```

**Error Responses**

| 상태 | code | 조건 |
|---|---|---|
| 409 | RESOURCE_ALREADY_EXISTS | 같은 객실 타입과 날짜의 재고 또는 요금 중복 등록 |

**처리 규칙**

- heldCount와 soldCount는 0으로 시작하며 입력받지 않는다.
- 객실 타입과 날짜의 조합이 이미 있으면 덮어쓰지 않는다.

검증 항목: [T01](#t01)

<a id="inv-02"></a>

### POST /api/v1/room-types/{roomTypeId}/inventories/bulk : 기간 재고 일괄 등록

API ID: `INV-02`  
인증: HOST, 대상 숙소의 소유자

**Path Variables**

| 변수 | 타입 | 설명 |
|---|---|---|
| roomTypeId | string | 대상 ID, 최대 64자 |

**Request Body**

```json
{
  "from": "2026-10-10",
  "to": "2026-10-12",
  "totalCount": 5
}
```

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `from` | date | O | 범위 시작일 포함 |
| `to` | date | O | from보다 뒤, 최대 366일, 끝 날짜 제외 |
| `totalCount` | integer | O | 0~100,000. 증감량이 아닌 총 수량 |

**Response `201 Created`**

```http
Location: /api/v1/room-types/room_001/inventories?from=2026-10-10&to=2026-10-12
```

응답 모델: [InventoryRange](#model-inventoryrange)

```json
{
  "roomTypeId": "room_001",
  "from": "2026-10-10",
  "to": "2026-10-12",
  "items": [
    {
      "roomTypeId": "room_001",
      "date": "2026-10-10",
      "totalCount": 5,
      "heldCount": 0,
      "soldCount": 0,
      "availableCount": 5,
      "version": 0
    },
    {
      "roomTypeId": "room_001",
      "date": "2026-10-11",
      "totalCount": 5,
      "heldCount": 0,
      "soldCount": 0,
      "availableCount": 5,
      "version": 0
    }
  ],
  "missingDates": []
}
```

**Error Responses**

| 상태 | code | 조건 |
|---|---|---|
| 409 | RESOURCE_ALREADY_EXISTS | 같은 객실 타입과 날짜의 재고 또는 요금 중복 등록 |

**처리 규칙**

- from은 서버의 오늘 이상이고, to는 제외한다. 최대 366일이다.
- 한 날짜라도 이미 존재하면 전부 실패한다. 새 날짜만 부분 등록하지 않는다.
- 성공 시 missingDates는 빈 배열이다. totalCount=0 등록도 허용한다.

검증 항목: [T03](#t03)

<a id="inv-03"></a>

### PATCH /api/v1/room-types/{roomTypeId}/inventories/{date} : 날짜별 재고 수정

API ID: `INV-03`  
인증: HOST, 대상 숙소의 소유자

**Path Variables**

| 변수 | 타입 | 설명 |
|---|---|---|
| roomTypeId | string | 대상 ID, 최대 64자 |
| date | date | YYYY-MM-DD. 서버의 오늘 이상 |

**Request Body**

```json
{
  "version": 0,
  "totalCount": 8
}
```

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `version` | integer | O | 0 이상. 마지막 조회 버전과 일치해야 함 |
| `totalCount` | integer | O | 0~100,000. 증감량이 아닌 총 수량 |

version 외 변경 필드가 한 개 이상 필요하다. 생략한 필드는 유지하며 null 허용 필드만 null로 해제할 수 있다.

**Response `200 OK`**

응답 모델: [DailyInventory](#model-dailyinventory)

```json
{
  "roomTypeId": "room_001",
  "date": "2026-10-10",
  "totalCount": 8,
  "heldCount": 0,
  "soldCount": 0,
  "availableCount": 8,
  "version": 1
}
```

**Error Responses**

| 상태 | code | 조건 |
|---|---|---|
| 409 | VERSION_CONFLICT | 수정 버전 불일치 |
| 409 | INVENTORY_BELOW_COMMITTED | 총 재고를 heldCount + soldCount 아래로 수정 |

**처리 규칙**

- 수정 날짜는 서버의 오늘 이상이다.
- totalCount >= heldCount + soldCount를 동시성 제어 안에서 검사한다.
- heldCount와 soldCount를 직접 수정하지 않는다.

검증 항목: [T04](#t04), [T05](#t05)

<a id="inv-04"></a>

### GET /api/v1/room-types/{roomTypeId}/inventories : 기간 재고 조회

API ID: `INV-04`  
인증: HOST, 대상 숙소의 소유자

**Path Variables**

| 변수 | 타입 | 설명 |
|---|---|---|
| roomTypeId | string | 대상 ID, 최대 64자 |

**Query Parameters**

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `from` | date | O | 범위 시작일 포함 |
| `to` | date | O | from보다 뒤, 최대 366일, 끝 날짜 제외 |

**Response `200 OK`**

응답 모델: [InventoryRange](#model-inventoryrange)

```json
{
  "roomTypeId": "room_001",
  "from": "2026-10-10",
  "to": "2026-10-12",
  "items": [
    {
      "roomTypeId": "room_001",
      "date": "2026-10-10",
      "totalCount": 5,
      "heldCount": 0,
      "soldCount": 0,
      "availableCount": 5,
      "version": 0
    },
    {
      "roomTypeId": "room_001",
      "date": "2026-10-11",
      "totalCount": 5,
      "heldCount": 0,
      "soldCount": 0,
      "availableCount": 5,
      "version": 0
    }
  ],
  "missingDates": []
}
```

**처리 규칙**

- 과거 날짜 조회를 허용한다. 없는 날짜는 items에 만들지 않고 missingDates에 적는다.

검증 항목: [T01](#t01), [T07](#t07)

<a id="inv-05"></a>

### GET /api/v1/room-types/{roomTypeId}/inventories/{date} : 날짜별 재고 조회

API ID: `INV-05`  
인증: HOST, 대상 숙소의 소유자

**Path Variables**

| 변수 | 타입 | 설명 |
|---|---|---|
| roomTypeId | string | 대상 ID, 최대 64자 |
| date | date | YYYY-MM-DD. 과거 조회 허용 |

**Response `200 OK`**

응답 모델: [DailyInventory](#model-dailyinventory)

```json
{
  "roomTypeId": "room_001",
  "date": "2026-10-10",
  "totalCount": 5,
  "heldCount": 0,
  "soldCount": 0,
  "availableCount": 5,
  "version": 0
}
```

**처리 규칙**

- 과거 날짜 조회를 허용한다. 해당 날짜 레코드가 없으면 404다.

검증 항목: [T01](#t01)

<a id="rates"></a>

## 요금

<a id="rate-01"></a>

### POST /api/v1/room-types/{roomTypeId}/rates : 날짜별 요금 등록

API ID: `RATE-01`  
인증: HOST, 대상 숙소의 소유자

**Path Variables**

| 변수 | 타입 | 설명 |
|---|---|---|
| roomTypeId | string | 대상 ID, 최대 64자 |

**Request Body**

```json
{
  "date": "2026-10-10",
  "amount": 100000,
  "currency": "KRW"
}
```

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `date` | date | O | 등록일은 서버의 오늘 이상 |
| `amount` | integer | O | 1~1,000,000,000원 |
| `currency` | string | O | KRW 고정 |

**Response `201 Created`**

```http
Location: /api/v1/room-types/room_001/rates/2026-10-10
```

응답 모델: [DailyRate](#model-dailyrate)

```json
{
  "roomTypeId": "room_001",
  "date": "2026-10-10",
  "amount": 100000,
  "currency": "KRW",
  "version": 0
}
```

**Error Responses**

| 상태 | code | 조건 |
|---|---|---|
| 409 | RESOURCE_ALREADY_EXISTS | 같은 객실 타입과 날짜의 재고 또는 요금 중복 등록 |

검증 항목: [T01](#t01)

<a id="rate-02"></a>

### PATCH /api/v1/room-types/{roomTypeId}/rates/{date} : 날짜별 요금 수정

API ID: `RATE-02`  
인증: HOST, 대상 숙소의 소유자

**Path Variables**

| 변수 | 타입 | 설명 |
|---|---|---|
| roomTypeId | string | 대상 ID, 최대 64자 |
| date | date | YYYY-MM-DD. 서버의 오늘 이상 |

**Request Body**

```json
{
  "version": 0,
  "amount": 110000
}
```

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `version` | integer | O | 0 이상. 마지막 조회 버전과 일치해야 함 |
| `amount` | integer | O | 1~1,000,000,000원 |

version 외 변경 필드가 한 개 이상 필요하다. 생략한 필드는 유지하며 null 허용 필드만 null로 해제할 수 있다.

**Response `200 OK`**

응답 모델: [DailyRate](#model-dailyrate)

```json
{
  "roomTypeId": "room_001",
  "date": "2026-10-10",
  "amount": 110000,
  "currency": "KRW",
  "version": 1
}
```

**Error Responses**

| 상태 | code | 조건 |
|---|---|---|
| 409 | VERSION_CONFLICT | 수정 버전 불일치 |

**처리 규칙**

- 수정 날짜는 서버의 오늘 이상이다. 통화는 바꾸지 않는다.
- 이미 생성된 예약의 가격 스냅샷은 변경하지 않는다.

검증 항목: [T05](#t05), [T13](#t13)

<a id="rate-03"></a>

### GET /api/v1/room-types/{roomTypeId}/rates : 기간 요금 조회

API ID: `RATE-03`  
인증: HOST, 대상 숙소의 소유자

**Path Variables**

| 변수 | 타입 | 설명 |
|---|---|---|
| roomTypeId | string | 대상 ID, 최대 64자 |

**Query Parameters**

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `from` | date | O | 범위 시작일 포함 |
| `to` | date | O | from보다 뒤, 최대 366일, 끝 날짜 제외 |

**Response `200 OK`**

응답 모델: [RateRange](#model-raterange)

```json
{
  "roomTypeId": "room_001",
  "from": "2026-10-10",
  "to": "2026-10-12",
  "items": [
    {
      "roomTypeId": "room_001",
      "date": "2026-10-10",
      "amount": 100000,
      "currency": "KRW",
      "version": 0
    },
    {
      "roomTypeId": "room_001",
      "date": "2026-10-11",
      "amount": 100000,
      "currency": "KRW",
      "version": 0
    }
  ],
  "missingDates": []
}
```

**처리 규칙**

- 과거 조회를 허용한다. 최대 366일이며 누락 날짜는 missingDates에 적는다.

검증 항목: [T01](#t01), [T07](#t07)

<a id="rate-04"></a>

### GET /api/v1/room-types/{roomTypeId}/rates/{date} : 날짜별 요금 조회

API ID: `RATE-04`  
인증: HOST, 대상 숙소의 소유자

**Path Variables**

| 변수 | 타입 | 설명 |
|---|---|---|
| roomTypeId | string | 대상 ID, 최대 64자 |
| date | date | YYYY-MM-DD. 과거 조회 허용 |

**Response `200 OK`**

응답 모델: [DailyRate](#model-dailyrate)

```json
{
  "roomTypeId": "room_001",
  "date": "2026-10-10",
  "amount": 100000,
  "currency": "KRW",
  "version": 0
}
```

**처리 규칙**

- 과거 날짜 조회를 허용한다. 해당 날짜 레코드가 없으면 404다.

검증 항목: [T01](#t01)

<a id="promotions"></a>

## 프로모션

<a id="promo-01"></a>

### POST /api/v1/promotions : 프로모션 등록

API ID: `PROMO-01`  
인증: OPERATOR

**Request Body**

```json
{
  "name": "가을 할인",
  "discountRate": 10,
  "campaignStartDate": "2026-10-01",
  "campaignEndDate": "2026-11-01",
  "stayStartDate": "2026-10-01",
  "stayEndDate": "2026-12-01",
  "minNights": 2,
  "regionCodes": [
    "SEOUL"
  ],
  "enabled": true
}
```

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `name` | string | O | 1~100자 |
| `discountRate` | integer | O | 1~99, 백분율 |
| `campaignStartDate` | date | O | 캠페인 적용 판단일 시작, 포함 |
| `campaignEndDate` | date | O | 시작보다 뒤, 끝 날짜 제외 |
| `stayStartDate` | date/null | X | 할인 대상 숙박일 시작. stayEndDate와 함께 설정 또는 둘 다 null |
| `stayEndDate` | date/null | X | 시작보다 뒤, 끝 날짜 제외. 두 필드 생략 시 둘 다 null |
| `minNights` | integer | O | 1~30 |
| `regionCodes` | string[] | O | 최대 100개, 중복 금지. 등록된 지역 코드. 빈 배열은 전체 지역 |
| `enabled` | boolean | X | 기본 true |

**Response `201 Created`**

```http
Location: /api/v1/promotions/promo_001
```

응답 모델: [Promotion](#model-promotion)

```json
{
  "id": "promo_001",
  "name": "가을 할인",
  "discountRate": 10,
  "campaignStartDate": "2026-10-01",
  "campaignEndDate": "2026-11-01",
  "stayStartDate": "2026-10-01",
  "stayEndDate": "2026-12-01",
  "minNights": 2,
  "regionCodes": [
    "SEOUL"
  ],
  "enabled": true,
  "version": 0,
  "createdAt": "2026-10-01T03:00:00.000Z",
  "updatedAt": "2026-10-01T03:00:00.000Z"
}
```

**처리 규칙**

- 정률 할인과 자동 적용형만 등록한다. 쿠폰과 수량 제한은 없다.
- 캠페인 기간은 적용 판단일이고, 숙박 기간은 할인 대상 날짜다. 두 기간을 구분한다.

검증 항목: [T28](#t28)

<a id="promo-02"></a>

### PATCH /api/v1/promotions/{promotionId} : 프로모션 수정

API ID: `PROMO-02`  
인증: OPERATOR

**Path Variables**

| 변수 | 타입 | 설명 |
|---|---|---|
| promotionId | string | 대상 ID, 최대 64자 |

**Request Body**

```json
{
  "version": 0,
  "enabled": false
}
```

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `version` | integer | O | 0 이상. 마지막 조회 버전과 일치해야 함 |
| `name` | string | X | 1~100자. 생략하면 기존 값 유지 |
| `discountRate` | integer | X | 1~99, 백분율. 생략하면 기존 값 유지 |
| `campaignStartDate` | date | X | 캠페인 적용 판단일 시작, 포함. 생략하면 기존 값 유지 |
| `campaignEndDate` | date | X | 시작보다 뒤, 끝 날짜 제외. 생략하면 기존 값 유지 |
| `stayStartDate` | date/null | X | 할인 대상 숙박일 시작. stayEndDate와 함께 설정 또는 둘 다 null. 생략하면 기존 값 유지 |
| `stayEndDate` | date/null | X | 시작보다 뒤, 끝 날짜 제외.. 생략하면 기존 값 유지 |
| `minNights` | integer | X | 1~30. 생략하면 기존 값 유지 |
| `regionCodes` | string[] | X | 최대 100개, 중복 금지. 등록된 지역 코드. 빈 배열은 전체 지역. 생략하면 기존 값 유지 |
| `enabled` | boolean | X | . 생략하면 기존 값 유지 |

version 외 변경 필드가 한 개 이상 필요하다. 생략한 필드는 유지하며 null 허용 필드만 null로 해제할 수 있다.

**Response `200 OK`**

응답 모델: [Promotion](#model-promotion)

```json
{
  "id": "promo_001",
  "name": "가을 할인",
  "discountRate": 10,
  "campaignStartDate": "2026-10-01",
  "campaignEndDate": "2026-11-01",
  "stayStartDate": "2026-10-01",
  "stayEndDate": "2026-12-01",
  "minNights": 2,
  "regionCodes": [
    "SEOUL"
  ],
  "enabled": false,
  "version": 1,
  "createdAt": "2026-10-01T03:00:00.000Z",
  "updatedAt": "2026-10-01T03:01:00.000Z"
}
```

**Error Responses**

| 상태 | code | 조건 |
|---|---|---|
| 409 | VERSION_CONFLICT | 수정 버전 불일치 |

**처리 규칙**

- 숙박 기간을 변경하거나 해제할 때 두 날짜를 함께 보낸다.
- 변경하지 않은 값과 합친 최종 상태에서도 날짜 순서와 제약을 검사한다.
- enabled=false는 수동 종료다. 기존 예약의 할인 스냅샷은 바뀌지 않는다.

검증 항목: [T13](#t13), [T28](#t28)

<a id="promo-03"></a>

### GET /api/v1/promotions/{promotionId} : 프로모션 상세 조회

API ID: `PROMO-03`  
인증: OPERATOR

**Path Variables**

| 변수 | 타입 | 설명 |
|---|---|---|
| promotionId | string | 대상 ID, 최대 64자 |

**Response `200 OK`**

응답 모델: [Promotion](#model-promotion)

```json
{
  "id": "promo_001",
  "name": "가을 할인",
  "discountRate": 10,
  "campaignStartDate": "2026-10-01",
  "campaignEndDate": "2026-11-01",
  "stayStartDate": "2026-10-01",
  "stayEndDate": "2026-12-01",
  "minNights": 2,
  "regionCodes": [
    "SEOUL"
  ],
  "enabled": true,
  "version": 0,
  "createdAt": "2026-10-01T03:00:00.000Z",
  "updatedAt": "2026-10-01T03:00:00.000Z"
}
```

검증 항목: [T28](#t28)

<a id="promo-04"></a>

### GET /api/v1/promotions : 프로모션 관리 목록 조회

API ID: `PROMO-04`  
인증: OPERATOR

**Query Parameters**

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `enabled` | boolean | X | true 또는 false. 생략하면 전체 |
| `page` | integer | X | 0 이상, 기본 0 |
| `size` | integer | X | 1~100, 기본 20 |

**Response `200 OK`**

응답 모델: [Page&lt;Promotion&gt;](#model-page-promotion)

```json
{
  "items": [
    {
      "id": "promo_001",
      "name": "가을 할인",
      "discountRate": 10,
      "campaignStartDate": "2026-10-01",
      "campaignEndDate": "2026-11-01",
      "stayStartDate": "2026-10-01",
      "stayEndDate": "2026-12-01",
      "minNights": 2,
      "regionCodes": [
        "SEOUL"
      ],
      "enabled": true,
      "version": 0,
      "createdAt": "2026-10-01T03:00:00.000Z",
      "updatedAt": "2026-10-01T03:00:00.000Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

검증 항목: [T28](#t28)

<a id="promo-05"></a>

### GET /api/v1/room-types/{roomTypeId}/applicable-promotions : 적용 가능 프로모션 조회

API ID: `PROMO-05`  
인증: 불필요

**Path Variables**

| 변수 | 타입 | 설명 |
|---|---|---|
| roomTypeId | string | 대상 ID, 최대 64자 |

**Query Parameters**

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `checkIn` | date | O | 서버의 오늘 이상, 숙박 시작일 포함 |
| `checkOut` | date | O | checkIn보다 뒤, 최대 30박, 끝 날짜 제외 |
| `guestCount` | integer | O | 1~100. 객실 수용 여부는 API별 처리 규칙에 따라 판단 |

**Response `200 OK`**

응답 모델: [ApplicablePromotions](#model-applicablepromotions)

```json
{
  "roomTypeId": "room_001",
  "checkIn": "2026-10-10",
  "checkOut": "2026-10-12",
  "guestCount": 2,
  "evaluatedAt": "2026-10-01T03:00:00.000Z",
  "items": [
    {
      "id": "promo_001",
      "name": "가을 할인",
      "discountRate": 10,
      "discountAmount": 20000,
      "selected": true
    }
  ],
  "selectedPromotionId": "promo_001"
}
```

**Error Responses**

| 상태 | code | 조건 |
|---|---|---|
| 409 | RATE_NOT_CONFIGURED | 하나 이상의 숙박 날짜에 요금 없음 |
| 409 | OCCUPANCY_EXCEEDED | 객실 최대 인원 초과 |

**처리 규칙**

- 서버의 오늘이 캠페인 기간에 속하고 지역, 박수, 숙박 기간 조건을 모두 만족하는 후보를 조회한다.
- 할인액이 가장 큰 하나를 선택한다. 동률이면 ID 오름차순이다.
- 재고를 선점하지 않는다. 재고 소진 여부와 할인 조건 충족 여부는 별개다.
- 후보가 없으면 items는 빈 배열, selectedPromotionId는 null이다.

검증 항목: [T28](#t28)

<a id="search"></a>

## 검색

<a id="search-01"></a>

### GET /api/v1/search/properties : 숙소 검색

API ID: `SEARCH-01`  
인증: 불필요

**Query Parameters**

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `regionCode` | string | O | 1~32자. 등록된 지역 코드 |
| `checkIn` | date | O | 서버의 오늘 이상, 숙박 시작일 포함 |
| `checkOut` | date | O | checkIn보다 뒤, 최대 30박, 끝 날짜 제외 |
| `guestCount` | integer | O | 1~100. 객실 수용 여부는 API별 처리 규칙에 따라 판단 |
| `page` | integer | X | 0 이상, 기본 0 |
| `size` | integer | X | 1~100, 기본 20 |

**Response `200 OK`**

응답 모델: [Page&lt;PropertySearchResult&gt;](#model-page-propertysearchresult)

```json
{
  "items": [
    {
      "property": {
        "id": "prop_001",
        "hostId": "host_001",
        "name": "서울 스테이",
        "regionCode": "SEOUL",
        "address": "서울특별시 종로구 예시로 10",
        "description": "숙박용 예시 숙소",
        "version": 0,
        "createdAt": "2026-10-01T03:00:00.000Z",
        "updatedAt": "2026-10-01T03:00:00.000Z"
      },
      "lowestTotalAmount": 180000,
      "currency": "KRW",
      "availableRoomTypes": [
        {
          "roomTypeId": "room_001",
          "name": "스탠다드 더블",
          "maxOccupancy": 2,
          "availableCount": 5,
          "totalAmount": 180000
        }
      ]
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

**처리 규칙**

- 인원 수용, 전 날짜 재고 존재, 전 날짜 가용 수 1 이상, 전 날짜 요금 존재를 모두 만족하는 객실만 포함한다.
- 해당 객실이 없는 숙소는 제외한다. 결과가 없으면 200과 빈 items를 반환한다.
- 검색은 Hold를 생성하지 않는다. 결과 이후 재고와 요금은 바뀔 수 있다.

검증 항목: [T06](#t06), [T07](#t07), [T27](#t27)

<a id="search-02"></a>

### GET /api/v1/room-types/{roomTypeId}/availability : 객실별 연박 가용성 조회

API ID: `SEARCH-02`  
인증: 불필요

**Path Variables**

| 변수 | 타입 | 설명 |
|---|---|---|
| roomTypeId | string | 대상 ID, 최대 64자 |

**Query Parameters**

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `checkIn` | date | O | 서버의 오늘 이상, 숙박 시작일 포함 |
| `checkOut` | date | O | checkIn보다 뒤, 최대 30박, 끝 날짜 제외 |
| `guestCount` | integer | O | 1~100. 객실 수용 여부는 API별 처리 규칙에 따라 판단 |

**Response `200 OK`**

응답 모델: [Availability](#model-availability)

```json
{
  "roomTypeId": "room_001",
  "checkIn": "2026-10-10",
  "checkOut": "2026-10-12",
  "guestCount": 2,
  "available": true,
  "availableCount": 5,
  "days": [
    {
      "date": "2026-10-10",
      "availableCount": 5
    },
    {
      "date": "2026-10-11",
      "availableCount": 5
    }
  ],
  "missingInventoryDates": [],
  "missingRateDates": [],
  "reasons": []
}
```

**처리 규칙**

- 가용성 부족은 200과 available=false로 반환한다. 날짜나 기간 형식 오류는 400이다.
- days는 전 숙박 날짜를 포함한다. 재고 레코드가 없으면 해당 날짜 availableCount=null이며 missingInventoryDates에도 넣는다.
- 요금 누락과 인원 초과도 reasons에 넣는다. 최상위 availableCount는 재고 기준 최소 수이며 인원이나 요금 조건까지 뜻하지 않는다.

검증 항목: [T06](#t06), [T07](#t07), [T27](#t27)

<a id="search-03"></a>

### GET /api/v1/room-types/{roomTypeId}/price-quote : 예상 숙박 금액 조회

API ID: `SEARCH-03`  
인증: 불필요

**Path Variables**

| 변수 | 타입 | 설명 |
|---|---|---|
| roomTypeId | string | 대상 ID, 최대 64자 |

**Query Parameters**

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `checkIn` | date | O | 서버의 오늘 이상, 숙박 시작일 포함 |
| `checkOut` | date | O | checkIn보다 뒤, 최대 30박, 끝 날짜 제외 |
| `guestCount` | integer | O | 1~100. 객실 수용 여부는 API별 처리 규칙에 따라 판단 |

**Response `200 OK`**

응답 모델: [PriceQuote](#model-pricequote)

```json
{
  "roomTypeId": "room_001",
  "checkIn": "2026-10-10",
  "checkOut": "2026-10-12",
  "guestCount": 2,
  "nights": 2,
  "estimatedAt": "2026-10-01T03:00:00.000Z",
  "price": {
    "currency": "KRW",
    "baseTotalAmount": 200000,
    "discountTotalAmount": 20000,
    "totalAmount": 180000,
    "appliedPromotion": {
      "id": "promo_001",
      "name": "가을 할인",
      "discountRate": 10
    },
    "days": [
      {
        "date": "2026-10-10",
        "baseAmount": 100000,
        "discountAmount": 10000,
        "finalAmount": 90000
      },
      {
        "date": "2026-10-11",
        "baseAmount": 100000,
        "discountAmount": 10000,
        "finalAmount": 90000
      }
    ]
  }
}
```

**Error Responses**

| 상태 | code | 조건 |
|---|---|---|
| 409 | RATE_NOT_CONFIGURED | 하나 이상의 숙박 날짜에 요금 없음 |
| 409 | OCCUPANCY_EXCEEDED | 객실 최대 인원 초과 |

**처리 규칙**

- 요금과 인원 조건을 확인하고 날짜별 단가, 할인액과 총액을 반환한다.
- 재고를 보장하거나 선점하지 않는다. 예약 생성 시 현재 가격으로 다시 계산한다.

검증 항목: [T12](#t12), [T13](#t13), [T28](#t28)

<a id="bookings"></a>

## 예약과 결제

<a id="book-01"></a>

### POST /api/v1/bookings : 예약 요청

API ID: `BOOK-01`  
인증: GUEST

**Request Headers**

| 헤더 | 필수 | 설명 |
|---|---|---|
| Idempotency-Key | O | 8~128자. 새 업무 요청은 새 키, 재전송은 같은 키 |

**Request Body**

```json
{
  "roomTypeId": "room_001",
  "checkIn": "2026-10-10",
  "checkOut": "2026-10-12",
  "guestCount": 2,
  "expectedTotalAmount": 180000,
  "currency": "KRW"
}
```

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `roomTypeId` | string | O | 비어 있지 않은 ID, 최대 64자 |
| `checkIn` | date | O | 서버의 오늘 이상, 숙박 시작일 포함 |
| `checkOut` | date | O | checkIn보다 뒤, 최대 30박, 끝 날짜 제외 |
| `guestCount` | integer | O | 1~100. 객실 수용 여부는 API별 처리 규칙에 따라 판단 |
| `expectedTotalAmount` | integer | O | 1~30,000,000,000원. 사용자가 확인한 예상 총액 |
| `currency` | string | O | KRW 고정 |

**Response `201 Created`**

```http
Location: /api/v1/bookings/booking_001
```

응답 모델: [Booking](#model-booking)

```json
{
  "id": "booking_001",
  "guestId": "guest_001",
  "propertyId": "prop_001",
  "roomTypeId": "room_001",
  "checkIn": "2026-10-10",
  "checkOut": "2026-10-12",
  "guestCount": 2,
  "status": "HELD",
  "expiresAt": "2026-10-01T03:10:00.000Z",
  "expirationReason": null,
  "priceSnapshot": {
    "currency": "KRW",
    "baseTotalAmount": 200000,
    "discountTotalAmount": 20000,
    "totalAmount": 180000,
    "appliedPromotion": {
      "id": "promo_001",
      "name": "가을 할인",
      "discountRate": 10
    },
    "days": [
      {
        "date": "2026-10-10",
        "baseAmount": 100000,
        "discountAmount": 10000,
        "finalAmount": 90000
      },
      {
        "date": "2026-10-11",
        "baseAmount": 100000,
        "discountAmount": 10000,
        "finalAmount": 90000
      }
    ]
  },
  "payment": {
    "attemptCount": 0,
    "approvedAttemptId": null,
    "attempts": [],
    "refund": null
  },
  "cancellationReason": null,
  "createdAt": "2026-10-01T03:00:00.000Z",
  "updatedAt": "2026-10-01T03:00:00.000Z",
  "confirmedAt": null,
  "canceledAt": null,
  "expiredAt": null,
  "serverNow": "2026-10-01T03:00:00.000Z",
  "version": 0
}
```

**Error Responses**

| 상태 | code | 조건 |
|---|---|---|
| 400 | IDEMPOTENCY_KEY_REQUIRED | 필수 멱등키 누락 또는 형식 오류 |
| 409 | IDEMPOTENCY_KEY_REUSED | 같은 멱등키에 다른 body |
| 409 | REQUEST_IN_PROGRESS | 동일 멱등 요청 처리 중 |
| 409 | INVENTORY_UNAVAILABLE | 하나 이상의 숙박 날짜에 선점 가능한 재고 없음 |
| 409 | INVENTORY_NOT_CONFIGURED | 하나 이상의 숙박 날짜에 재고 레코드 없음 |
| 409 | RATE_NOT_CONFIGURED | 하나 이상의 숙박 날짜에 요금 없음 |
| 409 | OCCUPANCY_EXCEEDED | 객실 최대 인원 초과 |
| 409 | PRICE_CHANGED | 예약 요청의 예상 총액과 서버 금액 불일치 |

**처리 규칙**

- guestId는 행위자에서 채운다. 객실 수는 1이며 프로모션 ID, 재고 수량과 예약 상태는 입력받지 않는다.
- 전 날짜 재고와 요금을 확인하고 가격을 재계산한다. 예상 총액이 다르면 PRICE_CHANGED이며 예약과 Hold는 만들지 않는다.
- 전 날짜 heldCount를 각각 1 올리고 HELD 예약, 가격 스냅샷과 멱등 결과를 함께 저장한다. 하나라도 실패하면 전부 롤백한다.
- 검색 결과만 믿고 선점하지 않는다. 동시 요청에서도 초과 예약이 없어야 한다.

검증 항목: [T06](#t06), [T07](#t07), [T08](#t08), [T09](#t09), [T10](#t10), [T11](#t11), [T12](#t12), [T13](#t13), [T29](#t29)

<a id="book-02"></a>

### GET /api/v1/bookings : 본인 예약 목록 조회

API ID: `BOOK-02`  
인증: GUEST

**Query Parameters**

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `status` | string | X | HELD / CONFIRMED / CANCELED / EXPIRED. 생략하면 전체 |
| `page` | integer | X | 0 이상, 기본 0 |
| `size` | integer | X | 1~100, 기본 20 |

**Response `200 OK`**

응답 모델: [Page&lt;Booking&gt;](#model-page-booking)

```json
{
  "items": [
    {
      "id": "booking_001",
      "guestId": "guest_001",
      "propertyId": "prop_001",
      "roomTypeId": "room_001",
      "checkIn": "2026-10-10",
      "checkOut": "2026-10-12",
      "guestCount": 2,
      "status": "HELD",
      "expiresAt": "2026-10-01T03:10:00.000Z",
      "expirationReason": null,
      "priceSnapshot": {
        "currency": "KRW",
        "baseTotalAmount": 200000,
        "discountTotalAmount": 20000,
        "totalAmount": 180000,
        "appliedPromotion": {
          "id": "promo_001",
          "name": "가을 할인",
          "discountRate": 10
        },
        "days": [
          {
            "date": "2026-10-10",
            "baseAmount": 100000,
            "discountAmount": 10000,
            "finalAmount": 90000
          },
          {
            "date": "2026-10-11",
            "baseAmount": 100000,
            "discountAmount": 10000,
            "finalAmount": 90000
          }
        ]
      },
      "payment": {
        "attemptCount": 0,
        "approvedAttemptId": null,
        "attempts": [],
        "refund": null
      },
      "cancellationReason": null,
      "createdAt": "2026-10-01T03:00:00.000Z",
      "updatedAt": "2026-10-01T03:00:00.000Z",
      "confirmedAt": null,
      "canceledAt": null,
      "expiredAt": null,
      "serverNow": "2026-10-01T03:00:00.000Z",
      "version": 0
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

**처리 규칙**

- 본인 예약만 반환한다. guestId와 hostId 쿼리는 받지 않는다.
- createdAt 내림차순, 동률이면 ID 내림차순이다.

검증 항목: [T02](#t02)

<a id="book-03"></a>

### GET /api/v1/bookings/{bookingId} : 예약 상세 조회

API ID: `BOOK-03`  
인증: GUEST, 대상 예약의 소유자

**Path Variables**

| 변수 | 타입 | 설명 |
|---|---|---|
| bookingId | string | 대상 ID, 최대 64자 |

**Response `200 OK`**

응답 모델: [Booking](#model-booking)

```json
{
  "id": "booking_001",
  "guestId": "guest_001",
  "propertyId": "prop_001",
  "roomTypeId": "room_001",
  "checkIn": "2026-10-10",
  "checkOut": "2026-10-12",
  "guestCount": 2,
  "status": "HELD",
  "expiresAt": "2026-10-01T03:10:00.000Z",
  "expirationReason": null,
  "priceSnapshot": {
    "currency": "KRW",
    "baseTotalAmount": 200000,
    "discountTotalAmount": 20000,
    "totalAmount": 180000,
    "appliedPromotion": {
      "id": "promo_001",
      "name": "가을 할인",
      "discountRate": 10
    },
    "days": [
      {
        "date": "2026-10-10",
        "baseAmount": 100000,
        "discountAmount": 10000,
        "finalAmount": 90000
      },
      {
        "date": "2026-10-11",
        "baseAmount": 100000,
        "discountAmount": 10000,
        "finalAmount": 90000
      }
    ]
  },
  "payment": {
    "attemptCount": 0,
    "approvedAttemptId": null,
    "attempts": [],
    "refund": null
  },
  "cancellationReason": null,
  "createdAt": "2026-10-01T03:00:00.000Z",
  "updatedAt": "2026-10-01T03:00:00.000Z",
  "confirmedAt": null,
  "canceledAt": null,
  "expiredAt": null,
  "serverNow": "2026-10-01T03:00:00.000Z",
  "version": 0
}
```

**처리 규칙**

- 결제 시도와 환불 정보를 함께 반환한다.
- 프론트는 serverNow와 expiresAt으로 남은 시간을 표시한다. 실제 결제 가능 여부는 서버 처리 시 다시 검사한다.

검증 항목: [T02](#t02), [T13](#t13), [T29](#t29)

<a id="pay-01"></a>

### POST /api/v1/bookings/{bookingId}/payment-attempts : Mock 결제 요청

API ID: `PAY-01`  
인증: GUEST, 대상 예약의 소유자

**Request Headers**

| 헤더 | 필수 | 설명 |
|---|---|---|
| Idempotency-Key | O | 8~128자. 새 업무 요청은 새 키, 재전송은 같은 키 |

**Path Variables**

| 변수 | 타입 | 설명 |
|---|---|---|
| bookingId | string | 대상 ID, 최대 64자 |

**Request Body**

```json
{
  "mockMode": "APPROVE"
}
```

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `mockMode` | string | X | APPROVE / DECLINE / DEFER. 생략하면 APPROVE |

**Response `202 Accepted`**

```http
Location: /api/v1/bookings/booking_001/payment-attempts
```

응답 모델: [PaymentAttempt](#model-paymentattempt)

```json
{
  "id": "attempt_001",
  "bookingId": "booking_001",
  "attemptNumber": 1,
  "status": "REQUESTED",
  "amount": 180000,
  "currency": "KRW",
  "pgTransactionId": "mock_tx_001",
  "mockMode": "APPROVE",
  "requestedAt": "2026-10-01T03:00:00.000Z",
  "completedAt": null,
  "failureCode": null
}
```

**Error Responses**

| 상태 | code | 조건 |
|---|---|---|
| 400 | IDEMPOTENCY_KEY_REQUIRED | 필수 멱등키 누락 또는 형식 오류 |
| 409 | IDEMPOTENCY_KEY_REUSED | 같은 멱등키에 다른 body |
| 409 | REQUEST_IN_PROGRESS | 동일 멱등 요청 처리 중 |
| 409 | BOOKING_EXPIRED | 결제 요청 시 TTL 만료 또는 이미 EXPIRED |
| 409 | BOOKING_STATE_CONFLICT | 현재 예약 상태에서 허용되지 않은 동작 |
| 409 | PAYMENT_IN_PROGRESS | 완료되지 않은 결제 시도 존재 |
| 409 | PAYMENT_ATTEMPTS_EXHAUSTED | 결제 시도 한도 도달 |

**처리 규칙**

- 접수 시도는 REQUESTED이며 202를 반환한다. 결제 결과는 예약 상세나 시도 목록으로 조회한다.
- 청구액은 예약의 고정된 스냅샷에서 가져온다. 클라이언트의 금액이나 카드정보는 받지 않는다.
- 유효한 HELD이고 진행 중 시도가 없으며 누적 시도 3회 미만일 때 새 시도를 받는다.
- 같은 키의 재전송은 시도를 늘리지 않는다. 실패 후 명시적인 다음 시도는 새 키를 사용한다.

검증 항목: [T14](#t14), [T15](#t15), [T16](#t16), [T17](#t17), [T18](#t18), [T26](#t26)

<a id="pay-02"></a>

### GET /api/v1/bookings/{bookingId}/payment-attempts : 결제 시도 목록 조회

API ID: `PAY-02`  
인증: GUEST, 대상 예약의 소유자

**Path Variables**

| 변수 | 타입 | 설명 |
|---|---|---|
| bookingId | string | 대상 ID, 최대 64자 |

**Response `200 OK`**

응답 모델: [PaymentAttemptList](#model-paymentattemptlist)

```json
{
  "bookingId": "booking_001",
  "attemptCount": 1,
  "items": [
    {
      "id": "attempt_001",
      "bookingId": "booking_001",
      "attemptNumber": 1,
      "status": "REQUESTED",
      "amount": 180000,
      "currency": "KRW",
      "pgTransactionId": "mock_tx_001",
      "mockMode": "APPROVE",
      "requestedAt": "2026-10-01T03:00:00.000Z",
      "completedAt": null,
      "failureCode": null
    }
  ]
}
```

**처리 규칙**

- 최대 3개이므로 페이지를 나누지 않는다. attemptNumber 오름차순이다.
- 환불 여부는 예약 상세의 payment.refund로 확인한다. 환불해도 승인 시도의 status는 APPROVED다.

검증 항목: [T14](#t14), [T16](#t16)

<a id="book-04"></a>

### POST /api/v1/bookings/{bookingId}/cancellations : 예약 취소

API ID: `BOOK-04`  
인증: GUEST, 대상 예약의 소유자

**Request Headers**

| 헤더 | 필수 | 설명 |
|---|---|---|
| Idempotency-Key | O | 8~128자. 새 업무 요청은 새 키, 재전송은 같은 키 |

**Path Variables**

| 변수 | 타입 | 설명 |
|---|---|---|
| bookingId | string | 대상 ID, 최대 64자 |

**Request Body**

```json
{
  "reason": "일정 변경"
}
```

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `reason` | string | X | 최대 300자. 생략하면 빈 문자열 |

**Response `200 OK`**

응답 모델: [Booking](#model-booking)

```json
{
  "id": "booking_001",
  "guestId": "guest_001",
  "propertyId": "prop_001",
  "roomTypeId": "room_001",
  "checkIn": "2026-10-10",
  "checkOut": "2026-10-12",
  "guestCount": 2,
  "status": "CANCELED",
  "expiresAt": "2026-10-01T03:10:00.000Z",
  "expirationReason": null,
  "priceSnapshot": {
    "currency": "KRW",
    "baseTotalAmount": 200000,
    "discountTotalAmount": 20000,
    "totalAmount": 180000,
    "appliedPromotion": {
      "id": "promo_001",
      "name": "가을 할인",
      "discountRate": 10
    },
    "days": [
      {
        "date": "2026-10-10",
        "baseAmount": 100000,
        "discountAmount": 10000,
        "finalAmount": 90000
      },
      {
        "date": "2026-10-11",
        "baseAmount": 100000,
        "discountAmount": 10000,
        "finalAmount": 90000
      }
    ]
  },
  "payment": {
    "attemptCount": 1,
    "approvedAttemptId": "attempt_001",
    "attempts": [
      {
        "id": "attempt_001",
        "bookingId": "booking_001",
        "attemptNumber": 1,
        "status": "APPROVED",
        "amount": 180000,
        "currency": "KRW",
        "pgTransactionId": "mock_tx_001",
        "mockMode": "APPROVE",
        "requestedAt": "2026-10-01T03:00:00.000Z",
        "completedAt": "2026-10-01T03:00:01.000Z",
        "failureCode": null
      }
    ],
    "refund": {
      "id": "refund_001",
      "paymentAttemptId": "attempt_001",
      "amount": 180000,
      "currency": "KRW",
      "status": "REFUNDED",
      "reason": "BOOKING_CANCELED",
      "refundedAt": "2026-10-01T03:05:00.000Z"
    }
  },
  "cancellationReason": "일정 변경",
  "createdAt": "2026-10-01T03:00:00.000Z",
  "updatedAt": "2026-10-01T03:05:00.000Z",
  "confirmedAt": "2026-10-01T03:00:01.000Z",
  "canceledAt": "2026-10-01T03:05:00.000Z",
  "expiredAt": null,
  "serverNow": "2026-10-01T03:05:00.000Z",
  "version": 2
}
```

**Error Responses**

| 상태 | code | 조건 |
|---|---|---|
| 400 | IDEMPOTENCY_KEY_REQUIRED | 필수 멱등키 누락 또는 형식 오류 |
| 409 | IDEMPOTENCY_KEY_REUSED | 같은 멱등키에 다른 body |
| 409 | REQUEST_IN_PROGRESS | 동일 멱등 요청 처리 중 |
| 409 | BOOKING_STATE_CONFLICT | 현재 예약 상태에서 허용되지 않은 동작 |
| 409 | CANCELLATION_NOT_ALLOWED | 제안한 취소 가능 날짜를 지남 |

**처리 규칙**

- 본인의 CONFIRMED 예약만 체크인 날짜 전에 전체 취소할 수 있다. 현재 초안은 취소 수수료 0과 전액 Mock 환불을 적용한다.
- 취소 상태, 모든 숙박 날짜의 soldCount 반환, 환불 기록이 함께 완료돼야 200을 반환한다.
- 같은 성공 키로 재전송하면 최초 응답을 반환한다. 새 키로 이미 취소한 예약을 다시 취소하면 409다.
- HELD 상태 이탈은 취소 API가 아닌 TTL 만료로 처리한다.

검증 항목: [T22](#t22), [T24](#t24), [T25](#t25)

<a id="internal"></a>

## 내부 처리와 Mock 이벤트

### Hold와 재고

예약 하나는 객실 타입 하나의 객실 한 개를 checkIn 이상, checkOut 미만으로 점유한다. Hold는 별도 자원이나 ID가 아닌 Booking의 HELD 상태다. 재고 수량은 재고 컨텍스트의 커맨드로만 변경한다.

| 내부 기능 | 트리거 | 상태와 재고 효과 |
|---|---|---|
| HoldInventory | 예약 생성 | 전 날짜 heldCount +1, Booking HELD 생성 |
| ConfirmBooking + CommitInventory | 유효한 HELD의 결제 승인 | Booking CONFIRMED, 전 날짜 heldCount -1과 soldCount +1 |
| ExpireBooking(TTL_EXPIRED) | TTL 스케줄러 또는 만료 시각 이후 결제 처리 | HELD에서 EXPIRED, 전 날짜 heldCount -1 |
| ExpireBooking(PAYMENT_FAILED) | TTL 전에 세 번째 결제 실패 | HELD에서 EXPIRED, 전 날짜 heldCount -1 |
| RefundPayment | 확정 예약 취소 | 승인 금액 환불, 전 날짜 soldCount -1 |
| RefundPayment | 만료 예약의 지연 승인 | 승인 금액 환불. 이미 반환한 재고를 다시 변경하지 않음 |

클라이언트용 /holds, /confirm, /release, /refund API는 만들지 않는다. 프론트가 status, heldCount 또는 soldCount를 PATCH하는 경로도 없다. Hold 원자성, Confirm과 환불 기능은 위 내부 처리로 기능 목록을 충족한다.

### 상태 전이와 시간 경계

| 현재 상태 | 조건 또는 입력 | 다음 상태 | 결제와 재고 |
|---|---|---|---|
| 없음 | 예약 생성 성공 | HELD | 전 날짜 선점, 금액 고정 |
| HELD | 승인 처리 시 now < expiresAt | CONFIRMED | 한 건 승인, 선점 재고를 판매 재고로 이동 |
| HELD | 1회 또는 2회 실패, now < expiresAt | HELD | 재고 유지, 완료된 시도 이후 재시도 가능 |
| HELD | 3회 실패, now < expiresAt | EXPIRED | PAYMENT_FAILED, 선점 재고 반환 |
| HELD | now >= expiresAt | EXPIRED | TTL_EXPIRED, 선점 재고 반환 |
| EXPIRED | 진행 중이던 시도의 지연 승인 | EXPIRED | 승인 기록 후 전액 Mock 환불, 재고 변경 없음 |
| CONFIRMED | 본인 취소, 취소 날짜 조건 충족 | CANCELED | 환불과 판매 재고 반환 |
| CONFIRMED 또는 CANCELED | 같은 승인 결과 재전달 | 유지 | 승인, 확정, 환불과 재고 변경을 반복하지 않음 |

TTL 스케줄러의 로컬 기본 주기는 1초를 제안한다. due 조건은 status=HELD와 expiresAt <= now다. DB 시각 또는 주입 가능한 공통 서버 Clock 중 하나로 판단 기준을 통일한다. 테스트는 Clock을 제어하고 긴 실제 대기를 요구하지 않는다.

승인과 만료가 경합하면 같은 예약에 대한 상태 전이를 직렬화하고, 전이 검사 시각을 기준으로 판단한다. 처리 시각이 정확히 expiresAt이면 만료다. 제공된 결제 시각으로 과거로 돌려 승인하지 않는다.

TTL과 세 번째 실패가 겹치면 이미 확정된 종료 원인을 덮어쓰지 않는다. 아직 HELD이고 실패 처리 시각에 TTL이 지났다면 TTL_EXPIRED를 우선한다. GET이 HELD를 보여줬더라도 이 검사에 따라 승인 대신 환불될 수 있다.

만료, 취소와 재고 반환은 재실행 가능해야 한다. 실제 상태가 한 번 전이된 경우에만 해당 재고 수량을 한 번 변경한다. CONFIRMED를 EXPIRED로 바꾸거나 판매 재고를 TTL로 반환하지 않는다.

### 가격과 프로모션

- 서버의 오늘이 campaignStartDate 이상, campaignEndDate 미만이고 enabled=true인 프로모션을 검사한다.
- 지역과 최소 박수 조건을 만족하고, 설정한 숙박 기간 안에 전체 숙박 구간이 들어와야 한다.
- 실제 할인액이 가장 큰 하나만 적용한다. 동률이면 ID 오름차순이다.
- 날짜별 할인액은 floor(baseAmount * discountRate / 100), 최종액은 baseAmount - discountAmount다. 전체 금액은 날짜별 값의 합이다.
- 할인 조건을 만족하는 후보가 없으면 appliedPromotion=null이고 모든 할인액은 0이다.
- 검색, 예상 금액과 예약은 같은 계산 규칙을 사용한다. 예약 생성 시 계산한 스냅샷 하나를 비교하고 그대로 저장한다. 비교 후 다시 계산해 다른 금액을 저장하지 않는다.
- 예약 생성 시 날짜별 단가, 할인액, 프로모션 ID와 이름을 고정한다. 이후 요금이나 프로모션 수정은 기존 예약에 반영하지 않는다.
- 예상 총액과 현재 금액이 다르면 PRICE_CHANGED다. 화면에서 다시 조회하고 확인받은 금액으로 요청한다. 같은 총액에서 할인 구성만 달라지면 최신 구성을 저장한다.
- 캠페인 기간이 지나면 조회와 계산에서 제외한다. 기간 만료로 enabled를 바꾸는 자동 작업은 없다.

### 결제 접수와 환불

- 게스트의 결제 요청은 예약이 받아 고정된 총액을 결제에 전달한다. 결제는 예약 Repository를 읽어 금액을 구하지 않는다.
- 새 시도 처리 순서는 멱등 재전송 확인, TTL 만료 여부, 예약 상태, 진행 중 시도, 시도 한도 검사다.
- EXPIRED 또는 만료 시각을 지난 HELD이면 BOOKING_EXPIRED다. HELD는 먼저 만료와 재고 반환을 저장한다.
- CONFIRMED와 CANCELED는 BOOKING_STATE_CONFLICT다. 유효한 HELD에 REQUESTED가 있으면 PAYMENT_IN_PROGRESS, 이미 3회면 PAYMENT_ATTEMPTS_EXHAUSTED다.
- 시도 저장과 횟수 증가는 한 번만 수행한다. 202는 접수 결과이며 승인 결과가 아니다.
- APPROVE는 승인, DECLINE은 실패 결과를 자동 전달한다. DEFER는 테스트용 수동 이벤트를 기다린다.
- 자동 결과는 시도 저장 후 전달하고, 저장된 REQUESTED와 mockMode로 재시작 후에도 재개한다. 중복 전달은 같은 업무를 반복하지 않는다.
- 실패는 접수 API의 500이 아닌 시도의 FAILED 상태다. 유효한 TTL 내 1회와 2회 실패는 HELD를 유지하고, 3회 실패는 만료와 재고 반환으로 이어진다.
- 취소는 CONFIRMED에서만 가능하다. HELD의 화면 이탈은 TTL로 처리한다. 호스트나 운영자의 취소는 제공하지 않는다.
- 취소는 전체 금액의 동기 Mock 환불을 사용한다. 취소 상태, 환불 기록과 재고 반환이 모두 저장돼야 성공이다. 실패하면 부분 결과를 남기지 않는다.
- 지연 승인 환불은 이미 반환한 재고를 다시 바꾸지 않는다. 같은 승인 시도에 환불은 한 개만 존재한다.
- 환불 후에도 시도는 APPROVED, approvedAttemptId는 기존 값을 유지한다. Refund로 반환 사실을 구분한다.
- GET에 잠시 HELD가 보이더라도 처리 시각이 expiresAt 이상이면 승인할 수 없다. 응답 상태나 화면 카운트다운이 승인 가능성을 보장하지 않는다.

<a id="internal-01"></a>

### POST /internal/mock-payments/events : Mock 결제 결과 전달

API ID: `INTERNAL-01`  
인증: MOCK_SYSTEM

**Request Body**

```json
{
  "eventId": "mock_event_0001",
  "paymentAttemptId": "attempt_001",
  "pgTransactionId": "mock_tx_001",
  "outcome": "APPROVED",
  "amount": 180000,
  "currency": "KRW"
}
```

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `eventId` | string | O | 1~128자. 이벤트 중복 식별자 |
| `paymentAttemptId` | string | O | 비어 있지 않은 ID, 최대 64자 |
| `pgTransactionId` | string | O | 비어 있지 않은 ID, 최대 64자 |
| `outcome` | string | O | APPROVED / FAILED |
| `amount` | integer | O | 1~30,000,000,000원. 대상 시도 금액과 일치 |
| `currency` | string | O | KRW 고정 |
| `failureCode` | string | 조건부 | outcome=FAILED에서 필수, MOCK_DECLINED. APPROVED에서는 보내지 않음 |

**Response `200 OK`**

응답 모델: [MockEventResult](#model-mockeventresult)

```json
{
  "eventId": "mock_event_0001",
  "paymentAttemptId": "attempt_001",
  "result": "PROCESSED",
  "processedAt": "2026-10-01T03:00:01.000Z"
}
```

**Error Responses**

| 상태 | code | 조건 |
|---|---|---|
| 409 | MOCK_EVENT_CONFLICT | 처리된 거래의 다른 결과 또는 같은 이벤트 ID의 다른 내용 |
| 409 | PAYMENT_AMOUNT_MISMATCH | Mock 거래 결과의 금액 또는 통화 불일치 |

**처리 규칙**

- 개발 프로파일의 테스트 러너와 Mock 어댑터에서 사용한다. 프론트 사용자 기능에서 직접 호출하지 않는다.
- Idempotency-Key 대신 eventId와 거래 결과로 중복을 판단한다.
- 이벤트, 시도 결과, 예약 정책, Mock 환불과 재고 변경이 함께 저장된 뒤 200을 반환한다.

**중복과 결과 처리**

1. 시도의 존재와 pgTransactionId 일치, amount와 currency 일치를 확인한다. 없는 시도는 404, 다른 거래 ID는 MOCK_EVENT_CONFLICT, 금액 차이는 PAYMENT_AMOUNT_MISMATCH다.
2. eventId를 유일하게 저장한다. 같은 eventId와 같은 body의 재전달은 DUPLICATE다. 다른 body면 MOCK_EVENT_CONFLICT다.
3. 다른 eventId라도 동일 거래에 같은 결과가 이미 처리됐으면 DUPLICATE다. 새로운 이벤트를 다시 발행하거나 수량을 바꾸지 않는다.
4. 한 시도에 이미 FAILED인데 APPROVED가 오거나 그 반대가 오면 MOCK_EVENT_CONFLICT다. DEFER 시도의 첫 결과가 늦게 도착하는 것과 모순되는 결과 재전달을 구분한다.
5. 승인 결과는 시도와 승인 한 건 제약을 확인한 뒤 예약 정책으로 전달한다. EXPIRED면 지연 승인 환불을 수행한다.
6. 실패 결과는 시도를 FAILED로 완료한다. TTL이 유효하고 세 번째면 만료시키고, 그보다 적으면 HELD를 유지한다.
7. 이벤트 처리 기록, 시도 결과, 예약 정책, 필요한 Mock 환불과 재고 변경이 모두 저장된 뒤 200을 반환한다. 실패하면 부분 반영 없이 롤백하고 재전달을 허용한다.
8. 승인 후 취소돼 환불된 시도에 같은 승인 이벤트가 다시 와도 추가 환불하지 않는다.

검증 항목: [T18](#t18), [T19](#t19), [T20](#t20), [T21](#t21), [T22](#t22), [T23](#t23), [T30](#t30)

<a id="models"></a>

## 응답 모델

모든 표의 필드는 항상 응답한다. 타입의 /null은 null 허용을 뜻한다. 배열은 값이 없으면 []다. 예시의 날짜와 시각은 2026-10-01을 기준으로 하며, 각 API 예시는 독립적인 상태를 보여준다.

<a id="model-property"></a>

### Property

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | string | 비어 있지 않은 ID, 최대 64자 |
| `hostId` | string | 비어 있지 않은 ID, 최대 64자 |
| `name` | string | 공백 제거 후 1~100자 |
| `regionCode` | string | 1~32자. 등록된 지역 코드 |
| `address` | string | 1~300자 |
| `description` | string | 최대 2,000자 |
| `version` | integer | 변경 버전 |
| `createdAt` | timestamp | UTC 시각 |
| `updatedAt` | timestamp | UTC 시각 |

<a id="model-roomtype"></a>

### RoomType

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | string | 비어 있지 않은 ID, 최대 64자 |
| `propertyId` | string | 비어 있지 않은 ID, 최대 64자 |
| `name` | string | 공백 제거 후 1~100자 |
| `maxOccupancy` | integer | 1~100 |
| `description` | string | 최대 2,000자 |
| `version` | integer | 변경 버전 |
| `createdAt` | timestamp | UTC 시각 |
| `updatedAt` | timestamp | UTC 시각 |

<a id="model-dailyinventory"></a>

### DailyInventory

| 필드 | 타입 | 설명 |
|---|---|---|
| `roomTypeId` | string | 비어 있지 않은 ID, 최대 64자 |
| `date` | date | 해당 숙박 날짜 |
| `totalCount` | integer | 0~100,000. 증감량이 아닌 총 수량 |
| `heldCount` | integer | 선점 수, 0 이상 |
| `soldCount` | integer | 판매 수, 0 이상 |
| `availableCount` | integer | totalCount - heldCount - soldCount, 0 이상 |
| `version` | integer | 재고 변경 버전 |

<a id="model-dailyrate"></a>

### DailyRate

| 필드 | 타입 | 설명 |
|---|---|---|
| `roomTypeId` | string | 비어 있지 않은 ID, 최대 64자 |
| `date` | date | 해당 숙박 날짜 |
| `amount` | integer | 1~1,000,000,000원 |
| `currency` | string | KRW 고정 |
| `version` | integer | 요금 변경 버전 |

<a id="model-inventoryrange"></a>

### InventoryRange

| 필드 | 타입 | 설명 |
|---|---|---|
| `roomTypeId` | string | 비어 있지 않은 ID, 최대 64자 |
| `from` | date | 범위 시작일 포함 |
| `to` | date | from보다 뒤, 최대 366일, 끝 날짜 제외 |
| `items` | [DailyInventory](#model-dailyinventory)[] | date 오름차순. 존재하는 날짜만 포함 |
| `missingDates` | date[] | 레코드가 없는 날짜, 없으면 빈 배열 |

<a id="model-raterange"></a>

### RateRange

| 필드 | 타입 | 설명 |
|---|---|---|
| `roomTypeId` | string | 비어 있지 않은 ID, 최대 64자 |
| `from` | date | 범위 시작일 포함 |
| `to` | date | from보다 뒤, 최대 366일, 끝 날짜 제외 |
| `items` | [DailyRate](#model-dailyrate)[] | date 오름차순. 존재하는 날짜만 포함 |
| `missingDates` | date[] | 레코드가 없는 날짜, 없으면 빈 배열 |

<a id="model-promotion"></a>

### Promotion

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | string | 비어 있지 않은 ID, 최대 64자 |
| `name` | string | 1~100자 |
| `discountRate` | integer | 1~99, 백분율 |
| `campaignStartDate` | date | 캠페인 적용 판단일 시작, 포함 |
| `campaignEndDate` | date | 시작보다 뒤, 끝 날짜 제외 |
| `stayStartDate` | date/null | 할인 대상 숙박일 시작. stayEndDate와 함께 설정 또는 둘 다 null |
| `stayEndDate` | date/null | 시작보다 뒤, 끝 날짜 제외. 숙박 기간 제한이 없으면 두 필드 모두 null |
| `minNights` | integer | 1~30 |
| `regionCodes` | string[] | 최대 100개, 중복 금지. 등록된 지역 코드. 빈 배열은 전체 지역 |
| `enabled` | boolean | 사용 여부 |
| `version` | integer | 변경 버전 |
| `createdAt` | timestamp | UTC 시각 |
| `updatedAt` | timestamp | UTC 시각 |

<a id="model-appliedpromotion"></a>

### AppliedPromotion

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | string | 비어 있지 않은 ID, 최대 64자 |
| `name` | string | 적용 시점에 복사한 이름 |
| `discountRate` | integer | 적용한 백분율 |

<a id="model-priceday"></a>

### PriceDay

| 필드 | 타입 | 설명 |
|---|---|---|
| `date` | date | 숙박 날짜 |
| `baseAmount` | integer | 할인 전 1박 금액 |
| `discountAmount` | integer | 날짜별 버림한 할인액 |
| `finalAmount` | integer | baseAmount - discountAmount |

<a id="model-pricesnapshot"></a>

### PriceSnapshot

| 필드 | 타입 | 설명 |
|---|---|---|
| `currency` | string | KRW 고정 |
| `baseTotalAmount` | integer | days의 baseAmount 합계 |
| `discountTotalAmount` | integer | days의 discountAmount 합계 |
| `totalAmount` | integer | days의 finalAmount 합계 |
| `appliedPromotion` | [AppliedPromotion](#model-appliedpromotion)/null | 선택한 프로모션. 없으면 null |
| `days` | [PriceDay](#model-priceday)[] | 숙박 전 날짜를 date 오름차순으로 포함 |

<a id="model-applicablepromotion"></a>

### ApplicablePromotion

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | string | 비어 있지 않은 ID, 최대 64자 |
| `name` | string | 프로모션 이름 |
| `discountRate` | integer | 할인 백분율 |
| `discountAmount` | integer | 전체 숙박의 할인액 |
| `selected` | boolean | 최종 적용 대상으로 선택됐는지 |

<a id="model-applicablepromotions"></a>

### ApplicablePromotions

| 필드 | 타입 | 설명 |
|---|---|---|
| `roomTypeId` | string | 비어 있지 않은 ID, 최대 64자 |
| `checkIn` | date | 숙박 시작일, 포함 |
| `checkOut` | date | 숙박 종료일, 제외 |
| `guestCount` | integer | 요청 또는 예약의 인원, 1~100 |
| `evaluatedAt` | timestamp | UTC 시각 |
| `items` | [ApplicablePromotion](#model-applicablepromotion)[] | 할인액 내림차순, 동률이면 ID 오름차순. 없으면 빈 배열 |
| `selectedPromotionId` | string/null | 선택한 프로모션 ID, 없으면 null |

<a id="model-roomsearchresult"></a>

### RoomSearchResult

| 필드 | 타입 | 설명 |
|---|---|---|
| `roomTypeId` | string | 비어 있지 않은 ID, 최대 64자 |
| `name` | string | 객실 타입 이름 |
| `maxOccupancy` | integer | 최대 인원 |
| `availableCount` | integer | 숙박 전 날짜 최소 가용 수 |
| `totalAmount` | integer | 할인 후 전체 숙박 금액 |

<a id="model-propertysearchresult"></a>

### PropertySearchResult

| 필드 | 타입 | 설명 |
|---|---|---|
| `property` | [Property](#model-property) | 숙소 정보 |
| `lowestTotalAmount` | integer | 포함된 객실의 최저 할인 후 총액 |
| `currency` | string | KRW 고정 |
| `availableRoomTypes` | [RoomSearchResult](#model-roomsearchresult)[] | 모든 예약 가능 조건을 충족한 객실 타입 |

<a id="model-availabilityday"></a>

### AvailabilityDay

| 필드 | 타입 | 설명 |
|---|---|---|
| `date` | date | 숙박 날짜 |
| `availableCount` | integer/null | 재고가 있으면 가용 수, 레코드가 없으면 null |

<a id="model-availability"></a>

### Availability

| 필드 | 타입 | 설명 |
|---|---|---|
| `roomTypeId` | string | 비어 있지 않은 ID, 최대 64자 |
| `checkIn` | date | 숙박 시작일, 포함 |
| `checkOut` | date | 숙박 종료일, 제외 |
| `guestCount` | integer | 요청 또는 예약의 인원, 1~100 |
| `available` | boolean | 인원, 전 날짜 재고와 요금을 모두 만족하는지 |
| `availableCount` | integer | 재고가 모두 있으면 최소 가용 수, 누락이면 0 |
| `days` | [AvailabilityDay](#model-availabilityday)[] | 전 숙박 날짜, date 오름차순 |
| `missingInventoryDates` | date[] | 재고 레코드 누락 날짜 |
| `missingRateDates` | date[] | 요금 누락 날짜 |
| `reasons` | string[] | OCCUPANCY_EXCEEDED / INVENTORY_NOT_CONFIGURED / INVENTORY_UNAVAILABLE / RATE_NOT_CONFIGURED. 해당 항목만 중복 없이 반환 |

<a id="model-pricequote"></a>

### PriceQuote

| 필드 | 타입 | 설명 |
|---|---|---|
| `roomTypeId` | string | 비어 있지 않은 ID, 최대 64자 |
| `checkIn` | date | 숙박 시작일, 포함 |
| `checkOut` | date | 숙박 종료일, 제외 |
| `guestCount` | integer | 요청 또는 예약의 인원, 1~100 |
| `nights` | integer | checkOut - checkIn, 1~30 |
| `estimatedAt` | timestamp | UTC 시각 |
| `price` | [PriceSnapshot](#model-pricesnapshot) | 조회 시점의 계산 결과. 예약 확정 금액은 아님 |

<a id="model-paymentattempt"></a>

### PaymentAttempt

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | string | 비어 있지 않은 ID, 최대 64자 |
| `bookingId` | string | 비어 있지 않은 ID, 최대 64자 |
| `attemptNumber` | integer | 1~3 |
| `status` | string | REQUESTED / APPROVED / FAILED |
| `amount` | integer | 예약 스냅샷 총액, 최대 30,000,000,000원 |
| `currency` | string | KRW 고정 |
| `pgTransactionId` | string | 비어 있지 않은 ID, 최대 64자 |
| `mockMode` | string | APPROVE / DECLINE / DEFER |
| `requestedAt` | timestamp | UTC 시각 |
| `completedAt` | timestamp/null | 미완료이면 null |
| `failureCode` | string/null | FAILED이면 MOCK_DECLINED, 그 외 null |

<a id="model-refund"></a>

### Refund

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | string | 비어 있지 않은 ID, 최대 64자 |
| `paymentAttemptId` | string | 비어 있지 않은 ID, 최대 64자 |
| `amount` | integer | 승인 금액 전액 |
| `currency` | string | KRW 고정 |
| `status` | string | REFUNDED |
| `reason` | string | BOOKING_CANCELED / LATE_APPROVAL |
| `refundedAt` | timestamp | UTC 시각 |

<a id="model-paymentsummary"></a>

### PaymentSummary

| 필드 | 타입 | 설명 |
|---|---|---|
| `attemptCount` | integer | 0~3 |
| `approvedAttemptId` | string/null | 승인 시도 ID, 승인 전 null. 환불해도 유지 |
| `attempts` | [PaymentAttempt](#model-paymentattempt)[] | attemptNumber 오름차순, 최대 3개 |
| `refund` | [Refund](#model-refund)/null | 환불 전 null |

<a id="model-booking"></a>

### Booking

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | string | 비어 있지 않은 ID, 최대 64자 |
| `guestId` | string | 비어 있지 않은 ID, 최대 64자 |
| `propertyId` | string | 비어 있지 않은 ID, 최대 64자 |
| `roomTypeId` | string | 비어 있지 않은 ID, 최대 64자 |
| `checkIn` | date | 숙박 시작일, 포함 |
| `checkOut` | date | 숙박 종료일, 제외 |
| `guestCount` | integer | 요청 또는 예약의 인원, 1~100 |
| `status` | string | HELD / CONFIRMED / CANCELED / EXPIRED |
| `expiresAt` | timestamp | 생성 시 저장한 Hold 만료 시각, 확정과 취소 후에도 유지 |
| `expirationReason` | string/null | EXPIRED이면 TTL_EXPIRED / PAYMENT_FAILED, 그 외 null |
| `priceSnapshot` | [PriceSnapshot](#model-pricesnapshot) | 생성 시점에 고정한 금액 |
| `payment` | [PaymentSummary](#model-paymentsummary) | 결제 시도와 환불 내역 |
| `cancellationReason` | string/null | 취소 전 null, 이유 없이 취소하면 빈 문자열 |
| `createdAt` | timestamp | UTC 시각 |
| `updatedAt` | timestamp | UTC 시각 |
| `confirmedAt` | timestamp/null | 확정 전 null, 취소 후에도 유지 |
| `canceledAt` | timestamp/null | 취소 전 null |
| `expiredAt` | timestamp/null | 만료 전 null |
| `serverNow` | timestamp | UTC 시각 |
| `version` | integer | 예약 상태 변경 버전 |

<a id="model-paymentattemptlist"></a>

### PaymentAttemptList

| 필드 | 타입 | 설명 |
|---|---|---|
| `bookingId` | string | 비어 있지 않은 ID, 최대 64자 |
| `attemptCount` | integer | 0~3 |
| `items` | [PaymentAttempt](#model-paymentattempt)[] | attemptNumber 오름차순, 최대 3개 |

<a id="model-mockeventresult"></a>

### MockEventResult

| 필드 | 타입 | 설명 |
|---|---|---|
| `eventId` | string | 입력 이벤트 ID |
| `paymentAttemptId` | string | 비어 있지 않은 ID, 최대 64자 |
| `result` | string | PROCESSED / DUPLICATE |
| `processedAt` | timestamp | 최초 업무 처리 완료 시각 |

<a id="model-page-property"></a>

### Page&lt;Property&gt;

| 필드 | 타입 | 설명 |
|---|---|---|
| `items` | [Property](#model-property)[] | 현재 페이지 목록, 없으면 빈 배열 |
| `page` | integer | 요청 페이지, 0 이상 |
| `size` | integer | 요청 크기, 1~100 |
| `totalElements` | integer | 전체 항목 수, 0 이상 |
| `totalPages` | integer | 전체 페이지 수, 결과가 없으면 0 |

<a id="model-page-roomtype"></a>

### Page&lt;RoomType&gt;

| 필드 | 타입 | 설명 |
|---|---|---|
| `items` | [RoomType](#model-roomtype)[] | 현재 페이지 목록, 없으면 빈 배열 |
| `page` | integer | 요청 페이지, 0 이상 |
| `size` | integer | 요청 크기, 1~100 |
| `totalElements` | integer | 전체 항목 수, 0 이상 |
| `totalPages` | integer | 전체 페이지 수, 결과가 없으면 0 |

<a id="model-page-promotion"></a>

### Page&lt;Promotion&gt;

| 필드 | 타입 | 설명 |
|---|---|---|
| `items` | [Promotion](#model-promotion)[] | 현재 페이지 목록, 없으면 빈 배열 |
| `page` | integer | 요청 페이지, 0 이상 |
| `size` | integer | 요청 크기, 1~100 |
| `totalElements` | integer | 전체 항목 수, 0 이상 |
| `totalPages` | integer | 전체 페이지 수, 결과가 없으면 0 |

<a id="model-page-propertysearchresult"></a>

### Page&lt;PropertySearchResult&gt;

| 필드 | 타입 | 설명 |
|---|---|---|
| `items` | [PropertySearchResult](#model-propertysearchresult)[] | 현재 페이지 목록, 없으면 빈 배열 |
| `page` | integer | 요청 페이지, 0 이상 |
| `size` | integer | 요청 크기, 1~100 |
| `totalElements` | integer | 전체 항목 수, 0 이상 |
| `totalPages` | integer | 전체 페이지 수, 결과가 없으면 0 |

<a id="model-page-booking"></a>

### Page&lt;Booking&gt;

| 필드 | 타입 | 설명 |
|---|---|---|
| `items` | [Booking](#model-booking)[] | 현재 페이지 목록, 없으면 빈 배열 |
| `page` | integer | 요청 페이지, 0 이상 |
| `size` | integer | 요청 크기, 1~100 |
| `totalElements` | integer | 전체 항목 수, 0 이상 |
| `totalPages` | integer | 전체 페이지 수, 결과가 없으면 0 |

<a id="verification"></a>

## 검증 기준

| ID | 시나리오 | 확인할 결과 |
|---|---|---|
| <a id="t01"></a>T01 | 숙소부터 재고와 요금까지 등록 후 조회 | 요청과 응답 스키마, 소유자, version 일치 |
| <a id="t02"></a>T02 | 다른 HOST의 숙소 수정 또는 다른 GUEST의 예약 조회 | 자원 정보 없이 404, 데이터 변경 없음 |
| <a id="t03"></a>T03 | 재고 일괄 등록 중 이미 존재하는 날짜 포함 | 전체 실패, 새 날짜도 등록되지 않음 |
| <a id="t04"></a>T04 | totalCount를 heldCount + soldCount 아래로 수정 | 409, 기존 수량 유지 |
| <a id="t05"></a>T05 | 버전이 지난 재고와 요금 수정 | VERSION_CONFLICT, 최근 변경 유지 |
| <a id="t06"></a>T06 | 체크아웃 날짜에 재고와 요금이 없음 | 그 날짜는 숙박 대상에서 제외하므로 예약 가능 |
| <a id="t07"></a>T07 | 숙박 날짜 중 한 날에 요금 또는 재고 없음 | 오류 코드 구분, Booking과 Hold 없음 |
| <a id="t08"></a>T08 | 마지막 객실에 동시 예약 | 한 요청만 성공, 초과 예약 0 |
| <a id="t09"></a>T09 | 연박 중 하루만 재고 부족 | 모든 날짜의 선점 변화와 Booking 없음 |
| <a id="t10"></a>T10 | 예약 성공 후 응답 유실과 같은 키 재전송 | 같은 예약과 최초 응답, 추가 선점 없음 |
| <a id="t11"></a>T11 | 같은 성공 키에 다른 예약 body | IDEMPOTENCY_KEY_REUSED |
| <a id="t12"></a>T12 | 검색 이후 가격 변경 | PRICE_CHANGED, 새 가격 동의 전 예약 생성 없음 |
| <a id="t13"></a>T13 | 예약 생성 이후 요금과 프로모션 변경 | 기존 스냅샷과 실제 청구액 유지 |
| <a id="t14"></a>T14 | 결제 응답 유실과 같은 키 재전송 | 시도와 횟수 추가 없음 |
| <a id="t15"></a>T15 | 진행 중 결제에 새 키로 다음 결제 요청 | PAYMENT_IN_PROGRESS, 시도 한 개 |
| <a id="t16"></a>T16 | 1회와 2회 결제 실패 | HELD와 선점 유지, 다음 시도 가능 |
| <a id="t17"></a>T17 | TTL 전 3회 결제 실패 | EXPIRED, PAYMENT_FAILED, 재고 한 번 반환 |
| <a id="t18"></a>T18 | 시계가 expiresAt과 정확히 같을 때 결제 승인 | EXPIRED, TTL_EXPIRED, 승인액 환불 |
| <a id="t19"></a>T19 | 만료 스케줄러와 승인 경합 | 확정 또는 만료와 환불 중 계약에 맞는 한 경로만 반영 |
| <a id="t20"></a>T20 | 이미 EXPIRED인 DEFER 시도에 지연 승인 | 예약은 EXPIRED, 환불 한 번, 재고 추가 반환 없음 |
| <a id="t21"></a>T21 | 같은 거래 승인 반복, 다른 eventId로도 반복 | DUPLICATE, 이중 확정과 불필요한 환불 없음 |
| <a id="t22"></a>T22 | 승인 후 취소 후 같은 승인 재전달 | 취소 유지, 환불 한 번 유지 |
| <a id="t23"></a>T23 | 이벤트 저장 이후 정책 처리 실패 유도 | 부분 결과 롤백, 재전달 시 정상 완료 |
| <a id="t24"></a>T24 | 본인 확정 예약 취소와 같은 키 재전송 | 전액 환불과 재고 반환 각 한 번 |
| <a id="t25"></a>T25 | HELD 취소 또는 체크인 날짜 이후 취소 | 해당 상태 또는 날짜 오류, 수량 변경 없음 |
| <a id="t26"></a>T26 | 자동 Mock 모드에서 접수 후 앱 재시작 | 저장된 시도로 결과 처리를 재개, 중복 업무 효과 없음 |
| <a id="t27"></a>T27 | 가용성은 있지만 인원 초과 또는 요금 누락 | availability=false와 reasons, 검색에서 제외 |
| <a id="t28"></a>T28 | 할인 소수점과 동률 후보 | 날짜별 버림, 날짜 합계 일치, 선택 결과 재현 |
| <a id="t29"></a>T29 | 만료 이후 예약 생성 요청의 성공 재전송 | 최초 HELD 스냅샷 재전송, 별도 GET은 EXPIRED |
| <a id="t30"></a>T30 | 개발 프로파일 밖에서 개발 헤더와 Mock 경로 사용 | 개발 인증과 Mock 이벤트 경로 비활성 |

동시성, 유니크 제약과 롤백은 실제 MySQL에서 확인한다. 메모리 Repository나 다른 DB에서 성공한 결과만으로 대체하지 않는다. 이 표는 향후 작성할 계약 테스트 목록이며 실행 결과가 아니다.

<a id="proposals"></a>

## 검토할 정책

다음 값은 구현용 초안의 제안이다. 정책이 바뀌면 해당 요청과 응답, 상태 처리와 검증 기준을 함께 변경한다.

| ID | 항목 | 초안에서 사용하는 제안 |
|---|---|---|
| P01 | Hold TTL | 생성 시점부터 10분, 로컬 스케줄러 주기 1초. 서버 설정으로 관리하고 예약에 expiresAt을 저장한다 |
| P02 | 기간 제한 | 예약과 검색은 최대 30박. 재고 일괄 등록과 날짜별 관리 조회는 최대 366일 |
| P03 | 통화와 달력 | KRW 정수 금액. 숙박과 캠페인 날짜는 Asia/Seoul, 시각 응답은 UTC |
| P04 | 프로모션 | 정률 할인, 중복 적용 없음. 실제 할인액이 가장 큰 하나를 선택하고 동률이면 ID 오름차순 |
| P05 | 취소 | 체크인 날짜 전까지 전체 취소와 전액 Mock 환불. 수수료는 0. 부분 취소와 예약 변경은 이번 구현에서 보류 |
| P06 | 검색과 예약 금액 차이 | 예약 요청의 expectedTotalAmount와 서버 재계산 금액이 다르면 409. 예약과 Hold는 만들지 않는다 |
| P07 | 로컬 행위자 | 회원 API 대신 서버에 등록한 개발용 행위자와 X-Dev-Actor-Id를 사용한다. 실제 인증 수단으로 취급하지 않는다 |
| P08 | 원자성 구현 후보 | 단일 MySQL 트랜잭션으로 예약과 재고 변경을 묶는다. 구체 락 방식과 Payment 애그리거트 구조는 미정이지만, 외부 계약의 전체 성공 또는 전체 실패는 지켜야 한다 |
| P09 | 가격 계산 | 날짜별 할인액을 원 단위로 버림한 뒤 합산한다. 세금과 별도 수수료는 추가하지 않는다 |
| P10 | 판매 제약 | 별도의 min_los와 closed 필드는 이번 초안에서 보류한다. 재고 0은 판매 불가로 처리한다 |
| P11 | 프로모션 종료 | 수동 종료는 PATCH enabled=false로 표현한다. 캠페인 날짜가 지나도 상태를 바꾸는 자동 작업은 만들지 않는다 |

회원 기능, 쿠폰, 알림, 정산, 리뷰, 호실 배정과 실제 PG 연동은 이번 범위에서 제외한다. 부분 취소, 예약 변경과 별도 판매 제약 필드는 위 제안에 따라 보류한다.
