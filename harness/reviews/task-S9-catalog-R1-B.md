# task-S9-catalog R1 평가자 B 리포트

평가일: 2026-09-14

평가 방식: 허용된 문서와 대상 코드 및 테스트만 정적으로 대조했다. 테스트는 실행하지 않았다. 기존 A와 B 리포트, 과거 평가 작업, `step9-verification.md`, `harness/docs/`, `harness/state/`, `harness/decisions/`는 읽지 않았다.

## 판정 요약

치명 1 / 보통 3 / 확인필요 2

## 상세

| 원본 번호 | 심각도 | 위치(파일:행) | 위반 축 | 문제 | 근거(코드 또는 실행 결과) | 수정 제안 |
|---:|---|---|---|---|---|---|
| 1 | 치명 | `backend/src/main/java/com/o2o/catalog/domain/Property.java:56`, `backend/src/main/java/com/o2o/catalog/domain/Property.java:102`, `backend/src/main/java/com/o2o/catalog/domain/RoomType.java:47`, `backend/src/main/java/com/o2o/catalog/domain/RoomType.java:94` | 추적성 양방향, 결정 근거의 자립성, 요구사항 역추적 | 채택된 낙관적 잠금이 원자적으로 강제되지 않는다. 두 트랜잭션이 같은 version을 읽으면 둘 다 메모리 대조를 통과하고 저장할 수 있어 마지막 저장이 앞선 수정을 덮는다. 두 요청 모두 200으로 끝날 수 있으므로 11 명세의 `version` 불일치 409와 계약 2-2절의 동시 수정 검출이 깨진다. 현재 테스트는 숙소의 순차적인 잘못된 version만 확인해 이 경합을 잡지 못한다. | `version` 필드는 `@Column`일 뿐이고, 애그리거트가 읽어 온 값과 요청값을 비교한 뒤 직접 1을 더한다. `PropertyJpaRepository`와 `RoomTypeJpaRepository`에도 version 조건부 갱신이나 잠금이 없다. `CatalogUpdateAndListApiTest:92-106`은 이미 저장된 뒤 잘못된 version을 보내는 순차 경로만 검사한다. | JPA `@Version`을 사용하거나 `where id = ? and version = ?` 조건부 갱신의 영향 행 수로 충돌을 판정한다. 프레임워크의 낙관적 잠금 예외도 409 `VERSION_CONFLICT`로 변환한다. Property와 RoomType 각각 같은 version으로 동시에 수정하는 테스트를 추가한다. |
| 2 | 보통 | `backend/src/main/java/com/o2o/catalog/domain/Region.java:27`, `backend/src/main/java/com/o2o/catalog/application/CatalogApplicationService.java:67`, `backend/src/main/java/com/o2o/catalog/api/PropertyController.java:96` | 추적성 양방향, 요구사항 역추적 | CAT-01과 CAT-02가 요구하는 등록된 지역 코드 확인이 없다. CAT-04의 `regionCode`는 1자 이상 32자 이하 제약도 적용되지 않는다. 임의 문자열이 숙소에 저장되고 목록 필터로 전달된다. | `Region.of`는 문자열을 그대로 감싸고 앱 서비스도 별도 조회 없이 생성한다. CAT-04 컨트롤러는 검증 애노테이션 없는 `String`을 받는다. `CatalogApiTest:350-356`은 등록 여부와 무관한 32자 문자열의 등록 성공을 기대하고, `CatalogUpdateAndListApiTest:195-205`는 매번 만든 임의 지역 코드로 등록과 조회를 성공시킨다. 이는 11 CAT-01 처리 규칙과 CAT-01, CAT-02, CAT-04 필드 제약을 반대로 고정한다. | 지역 fixture 또는 지역 조회 포트를 두고 등록과 수정 전에 존재를 확인한다. CAT-04에도 1자 이상 32자 이하 검증을 적용한다. 미등록 지역의 등록, 수정, 목록 조회가 400 `INVALID_REQUEST`인지 확인하는 테스트를 추가하고 임의 지역 성공 테스트는 등록된 fixture를 사용한다. |
| 3 | 보통 | `backend/src/main/java/com/o2o/catalog/api/UpdatePropertyRequest.java:19`, `backend/src/main/java/com/o2o/catalog/api/UpdateRoomTypeRequest.java:17`, `backend/src/main/java/com/o2o/catalog/domain/Property.java:105`, `backend/src/main/java/com/o2o/catalog/domain/RoomType.java:103` | 추적성 양방향, 요구사항 역추적 | 이름의 공백 제거 후 1자 이상 100자 규칙이 구현되지 않았다. 특히 PATCH는 공백만 있는 이름이 `@Size(min = 1)`을 통과하고 그대로 저장된다. 앞뒤 공백을 제외하면 100자 이하인 입력도 원문 길이로 검사되어 거절될 수 있다. | 등록 DTO의 `@NotBlank`는 공백만 있는 값은 막지만 정규화하지 않는다. 수정 DTO는 `@Size`만 사용한다. 애그리거트는 전달된 이름을 그대로 대입하고 대상 테스트에는 공백 정규화 경계가 없다. | 요청 경계에서 이름을 먼저 `strip`한 뒤 1자 이상 100자를 검사하고 정규화된 값을 저장한다. CAT-01, CAT-02, CAT-06, CAT-07 각각에 공백만 있는 값과 앞뒤 공백이 있는 경계값 테스트를 추가한다. |
| 4 | 확인필요 | `backend/src/main/java/com/o2o/shared/SharedExceptionHandler.java:24` | 추적성 양방향, 요구사항 역추적 | 허용된 대상 코드만 보면 예상하지 못한 예외를 500 `INTERNAL_ERROR`의 공통 Error 모델로 바꾸는 경로가 없다. 타입 변환 등 일부 400 경로도 동일 형식을 보장하는지 확인할 수 없다. 다만 허용 범위 밖의 다른 전역 Advice가 처리할 가능성을 배제할 수 없어 확인필요다. | 대상의 전역 Advice는 행위자, Bean Validation, 읽을 수 없는 body, version 충돌, `IllegalArgumentException`만 처리한다. 11 에러 응답 절은 모든 API에 공통 형식과 500 `INTERNAL_ERROR`를 적용한다고 정한다. 허용 범위에는 나머지 전역 예외 처리 코드가 포함되지 않았다. | 전체 전역 Advice를 허용 입력에 추가해 최종 매핑을 확인한다. 없다면 예상하지 못한 예외와 파라미터 타입 오류를 공통 Error 모델로 변환하고 응답에 내부 예외 내용을 넣지 않는 테스트를 추가한다. |
| 5 | 보통 | `backend/src/test/java/com/o2o/catalog/domain/RoomTypeTest.java:27`, `backend/src/test/java/com/o2o/catalog/api/CatalogUpdateAndListApiTest.java:154` | 요구사항 역추적 | I14는 등록과 수정 모두 RoomType이 지켜야 하지만 테스트는 등록 실패만 검사한다. CAT-07 테스트는 유효한 하향 수정과 소유권만 확인해 `RoomType.update`에서 I14 검사를 없애거나 PATCH DTO의 하한을 제거해도 대상 테스트가 잡지 못한다. | `RoomTypeTest` 네 건은 전부 `RoomType.register`만 호출한다. CAT-07 두 건은 `maxOccupancy:2` 성공과 다른 소유자 404다. 06-4의 updateRoomType 계약은 인원 양수 I14를 명시한다. | `RoomType.update`에 0과 음수를 넣는 도메인 실패 테스트와 양수 성공 테스트를 짝으로 추가한다. CAT-07에도 `maxOccupancy` 0과 101이 400이고 기존 값과 version이 유지되는 API 테스트를 추가한다. |
| 6 | 확인필요 | `backend/src/main/java/com/o2o/catalog/domain/PropertyRepository.java:45`, `backend/src/main/java/com/o2o/catalog/domain/RoomTypeRepository.java:26` | 추적성 양방향, 결정 근거의 자립성 | 검색용 무페이지 조회 메서드 둘은 이 작업 계약의 API 9개와 허용된 11, 06-2, 06-4 문서에 근거가 없다. 대상 목록은 다른 프로모션과 검색 묶음이 추가했다고 설명하지만 그 묶음의 계약과 06-1은 허용 입력이 아니어서 추가 동작의 정당성을 독립 판정할 수 없다. | `findAllByRegionCode`와 `findAllByPropertyId` 및 JPA 구현은 SEARCH-01, 06-1, 다른 작업 계약을 주석 근거로 든다. 현재 허용 문서에는 해당 계약이 없다. | 이 두 메서드를 현재 평가 대상에서 제외하거나, 검색 묶음 계약과 필요한 06-1 절을 허용 입력에 추가해 별도 묶음에서 양방향 추적을 확인한다. |

## 요구사항 역추적표

| 요구사항 | 대응 테스트 또는 코드 | 커버 여부 |
|---|---|---|
| R1 연박 중 한 날짜라도 재고 부족 시 전체 선점 실패 | 대상 코드에는 재고 선점과 Booking 생성 경로가 없다. | 해당 없음. 입력 팩 문장과 대상 58개 파일을 직접 대조했다. |
| R2 동시 요청에서도 초과 예약 0 | 대상 코드에는 재고 수량과 예약 경합 경로가 없다. 카탈로그 version 경합은 별도 CAT-02와 CAT-07 계약이며 원본 번호 1에서 지적했다. | 해당 없음 |
| R3 동일 요청 재시도 시 중복 Booking 방지 | 대상 API 9개에는 Idempotency-Key와 Booking 생성이 없다. | 해당 없음 |
| R4 HELD 종료 시 재고 반환 보장 | 대상 코드에는 HELD, 만료, 결제 실패, 재고 반환이 없다. | 해당 없음 |
| R5 확정 Booking 금액의 스냅샷 불변 | 대상 코드에는 Booking, 금액, 요금, 프로모션 스냅샷이 없다. | 해당 없음 |
| CAT-01 숙소 등록 | `CatalogApiTest:74-90`, `CatalogApiTest:142-166`, `CatalogApiTest:197-217`, `CatalogApiTest:274-312`, `CatalogApiTest:341-375` | 부분 커버. 등록 지역 존재 확인과 이름 공백 정규화는 원본 번호 2와 3. |
| CAT-02 숙소 수정 | `CatalogUpdateAndListApiTest:73-149`, `PropertyController:79-88`, `Property.update:100-119` | 부분 커버. 동시 version 경합, 등록 지역 확인, 이름 공백 정규화는 원본 번호 1부터 3. |
| CAT-03 숙소 상세 조회 | `CatalogApiTest:92-109`, `CatalogApplicationServiceTest:93-110` | 커버 |
| CAT-04 숙소 목록 조회 | `CatalogUpdateAndListApiTest:195-254`, `PageQuery:19-31`, `SpringPage:26-32` | 부분 커버. regionCode 형식과 등록 여부는 원본 번호 2. |
| CAT-05 본인 숙소 목록 조회 | `CatalogUpdateAndListApiTest:258-270`, `HostPropertyController:35-44` | 커버 |
| CAT-06 객실 타입 등록 | `CatalogApiTest:111-138`, `CatalogApiTest:168-181`, `CatalogApiTest:219-230`, `CatalogApiTest:314-331`, `CatalogApplicationServiceTest:50-81` | 커버 |
| CAT-07 객실 타입 수정 | `CatalogUpdateAndListApiTest:154-191`, `RoomType.update:92-111` | 부분 커버. 동시 version 경합과 I14 수정 실패 경로는 원본 번호 1과 5. |
| CAT-08 객실 타입 상세 조회 | `CatalogApiTest:125-138`, `RoomTypeController:62-67` | 커버 |
| CAT-09 숙소의 객실 타입 목록 조회 | `CatalogUpdateAndListApiTest:274-298`, `PageQuery:19-31`, `SpringPage:26-32` | 커버. 공통 페이지 경계와 정렬 테스트를 공유 코드까지 정적으로 연결했다. |
| I14 maxOccupancy > 0 | `RoomTypeTest:27-54`, `CatalogApiTest:219-230`, `RoomType.register:76-82`, `RoomType.update:92-111` | 부분 커버. 등록은 커버되지만 수정 실패 테스트가 없어 원본 번호 5. |

## 실행 기록

| 명령 | 목적 | 종료 상태 | 결과 요약 | 직접 실행 여부 |
|---|---|---|---|---|
| `node harness/out/task-S9-catalog-R1/verify-eval-workspace.mjs` | 허용 입력을 열기 전 작업공간 검증 | 실패 | 샌드박스에서 `spawnSync git EPERM`이 발생해 검사 0건, 실패 1건이었다. 제품 판정으로 사용하지 않았다. | 예 |
| `node harness/out/task-S9-catalog-R1/verify-eval-workspace.mjs` | 동일한 읽기 전용 검증 재실행 | 통과 | 허용된 환경에서 10건 전부 PASS. 금지 파일 존재 알림을 확인했고 열지 않았다. | 예 |
| 테스트 명령 없음 | 평가자 B의 문서와 코드 정적 대조 | 미실행 | 요청문 4절에 따라 테스트를 실행하지 않았다. A의 검증 ID 결과를 재판정하지 않았다. | 아니오 |

## 보조 표

| 원본 번호 | 지적 ID | 심각도 | 위반 기준 |
|---:|---|---|---|
| 1 | S9-R1-B-01 | 치명 | 추적성 양방향, 결정 근거의 자립성, 요구사항 역추적 |
| 2 | S9-R1-B-02 | 보통 | 추적성 양방향, 요구사항 역추적 |
| 3 | S9-R1-B-03 | 보통 | 추적성 양방향, 요구사항 역추적 |
| 4 | S9-R1-B-04 | 확인필요 | 추적성 양방향, 요구사항 역추적 |
| 5 | S9-R1-B-05 | 보통 | 요구사항 역추적 |
| 6 | S9-R1-B-06 | 확인필요 | 추적성 양방향, 결정 근거의 자립성 |

요약 대조: 치명 1건은 S9-R1-B-01, 보통 3건은 S9-R1-B-02, S9-R1-B-03, S9-R1-B-05, 확인필요 2건은 S9-R1-B-04, S9-R1-B-06이다.

## 실제로 읽은 허용 입력과 SHA256

요청문에 기록된 앞 16자리 해시와 실제 해시는 모두 일치했다. 대상 코드 58개도 `eval-target-files.md`의 앞 16자리와 모두 일치했다.

### 요청과 평가 문서

| 파일 | SHA256 |
|---|---|
| `harness/out/task-S9-catalog-R1/eval-request-B.md` | `3474906a80a69ee261faba4a420de2c7c94240c4e790d220bcee2b55fed8ec80` |
| `harness/out/task-S9-catalog-R1/eval-target-files.md` | `c019bc515f6c68e21c621b54be091c310ca380734f2a7e92fdd9152caf9c3696` |
| `harness/project-sync/o2o-review-input-pack.md` | `1608942c0815f9113ef022e74131743bdca3cb30cfaebb277cb8e038cf0dc645` |
| `harness/prompts/eval-criteria-code.md` | `c5ed835951fe09a5c015d50abbbacf9755af6ba8be807e1be1b9c0ba2cb1553d` |
| `harness/tasks/task-S9-catalog.md` | `7d6cc521e6df67e6b3c6d4b8959c7f5dc92d0b7834c395745215a3c057725581` |
| `document/11-o2o-api-spec.md` | `3f2613a77b6499039b6d8b2310504d7c37047005ae028f6568455472c1ea105b` |
| `document/06-2-o2o-aggregates.md` | `113c6734b5525e1d5a5b2116de1250232707bc0518b3470d133ec0b7c993cec9` |
| `document/06-4-o2o-contracts.md` | `edacbe47d6d63e3f2b72849234a0a9dbd4dbd297a8eee7b1ea19d70ab5e3c3e3` |

### 대상 코드와 테스트

| 파일 | SHA256 |
|---|---|
| `backend/src/main/java/com/o2o/BackendApplication.java` | `63d8dd1cd4fc3522cf94b9b4116ac0e67b177c59ebf5cac20fd724f4d4a2e12d` |
| `backend/src/main/java/com/o2o/catalog/api/ApiTime.java` | `28bd8f96d46b14d794d00ceb8934d5efa05f608b0320aab9a5173592f433e4ed` |
| `backend/src/main/java/com/o2o/catalog/api/CatalogExceptionHandler.java` | `8b151584497dd6a3bb94552a57d5605435357c1dab0789c88937cc7f096922d1` |
| `backend/src/main/java/com/o2o/catalog/api/HostPropertyController.java` | `888443528f5a7b76fa8756e08ac6285c1b93db3c7ba6f2177ec82d6fc1f2a110` |
| `backend/src/main/java/com/o2o/catalog/api/PageResponse.java` | `abb54058fe65684fb63d84b5e36549ffa6d77bdee8e30a7ae1580f825b53b252` |
| `backend/src/main/java/com/o2o/catalog/api/PropertyController.java` | `e8a2d47f1695a43b7d4e6dd6e205caa544c0bbcbf6780aaa66abec14e353d7c7` |
| `backend/src/main/java/com/o2o/catalog/api/PropertyResponse.java` | `f11a5e9d1d3576f253cfbdf30c3e1aba2d5f12749dee65b80a07b71d7efe1c4d` |
| `backend/src/main/java/com/o2o/catalog/api/RegisterPropertyRequest.java` | `8ee42f870f97b09dccd7a3f079d564cbb267ec3bf4bd297e2dcc11bff97246e3` |
| `backend/src/main/java/com/o2o/catalog/api/RegisterRoomTypeRequest.java` | `fb15201b56b16ced7ef007e02e6078a11e6b3441f95739c1f0b7829242460ab4` |
| `backend/src/main/java/com/o2o/catalog/api/RoomTypeController.java` | `cc8e89bfe80ef6ee298512d6bd52426b6dc51b034971d439202624b158780a41` |
| `backend/src/main/java/com/o2o/catalog/api/RoomTypeResponse.java` | `be2e84cefe5e26293afd30ff70375207f3cca2056927f819985f1e88e8bbcf74` |
| `backend/src/main/java/com/o2o/catalog/api/UpdatePropertyRequest.java` | `e050b49cfbcc350e28c1a44635eb00c94bd95ca8600744caeacd685189aa04ee` |
| `backend/src/main/java/com/o2o/catalog/api/UpdateRoomTypeRequest.java` | `09db488c44a7d42d8dee6ec1925946bf5fc636cb1f9b31bd9d187f2a8d29bac7` |
| `backend/src/main/java/com/o2o/catalog/api/package-info.java` | `dfc17890ea93502e7a36f62e1b2226d2552235d73756d964b98c7f078aa9cfb0` |
| `backend/src/main/java/com/o2o/catalog/application/CatalogApplicationService.java` | `545943809fe3ba1ee2a05fb8c27213d325b6fcb47e37ff75e4b6d0fbb8e10a0e` |
| `backend/src/main/java/com/o2o/catalog/application/package-info.java` | `58767ad768782efa0ebb65e444c86312b47b789be7b442b31e1132c0d48527c3` |
| `backend/src/main/java/com/o2o/catalog/domain/Address.java` | `6e17f88d97c67d42290d373550a6c2c447674cccb62c405ee00198afcc4f84c2` |
| `backend/src/main/java/com/o2o/catalog/domain/InvalidOccupancyException.java` | `11279b782e8f50d10577b24cac4cb1620459ecc00d1c0f964ffd22e91342b256` |
| `backend/src/main/java/com/o2o/catalog/domain/Property.java` | `e73cf584b8fbb36e4e9b40facb37dc21050daab6a0f2b0ea79945b1c24332e7c` |
| `backend/src/main/java/com/o2o/catalog/domain/PropertyNotFoundException.java` | `06087bac1c0fd05678e15fdb4dd2cbc471010708d7a6972a785e58bbfdbc8422` |
| `backend/src/main/java/com/o2o/catalog/domain/PropertyRegistered.java` | `0decefabfd0a435f00d3523a915b8e49ab8c0233f2d5d599057099e4455ff165` |
| `backend/src/main/java/com/o2o/catalog/domain/PropertyRepository.java` | `16e33bf00a476b7d701a57515a68bb84d0e66b9efe4df7e6501c0f5f695080db` |
| `backend/src/main/java/com/o2o/catalog/domain/PropertyUpdated.java` | `f55adf67ef1edcb46e5f983b666b914c7850a3ad2beae77f68dc88a105c2ac06` |
| `backend/src/main/java/com/o2o/catalog/domain/Region.java` | `6ca668bd794aec193dfdfed692793c2859eb6c6b309c36aa9bb007f1d5e08f94` |
| `backend/src/main/java/com/o2o/catalog/domain/RoomType.java` | `d1636604e01bc6c68d02c7ed2dbc7d8cf061ec9e6603a79e45f78f43aa11a9bd` |
| `backend/src/main/java/com/o2o/catalog/domain/RoomTypeNotFoundException.java` | `d6150d55acd65a4eb32de1aaa370f929c424ea6e6141eee331ba646d2429df42` |
| `backend/src/main/java/com/o2o/catalog/domain/RoomTypeRegistered.java` | `b8df7d1afa2adecb3d2f360b58858e5325ace9640f872b9cf25bd9ccae3a06a9` |
| `backend/src/main/java/com/o2o/catalog/domain/RoomTypeRepository.java` | `4bc9fb2944398c87d6d4a5185a6d78c8b78cbd83f884f0f78458f24381e2558e` |
| `backend/src/main/java/com/o2o/catalog/domain/RoomTypeUpdated.java` | `b70df072e84ec19a661f29dd0a31d8e4f41fb393871e77405a37b5c947718c78` |
| `backend/src/main/java/com/o2o/catalog/domain/package-info.java` | `ac3f49e1fb65f0269da4de1338617ea6f7b6c3d06bb8c760f73e8c7d25542d87` |
| `backend/src/main/java/com/o2o/catalog/infrastructure/JpaPropertyRepository.java` | `13e9019d988f9ddcb33ec6c991079a9b4fa969cf146d799733a44655e97c895d` |
| `backend/src/main/java/com/o2o/catalog/infrastructure/JpaRoomTypeRepository.java` | `1e8c20f8aedbe0af5c3fe41775678f88e33dc47c69102f4659d6feea14cc55d2` |
| `backend/src/main/java/com/o2o/catalog/infrastructure/PropertyJpaRepository.java` | `d4031307cddeb409401d82f8fd8e2d29026828ea51b057bbdf938067969500f7` |
| `backend/src/main/java/com/o2o/catalog/infrastructure/RoomTypeJpaRepository.java` | `f04526290d1cd17526be7b8be3beb551b121f2e3ba4312b49682b62549c25ae3` |
| `backend/src/main/java/com/o2o/catalog/infrastructure/SpringPage.java` | `f50f329323f90e73ae11086023bf248222b29d796079596bb7ba1e7c4b6d0130` |
| `backend/src/main/java/com/o2o/catalog/infrastructure/package-info.java` | `abc8e180c3586037ce52cafee6cd40fa52958f60be52a55056ae7e6a4e54ccd4` |
| `backend/src/main/java/com/o2o/catalog/package-info.java` | `d66c8ce0b0e03e7bfa241a7a9d3f2b4bb624218576e4dc3fe908dea87e21e0f2` |
| `backend/src/main/java/com/o2o/shared/Actor.java` | `7f7e903397728fe8285472ab651e22c2591c367d4ce4fd0a723ae6f6b83d3597` |
| `backend/src/main/java/com/o2o/shared/ActorAccessDeniedException.java` | `0f965fc018766d2ae2bbc6cb4d8434f657860af62f1756be96e34261f949e3fc` |
| `backend/src/main/java/com/o2o/shared/ActorRegistry.java` | `9b0e000f80967372e7e9952caa9fa417d9d59d7e7307706a8cbe538109fe5e83` |
| `backend/src/main/java/com/o2o/shared/ActorRequiredException.java` | `25053079edb39cbe6a9fca94587fa86275535ec599b4e08105b8decb04449712` |
| `backend/src/main/java/com/o2o/shared/ActorResolver.java` | `c40ff8e6d5a4547b1ae719cd8a804f3d3b84f45b1e633b2d916cceaf7cc82092` |
| `backend/src/main/java/com/o2o/shared/ActorRole.java` | `3676bc3d906743a24472bb5d81df6874c436090ac47d992ed7e9be90d581ce74` |
| `backend/src/main/java/com/o2o/shared/ErrorResponse.java` | `c62440297db7ef67cb7134effa4ca62dc3a0171938ed14cbc1e36f8e167d8d2b` |
| `backend/src/main/java/com/o2o/shared/HostId.java` | `4aee2c3a7e435bc9d69b2e21bc084405177abd0b2f68fd2119669b1a62140541` |
| `backend/src/main/java/com/o2o/shared/PageQuery.java` | `6a328d62d68a47481e7cc449aa1b820f6ab1e68b4fd7542d35e09325a89c6f73` |
| `backend/src/main/java/com/o2o/shared/PageResult.java` | `ebe19b03ab5c8263c6c22729b37c4fda7a16332729eddd6c2b8a8a3a2b5d1cc4` |
| `backend/src/main/java/com/o2o/shared/PropertyId.java` | `e589d1cf50597b75016459aef6e8b073640db7cbb9d314a64770fb6e2bb5c4c9` |
| `backend/src/main/java/com/o2o/shared/RoomTypeId.java` | `b0c4ca9058044fb9f546a2e3796e8bd6cc890b0f8a2931d466d1701c5ec1a659` |
| `backend/src/main/java/com/o2o/shared/SharedExceptionHandler.java` | `bdde2ead0ce4c920f1678ffa2fb9bf9b4373ea9877065ef063d5db92a45b069b` |
| `backend/src/main/java/com/o2o/shared/VersionConflictException.java` | `68dfc27ff3545de6c9c7db2855d08bae359a6916f662bb7503cc02c58f1d7d5c` |
| `backend/src/main/java/com/o2o/shared/package-info.java` | `85dc998fba6350bee782ae19ca5658fc3dc227e90d470e9c70858deb91875a59` |
| `backend/src/test/java/com/o2o/BackendApplicationTests.java` | `164e5536219a1ab9e9170f6f9f413587fa691a55bb0cb9ed114fef0c61ff1200` |
| `backend/src/test/java/com/o2o/DatabaseConnectionTest.java` | `3883835eddebf1374dc3244e4a5d690b54d6ef6a25ffba27ddea691a747de7ed` |
| `backend/src/test/java/com/o2o/catalog/api/CatalogApiTest.java` | `700d593eed9f374178f807a22a1863d226547c872a6bd3ba80fbfb8b357f1230` |
| `backend/src/test/java/com/o2o/catalog/api/CatalogUpdateAndListApiTest.java` | `1273258f096fc74632b3470ac5627400627518295f4d55968510865cc53443fe` |
| `backend/src/test/java/com/o2o/catalog/application/CatalogApplicationServiceTest.java` | `33eb39e10a7f18a9d00f1ebe436fadf7933d9e2a5cdc0d559467a17223f7056d` |
| `backend/src/test/java/com/o2o/catalog/domain/RoomTypeTest.java` | `2c50f1c299c1dbc2e1f302bbd484538365941b1801b5482666c088a721199054` |
