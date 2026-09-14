# task-S9-catalog R1 평가자 A 재평가

평가일: 2026-09-14

평가 작업공간: `C:/Dev/potenup/99_projects/o2o-dev`

작업공간 검증은 최초 샌드박스 실행에서 `spawnSync git EPERM`으로 실패했다. 같은 명령을 허용된 환경에서 다시 실행해 10건 전부 PASS를 확인했다. 금지 파일은 읽지 않았다.

## 판정 요약

치명 1 / 보통 2 / 확인필요 1

## 상세

| 심각도 | 위치(파일:행) | 위반 축 | 문제 | 근거(코드 또는 실행 결과) | 수정 제안 |
|---|---|---|---|---|---|
| 치명 | `backend/src/main/java/com/o2o/catalog/domain/Property.java:56` | 동시성과 트랜잭션, 계약 하강 | `version`이 일반 컬럼이고 수정 시 조회한 엔티티의 숫자만 비교한다. 동시 요청 둘이 같은 version을 읽으면 둘 다 통과하고 뒤의 저장이 앞의 값을 덮을 수 있다. CAT-02와 CAT-07의 낙관적 잠금과 409 계약이 실제 경합에서 보장되지 않는다. | `Property.update`와 `RoomType.update`는 메모리 값 비교 뒤 version을 직접 올리지만 `@Version`이나 version 조건부 UPDATE가 없다. C4 테스트 `CatalogUpdateAndListApiTest:92`는 요청 version 99인 순차 실패만 확인한다. | JPA `@Version` 또는 `where id = ? and version = ?` 조건부 갱신으로 DB 커밋 시점 충돌을 검출하고 409로 번역한다. Property와 RoomType 각각 같은 version의 동시 PATCH 테스트를 실제 MySQL에 추가한다. |
| 보통 | `backend/src/main/java/com/o2o/catalog/domain/Region.java:27` | API 계약 준수, 계약 하강 | 등록된 지역 코드만 저장해야 하지만 임의 문자열을 그대로 받는다. CAT-01과 CAT-02의 지역 존재 확인이 없고 CAT-04도 임의 코드를 조회 조건으로 받는다. | `Region.of`는 확인 없이 객체를 만든다. 같은 파일 9행부터 12행도 지역 fixture가 없어 확인하지 않는다고 명시한다. 11 CAT-01은 등록 지역 코드 확인을 요구한다. `CatalogUpdateAndListApiTest:197`은 임의 `REGION_` 값을 정상 등록한다. | 승인된 지역 fixture 조회 포트를 두고 등록과 수정 전에 존재를 확인한다. CAT-04에도 길이와 등록 코드 검증을 적용하고 미등록 코드의 400 테스트를 추가한다. |
| 보통 | `backend/src/main/java/com/o2o/catalog/api/UpdatePropertyRequest.java:17` | API 계약 준수, 계약 하강 | PATCH 문자열 필드가 공백 제거 후 최소 길이와 허용하지 않은 null 규칙을 온전히 지키지 않는다. 공백만 있는 name, regionCode, address를 `@Size`가 허용하고 POST의 description 명시적 null도 생략과 같이 빈 문자열로 바꾼다. | `UpdatePropertyRequest`와 `UpdateRoomTypeRequest` 문자열은 `@Size`뿐이다. 두 등록 DTO의 `descriptionOrEmpty`는 null을 빈 문자열로 바꾼다. 11 공통 규칙은 허용하지 않은 null을 400으로 하고 name 등은 공백 제거 후 최소 길이를 요구한다. 대상 API 테스트에는 이 경계가 없다. | 생략과 명시적 null을 구분하고 명세가 null을 허용하지 않는 필드는 400으로 거절한다. 문자열은 trim한 값으로 길이를 검사하고 등록과 수정 양쪽에 공백 및 null 테스트를 추가한다. |
| 확인필요 | `backend/src/main/java/com/o2o/catalog/api/PropertyController.java:94` | API 계약 준수 | 명세에 없는 Query 필드를 거절하는 장치가 허용 입력 안에서 확인되지 않는다. Spring 기본 동작이면 `?unknown=x`를 무시하고 200을 반환한다. 목록 밖 전역 필터가 막는지는 허용 입력만으로 확인할 수 없다. | 11 공통 헤더 74행은 명세에 표시하지 않은 Query 필드를 지원하지 않는다고 한다. 세 목록 컨트롤러와 대상 테스트에는 미정의 Query 검증이 없다. 승인 명령에는 이를 직접 호출할 명령이 없다. | 전역 처리 유무를 확인하고 없다면 허용 Query 이름을 검사해 400 `INVALID_REQUEST`로 통일한다. CAT-04, CAT-05, CAT-09 테스트를 추가한다. |

구조 축에서 Property와 RoomType만 Repository를 가지며 둘 다 06-2의 애그리거트 루트다. catalog domain에서 infrastructure 참조는 없었다. JPA 애노테이션은 입력 팩의 도메인 모델과 JPA 엔티티 비분리 전제에 따른다. 허용 대상 안에서 다른 컨텍스트 직접 참조는 찾지 못했다. 카탈로그 API에는 Idempotency-Key 적용 대상이 없어 멱등 규칙 축은 해당 없음이다.

## 실행 기록

| 명령 | 목적 | 종료 상태 | 결과 요약 | 직접 실행 여부 |
|---|---|---|---|---|
| `node harness/out/task-S9-catalog-R1/verify-eval-workspace.mjs` | 작업공간 검증 | 최초 실패 | 샌드박스의 `spawnSync git EPERM` | 직접 실행 |
| 같은 작업공간 검증 | 작업공간 재검증 | PASS | 허용된 환경에서 10건 통과 | 직접 실행 |
| `docker compose -f backend/docker-compose.yml up -d` | 테스트 DB 기동 | 최초 실패 뒤 성공 | 샌드박스 Docker pipe 거부 뒤 허용된 환경에서 `o2o-catalog-mysql Running` | 직접 실행 |
| `$env:JAVA_HOME='C:/Users/user/.jdks/ms-21.0.11'; .\\backend\\gradlew.bat -p backend test` | 전체 테스트 | 최초 실패 뒤 종료 코드 0 | Gradle 잠금 파일 접근 거부 뒤 `BUILD SUCCESSFUL in 14s`. 단 `:test UP-TO-DATE`라 이번 명령에서 테스트 본문은 재실행되지 않았다. 현재 XML은 48파일 419건, 실패 0 | 직접 실행. 신선한 재실행은 아님 |
| `node harness/tools/check.mjs g1 <대상 테스트 소스> --type code --artifact <대응 XML>` 6회 | 대상 테스트 게이트 | 모두 PASS | 6파일 각각 검사 6건 통과. 합계 55건, 실패 0, 오류 0, 건너뜀 0 | 직접 실행. 현재 XML 사용 |
| step3, step4, step5, step6, step6-2 JUnit XML | 단계 결과 확인 | 통과 기록 | 각각 2, 2, 11, 39, 55건이고 실패와 오류 0 | 생성자 전달 결과 |
| step6 `http-calls.txt` | 실제 서버 기록 확인 | 통과 기록 | CAT-01, CAT-03, CAT-06, CAT-08과 일부 오류 응답 | 생성자 전달 결과 |
| booking-lifecycle step9 JUnit XML과 요약 | 기준 커밋 계열 결과 확인 | 통과 기록 | 48파일 419건, 실패 0. 카탈로그 몫 6파일 55건 | 생성자 전달 결과 |
| 같은 version 동시 PATCH | DB 낙관적 잠금 검증 | 미실행 | 승인된 전용 재현 명령이 없고 소스와 테스트 수정이 금지됐다. 정적 코드와 기존 테스트로 판정했다. | 미실행 |
| 미등록 지역, 공백 및 null, 미정의 Query HTTP 재현 | API 경계 검증 | 미실행 | 승인 명령에 서버 기동과 임의 HTTP 호출이 없다. 코드와 전달 기록으로 판정했다. | 미실행 |

## 검증 ID 결과

| ID | 통과 / 실패 / 미실행 | 근거 |
|---|---|---|
| T01 카탈로그 구간 | 실패 | 경로와 기존 XML은 통과했지만 version 충돌 보장, 등록 지역 확인, 일부 요청 검증이 명세와 다르다. |
| T02 카탈로그 구간 | 통과 | C3에서 다른 HOST의 PATCH는 404이고 값이 유지된다. CAT-07 다른 HOST 수정도 404다. |
| C1 | 통과 | 0과 음수를 거절하고 1과 100을 허용한다. |
| C2 | 통과 | 없는 propertyId 등록 거절과 존재하는 숙소 등록을 검사한다. |
| C3 | 통과 | 다른 HOST PATCH의 404와 수정 전 값 유지를 검사한다. |
| C4 | 실패 | 순차 stale version은 거절하지만 같은 version 동시 요청을 DB 커밋에서 거절할 장치와 테스트가 없다. |
| C5 | 통과 | Property PATCH에서 version만 보내거나 version을 생략하면 400이다. RoomType도 같은 DTO 가드다. |
| C6 | 통과 | body hostId는 400이고 소유자는 헤더 행위자로 저장된다. 남의 숙소 객실 등록은 NotFound다. |
| C7 | 통과 | page와 size 기본값, size 100, 101과 0 및 음수 page 경계를 검사한다. |
| C8 | 통과 | id 오름차순과 페이지 범위 초과 빈 items를 검사한다. |
| C9 | 통과 | 명세에 없는 JSON body 필드는 400이고 정상 body는 통과한다. |
| C10 | 통과 | 헤더 없음과 미등록 ID는 401, 역할 불일치는 403, 공개 조회는 200이다. |

통과 표시는 생성자 전달 XML과 현재 G1 XML 및 코드 대조를 근거로 한다. 이번 Gradle 명령은 UP-TO-DATE였으므로 새 테스트 실행 결과로 바꾸지 않았다.

## 보조 표

| 원본 번호 | 지적 ID | 심각도 | 위반 기준 |
|---|---|---|---|
| 1 | S9-R1-A-01 | 치명 | 동시성과 트랜잭션, 계약 하강 |
| 2 | S9-R1-A-02 | 보통 | API 계약 준수, 계약 하강 |
| 3 | S9-R1-A-03 | 보통 | API 계약 준수, 계약 하강 |
| 4 | S9-R1-A-04 | 확인필요 | API 계약 준수 |

보조 표 집계도 치명 1 / 보통 2 / 확인필요 1이다.

## 읽은 파일과 SHA256

요청과 기준 문서의 실제 SHA256은 아래와 같다. 요청문에 기록된 앞 16자리와 모두 일치했다.

```text
harness/out/task-S9-catalog-R1/eval-request-A.md 534e2ecc433a07eb756ba38759530040b82a0c9b38d212f8b7da942f773fca30
harness/out/task-S9-catalog-R1/eval-target-files.md c019bc515f6c68e21c621b54be091c310ca380734f2a7e92fdd9152caf9c3696
harness/project-sync/o2o-review-input-pack.md 1608942c0815f9113ef022e74131743bdca3cb30cfaebb277cb8e038cf0dc645
harness/prompts/eval-criteria-code.md c5ed835951fe09a5c015d50abbbacf9755af6ba8be807e1be1b9c0ba2cb1553d
harness/tasks/task-S9-catalog.md 7d6cc521e6df67e6b3c6d4b8959c7f5dc92d0b7834c395745215a3c057725581
document/11-o2o-api-spec.md 3f2613a77b6499039b6d8b2310504d7c37047005ae028f6568455472c1ea105b
document/06-2-o2o-aggregates.md 113c6734b5525e1d5a5b2116de1250232707bc0518b3470d133ec0b7c993cec9
document/06-4-o2o-contracts.md edacbe47d6d63e3f2b72849234a0a9dbd4dbd297a8eee7b1ea19d70ab5e3c3e3
```

평가 대상 코드 58개는 `eval-target-files.md`의 경로만 읽었다. 실제 SHA256 58개 모두 목록에 기록된 앞 16자리와 일치했고 불일치 수는 0건이다.

```text
backend/src/main/java/com/o2o/BackendApplication.java 63d8dd1cd4fc3522cf94b9b4116ac0e67b177c59ebf5cac20fd724f4d4a2e12d
backend/src/main/java/com/o2o/catalog/api/ApiTime.java 28bd8f96d46b14d794d00ceb8934d5efa05f608b0320aab9a5173592f433e4ed
backend/src/main/java/com/o2o/catalog/api/CatalogExceptionHandler.java 8b151584497dd6a3bb94552a57d5605435357c1dab0789c88937cc7f096922d1
backend/src/main/java/com/o2o/catalog/api/HostPropertyController.java 888443528f5a7b76fa8756e08ac6285c1b93db3c7ba6f2177ec82d6fc1f2a110
backend/src/main/java/com/o2o/catalog/api/PageResponse.java abb54058fe65684fb63d84b5e36549ffa6d77bdee8e30a7ae1580f825b53b252
backend/src/main/java/com/o2o/catalog/api/PropertyController.java e8a2d47f1695a43b7d4e6dd6e205caa544c0bbcbf6780aaa66abec14e353d7c7
backend/src/main/java/com/o2o/catalog/api/PropertyResponse.java f11a5e9d1d3576f253cfbdf30c3e1aba2d5f12749dee65b80a07b71d7efe1c4d
backend/src/main/java/com/o2o/catalog/api/RegisterPropertyRequest.java 8ee42f870f97b09dccd7a3f079d564cbb267ec3bf4bd297e2dcc11bff97246e3
backend/src/main/java/com/o2o/catalog/api/RegisterRoomTypeRequest.java fb15201b56b16ced7ef007e02e6078a11e6b3441f95739c1f0b7829242460ab4
backend/src/main/java/com/o2o/catalog/api/RoomTypeController.java cc8e89bfe80ef6ee298512d6bd52426b6dc51b034971d439202624b158780a41
backend/src/main/java/com/o2o/catalog/api/RoomTypeResponse.java be2e84cefe5e26293afd30ff70375207f3cca2056927f819985f1e88e8bbcf74
backend/src/main/java/com/o2o/catalog/api/UpdatePropertyRequest.java e050b49cfbcc350e28c1a44635eb00c94bd95ca8600744caeacd685189aa04ee
backend/src/main/java/com/o2o/catalog/api/UpdateRoomTypeRequest.java 09db488c44a7d42d8dee6ec1925946bf5fc636cb1f9b31bd9d187f2a8d29bac7
backend/src/main/java/com/o2o/catalog/api/package-info.java dfc17890ea93502e7a36f62e1b2226d2552235d73756d964b98c7f078aa9cfb0
backend/src/main/java/com/o2o/catalog/application/CatalogApplicationService.java 545943809fe3ba1ee2a05fb8c27213d325b6fcb47e37ff75e4b6d0fbb8e10a0e
backend/src/main/java/com/o2o/catalog/application/package-info.java 58767ad768782efa0ebb65e444c86312b47b789be7b442b31e1132c0d48527c3
backend/src/main/java/com/o2o/catalog/domain/Address.java 6e17f88d97c67d42290d373550a6c2c447674cccb62c405ee00198afcc4f84c2
backend/src/main/java/com/o2o/catalog/domain/InvalidOccupancyException.java 11279b782e8f50d10577b24cac4cb1620459ecc00d1c0f964ffd22e91342b256
backend/src/main/java/com/o2o/catalog/domain/Property.java e73cf584b8fbb36e4e9b40facb37dc21050daab6a0f2b0ea79945b1c24332e7c
backend/src/main/java/com/o2o/catalog/domain/PropertyNotFoundException.java 06087bac1c0fd05678e15fdb4dd2cbc471010708d7a6972a785e58bbfdbc8422
backend/src/main/java/com/o2o/catalog/domain/PropertyRegistered.java 0decefabfd0a435f00d3523a915b8e49ab8c0233f2d5d599057099e4455ff165
backend/src/main/java/com/o2o/catalog/domain/PropertyRepository.java 16e33bf00a476b7d701a57515a68bb84d0e66b9efe4df7e6501c0f5f695080db
backend/src/main/java/com/o2o/catalog/domain/PropertyUpdated.java f55adf67ef1edcb46e5f983b666b914c7850a3ad2beae77f68dc88a105c2ac06
backend/src/main/java/com/o2o/catalog/domain/Region.java 6ca668bd794aec193dfdfed692793c2859eb6c6b309c36aa9bb007f1d5e08f94
backend/src/main/java/com/o2o/catalog/domain/RoomType.java d1636604e01bc6c68d02c7ed2dbc7d8cf061ec9e6603a79e45f78f43aa11a9bd
backend/src/main/java/com/o2o/catalog/domain/RoomTypeNotFoundException.java d6150d55acd65a4eb32de1aaa370f929c424ea6e6141eee331ba646d2429df42
backend/src/main/java/com/o2o/catalog/domain/RoomTypeRegistered.java b8df7d1afa2adecb3d2f360b58858e5325ace9640f872b9cf25bd9ccae3a06a9
backend/src/main/java/com/o2o/catalog/domain/RoomTypeRepository.java 4bc9fb2944398c87d6d4a5185a6d78c8b78cbd83f884f0f78458f24381e2558e
backend/src/main/java/com/o2o/catalog/domain/RoomTypeUpdated.java b70df072e84ec19a661f29dd0a31d8e4f41fb393871e77405a37b5c947718c78
backend/src/main/java/com/o2o/catalog/domain/package-info.java ac3f49e1fb65f0269da4de1338617ea6f7b6c3d06bb8c760f73e8c7d25542d87
backend/src/main/java/com/o2o/catalog/infrastructure/JpaPropertyRepository.java 13e9019d988f9ddcb33ec6c991079a9b4fa969cf146d799733a44655e97c895d
backend/src/main/java/com/o2o/catalog/infrastructure/JpaRoomTypeRepository.java 1e8c20f8aedbe0af5c3fe41775678f88e33dc47c69102f4659d6feea14cc55d2
backend/src/main/java/com/o2o/catalog/infrastructure/PropertyJpaRepository.java d4031307cddeb409401d82f8fd8e2d29026828ea51b057bbdf938067969500f7
backend/src/main/java/com/o2o/catalog/infrastructure/RoomTypeJpaRepository.java f04526290d1cd17526be7b8be3beb551b121f2e3ba4312b49682b62549c25ae3
backend/src/main/java/com/o2o/catalog/infrastructure/SpringPage.java f50f329323f90e73ae11086023bf248222b29d796079596bb7ba1e7c4b6d0130
backend/src/main/java/com/o2o/catalog/infrastructure/package-info.java abc8e180c3586037ce52cafee6cd40fa52958f60be52a55056ae7e6a4e54ccd4
backend/src/main/java/com/o2o/catalog/package-info.java d66c8ce0b0e03e7bfa241a7a9d3f2b4bb624218576e4dc3fe908dea87e21e0f2
backend/src/main/java/com/o2o/shared/Actor.java 7f7e903397728fe8285472ab651e22c2591c367d4ce4fd0a723ae6f6b83d3597
backend/src/main/java/com/o2o/shared/ActorAccessDeniedException.java 0f965fc018766d2ae2bbc6cb4d8434f657860af62f1756be96e34261f949e3fc
backend/src/main/java/com/o2o/shared/ActorRegistry.java 9b0e000f80967372e7e9952caa9fa417d9d59d7e7307706a8cbe538109fe5e83
backend/src/main/java/com/o2o/shared/ActorRequiredException.java 25053079edb39cbe6a9fca94587fa86275535ec599b4e08105b8decb04449712
backend/src/main/java/com/o2o/shared/ActorResolver.java c40ff8e6d5a4547b1ae719cd8a804f3d3b84f45b1e633b2d916cceaf7cc82092
backend/src/main/java/com/o2o/shared/ActorRole.java 3676bc3d906743a24472bb5d81df6874c436090ac47d992ed7e9be90d581ce74
backend/src/main/java/com/o2o/shared/ErrorResponse.java c62440297db7ef67cb7134effa4ca62dc3a0171938ed14cbc1e36f8e167d8d2b
backend/src/main/java/com/o2o/shared/HostId.java 4aee2c3a7e435bc9d69b2e21bc084405177abd0b2f68fd2119669b1a62140541
backend/src/main/java/com/o2o/shared/PageQuery.java 6a328d62d68a47481e7cc449aa1b820f6ab1e68b4fd7542d35e09325a89c6f73
backend/src/main/java/com/o2o/shared/PageResult.java ebe19b03ab5c8263c6c22729b37c4fda7a16332729eddd6c2b8a8a3a2b5d1cc4
backend/src/main/java/com/o2o/shared/PropertyId.java e589d1cf50597b75016459aef6e8b073640db7cbb9d314a64770fb6e2bb5c4c9
backend/src/main/java/com/o2o/shared/RoomTypeId.java b0c4ca9058044fb9f546a2e3796e8bd6cc890b0f8a2931d466d1701c5ec1a659
backend/src/main/java/com/o2o/shared/SharedExceptionHandler.java bdde2ead0ce4c920f1678ffa2fb9bf9b4373ea9877065ef063d5db92a45b069b
backend/src/main/java/com/o2o/shared/VersionConflictException.java 68dfc27ff3545de6c9c7db2855d08bae359a6916f662bb7503cc02c58f1d7d5c
backend/src/main/java/com/o2o/shared/package-info.java 85dc998fba6350bee782ae19ca5658fc3dc227e90d470e9c70858deb91875a59
backend/src/test/java/com/o2o/BackendApplicationTests.java 164e5536219a1ab9e9170f6f9f413587fa691a55bb0cb9ed114fef0c61ff1200
backend/src/test/java/com/o2o/DatabaseConnectionTest.java 3883835eddebf1374dc3244e4a5d690b54d6ef6a25ffba27ddea691a747de7ed
backend/src/test/java/com/o2o/catalog/api/CatalogApiTest.java 700d593eed9f374178f807a22a1863d226547c872a6bd3ba80fbfb8b357f1230
backend/src/test/java/com/o2o/catalog/api/CatalogUpdateAndListApiTest.java 1273258f096fc74632b3470ac5627400627518295f4d55968510865cc53443fe
backend/src/test/java/com/o2o/catalog/application/CatalogApplicationServiceTest.java 33eb39e10a7f18a9d00f1ebe436fadf7933d9e2a5cdc0d559467a17223f7056d
backend/src/test/java/com/o2o/catalog/domain/RoomTypeTest.java 2c50f1c299c1dbc2e1f302bbd484538365941b1801b5482666c088a721199054
```

판정에 직접 쓴 실행 증거의 SHA256은 아래와 같다.

```text
harness/out/task-S9-catalog-R1/step6/http-calls.txt aeb062e8930fe5e0bc7489328f7276eb551a2ddb02ab944bd48deeb890d5f026
harness/out/task-S9-catalog-R1/step6-2/TEST-com.o2o.catalog.api.CatalogApiTest.xml 8dcc02778f4fff7f24297c548ed29177ee513160f04a1241b0ce29a91f645d62
harness/out/task-S9-catalog-R1/step6-2/TEST-com.o2o.catalog.api.CatalogUpdateAndListApiTest.xml 3157a2048afad28ab7ca91724d0c08fc05c79f3a231624ce463deafb3280d0fd
harness/out/task-S9-catalog-R1/step6-2/TEST-com.o2o.catalog.application.CatalogApplicationServiceTest.xml 8eed06c6e86377ed07cc625479d9b6f7f221fa9216db592ec77aa978c41b1e8d
harness/out/task-S9-catalog-R1/step6-2/TEST-com.o2o.catalog.domain.RoomTypeTest.xml b61f08a5c99b74d9a360ce21aa13e5d9ef2e28c8ac0341ea1724b48c455311f3
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.catalog.api.CatalogApiTest.xml 4f58a4c94be7be97451c18ac9f328d7579a9277ec4bcff7021e6098a3b14d92f
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.catalog.api.CatalogUpdateAndListApiTest.xml 4b34be5e4b1604cbd32f9d73900da8ce54aa4ba2514d744ab3748a3835c7cb19
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.catalog.application.CatalogApplicationServiceTest.xml 651d346877e991f8d56767bc1b1ce8d83605fe3a3ef4971c79d6eb7f6f0cb517
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.catalog.domain.RoomTypeTest.xml 6dcbcd559dec4e8d6f0e5a5130d53363a17295766b2c70da55ce9a6738c13d07
harness/out/task-S9-booking-lifecycle-R1/step9/test-summary.txt 70607c73cd1a146e231325b9559aeed610e3c4cbc5984a6bc3ee9748d2ffa98c
```

### 실제로 읽은 실행 증거 전체

```text
harness/out/task-S9-catalog-R1/step3/TEST-com.o2o.BackendApplicationTests.xml d09cc2e55f7d9844c3bd5eff60b6c2711af05fae85cfe19b0b28bd1b12f43a8c
harness/out/task-S9-catalog-R1/step3/TEST-com.o2o.DatabaseConnectionTest.xml ec4b6f3dda3bb7aee635349cc34eb206e4aead705ba4c45eb2f70ffc7cee7ad0
harness/out/task-S9-catalog-R1/step4/TEST-com.o2o.BackendApplicationTests.xml 2b4aa60cad0a57ea516d17fb13dc469b6932aef89bf799e12eb8cad7dc9fecd7
harness/out/task-S9-catalog-R1/step4/TEST-com.o2o.DatabaseConnectionTest.xml 3f6616e1cd245c8383356211523a3bdbbb79e9cf35944cd92a3a04f52db023a5
harness/out/task-S9-catalog-R1/step5/TEST-com.o2o.BackendApplicationTests.xml aa865ce698b1ecba9a24a9ef23b8041ea8e9614affb1d5fd2c51287714d3a7d4
harness/out/task-S9-catalog-R1/step5/TEST-com.o2o.catalog.application.CatalogApplicationServiceTest.xml cb9be8ba27e19dd4fc8610e15529d928a756fad605c1a1e73b9c67aaf7fe0598
harness/out/task-S9-catalog-R1/step5/TEST-com.o2o.catalog.domain.RoomTypeTest.xml d00ef3e720cbfc0249f740c259a93052a96a371cff2db4b08dbdc6dab25c4f61
harness/out/task-S9-catalog-R1/step5/TEST-com.o2o.DatabaseConnectionTest.xml 5765bb1d520e89c83e36bc21aff8041e811b7e39a25e5a528a2b2929345677d2
harness/out/task-S9-catalog-R1/step6/http-calls.txt aeb062e8930fe5e0bc7489328f7276eb551a2ddb02ab944bd48deeb890d5f026
harness/out/task-S9-catalog-R1/step6/TEST-com.o2o.BackendApplicationTests.xml 6c0e6a498442a1b0844ad168fedf36b6aacf6e475f60280baa6f40b5319bcbab
harness/out/task-S9-catalog-R1/step6/TEST-com.o2o.catalog.api.CatalogApiTest.xml 94ed1c449764c53ca30c6032a513cf548e50b0424e22ea20de68f2cce2d2ceab
harness/out/task-S9-catalog-R1/step6/TEST-com.o2o.catalog.application.CatalogApplicationServiceTest.xml b9881debe746684bbcc843fbb76c7b0d52f7c0321e8e01a219677bffdcdad688
harness/out/task-S9-catalog-R1/step6/TEST-com.o2o.catalog.domain.RoomTypeTest.xml a72231e24716aa19fd605f11622cbf9dadbbde40f0bb5ec8184f4800f36186d8
harness/out/task-S9-catalog-R1/step6/TEST-com.o2o.DatabaseConnectionTest.xml d9e1faaa2cfa48370910ad48ad5d11cced32d070b0d1d577aead482c651eb401
harness/out/task-S9-catalog-R1/step6-2/TEST-com.o2o.BackendApplicationTests.xml ad29ccda493adcd6a84d105ebac234dfe72ad24bb3d4ae2848c1aae9db07f330
harness/out/task-S9-catalog-R1/step6-2/TEST-com.o2o.catalog.api.CatalogApiTest.xml 8dcc02778f4fff7f24297c548ed29177ee513160f04a1241b0ce29a91f645d62
harness/out/task-S9-catalog-R1/step6-2/TEST-com.o2o.catalog.api.CatalogUpdateAndListApiTest.xml 3157a2048afad28ab7ca91724d0c08fc05c79f3a231624ce463deafb3280d0fd
harness/out/task-S9-catalog-R1/step6-2/TEST-com.o2o.catalog.application.CatalogApplicationServiceTest.xml 8eed06c6e86377ed07cc625479d9b6f7f221fa9216db592ec77aa978c41b1e8d
harness/out/task-S9-catalog-R1/step6-2/TEST-com.o2o.catalog.domain.RoomTypeTest.xml b61f08a5c99b74d9a360ce21aa13e5d9ef2e28c8ac0341ea1724b48c455311f3
harness/out/task-S9-catalog-R1/step6-2/TEST-com.o2o.DatabaseConnectionTest.xml c7dd416ac2d786e9a7cdacf672ca2ad94d932c3352b13b5481909207ac716624
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.BackendApplicationTests.xml 372a6ccfa5d1feb8883ccb8bf54dc87da48307d994cb5f4f8b68f20ec12e6340
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.booking.api.BookingApiTest.xml adc671709d90d985fbf2ae307bbeeda472d6027fc6eb10bff4e1ca97f4b860a6
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.booking.api.BookingCancelApiTest.xml 7fd48dbfc05232ec4cb302dcabc57e46e44c6aa354f2478464ea0d12ff51b69e
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.booking.api.BookingPaymentApiTest.xml abd3a827f614fc958ae660d32e4ff26fee47f04baecb4ef6959b81ca9ea46f80
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.booking.api.BookingQueryApiTest.xml fe81df81c98043bff749632eb2fd10d9805ebdfd30132efb635e67dd2b5795bb
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.booking.api.RequestBookingRequestTest.xml c1c39cc1e9da789b447d91f789bee41a44e7ecd6b1c2c73c75af23eddc25aa04
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.booking.application.ApprovalLossRecoveryTest.xml 4c403910cc8d614e308d5ff913395a5573ee604068a3d5f3df4e4be522500942
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.booking.application.BookingApplicationServiceTest.xml 344bf6b0df0ce4d0951d56c91af39ca6b5483bb0fe4adfce9a1d5488d2df5917
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.booking.application.BookingEventTest.xml 0093ff26613c5f01acb0566da4881d0d03bdb79df22745d7ae350812710e07bd
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.booking.application.BookingLifecycleEventTest.xml 90ad3779f77bbae002705080fead02b4b35bfbde310c8b5736d7092a0e79b1f3
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.booking.application.BookingLockContentionTest.xml c63f06c6747ee5f896647966dd9d6744cac0c5891b1ec1ddea92d7023f528240
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.booking.application.BookingPaymentServiceTest.xml 99d998d93af24c28ce39f69876bfd2b629754be8daef6d1143abaf7321539662
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.booking.application.CancelBookingTest.xml b840e44461378f68412188455cace677cc63e59e961e812e004af1a60501326c
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.booking.application.ExpireDueBookingsTest.xml 8ebddd5d1d5c7e9b7adb2843e7979083cd39b92df03ef062c77d916fe39a6d5b
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.booking.application.PaymentOutcomeServiceTest.xml 0cd4fe91b9d8c675fd59f5b62d6c30c3c967bbaa36d4b8c9e07037f81b461b8d
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.booking.domain.BookingTest.xml e4ed77ebd56f9c8732dff4ffa396836a7999fecc992ef68cccc29a82c3c7bca7
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.booking.domain.BookingTransitionTest.xml 743ac31cff6c3d54432e95878d07082135666fdcf630e793e653d08d2261f096
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.booking.domain.IdempotencyRecordTest.xml e0e73a026627b484875e2deb592549290eda88b7a727fc6b34d6ded34b2d14ea
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.booking.domain.PriceSnapshotTest.xml 8eaca560c667d8f3aa1331beb90741d9dc01ffe8d3f7ed70f2b4437765b36155
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.booking.domain.StayPeriodTest.xml da85007013b137bcfd83e48717e865b8c4858c201d58c89318b045f8b8e7d33c
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.booking.infrastructure.PricingPriceQuoteAdapterTest.xml bce651566a8425a8066747605d77f6d759ce6e4806637aac4d655d1c1fdef3d0
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.catalog.api.CatalogApiTest.xml 4f58a4c94be7be97451c18ac9f328d7579a9277ec4bcff7021e6098a3b14d92f
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.catalog.api.CatalogUpdateAndListApiTest.xml 4b34be5e4b1604cbd32f9d73900da8ce54aa4ba2514d744ab3748a3835c7cb19
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.catalog.application.CatalogApplicationServiceTest.xml 651d346877e991f8d56767bc1b1ce8d83605fe3a3ef4971c79d6eb7f6f0cb517
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.catalog.domain.RoomTypeTest.xml 6dcbcd559dec4e8d6f0e5a5130d53363a17295766b2c70da55ce9a6738c13d07
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.DatabaseConnectionTest.xml eceff9f7cf531536daa180181c13669f85f68e364ae95a45230141a2cb56adc1
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.inventory.api.InventoryApiTest.xml e6162f2975be30a7ee226d66d621d8900fe9ec4fbe2634d7a9cb51ac33832c6d
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.inventory.api.InventoryQueryApiTest.xml decfc1fedc2e2161d69aab3a831eb7859642aa7b4dce3c49641fdb862b5ef705
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.inventory.application.InventoryApplicationServiceTest.xml 6262f9ca16d3ea3f6c05e95590ca39e3b5b80eaad86276f944573f1908f2069a
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.inventory.application.InventoryEventTest.xml 55602ffe87495f7536228e9a920d01032e9d35a4fdf977fe666c3d9f091edf89
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.inventory.domain.DailyInventoryAllocationTest.xml 186ddc394154c532d066e328f603c848cc04686ab0fc6e47d6782aa08a7ed38b
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.inventory.domain.DailyInventoryTest.xml 1d15f44afc7d31f0dd44badc103a02840ddf45bc589aeafdebf73fff80ae3b2a
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.inventory.domain.DailyRateTest.xml 32f4fe26032fb47f008e2dcff70151520ea77fbca464725bc0afc2052b1f8b1c
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.inventory.domain.InventoryAllocationServiceTest.xml 7ad65b7bac99d69e659a392a00319e30cd65d7a6c33c770653dba687adc4922d
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.payment.api.DevProfileBoundaryTest.xml 85e512343a4854f35e0c77b39128bd2b42c3493c0b7d7552398b5b02584bc97a
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.payment.api.MockPaymentEventApiTest.xml 9f3eb94ddf53bbb3263f8b91b2b66c1d1aa3c0c0342f1a8d0e3aed02242a401a
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.payment.application.PaymentApplicationServiceTest.xml ce7ceea5e95e2598580af8faadd3acf7ea7d494d35ed1f27e5f6da9583ae88f9
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.payment.application.PaymentAutoResultTest.xml 4b3e29c25e97fb5a5b0affa1877d79e092534f4f780a82065e0b69195b4c84d4
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.payment.application.PaymentEventTest.xml fd777c3e8b47348d06ad67c4bf5991a80d88a3f0265b3d5340a0b0182dcb1c7d
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.payment.domain.PaymentTest.xml c34af0fbad273880f24dcf7301365d7976104a41da685751b9a9e83c2aab90e9
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.promotion.api.PromotionApiTest.xml 123621325b337ee1014c3f0e3fa69e92b2b0c346699ab509ddc3bbf79fd293aa
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.promotion.application.PromotionApplicationServiceTest.xml 97421fa2db80d1fd3ceee95e657cdec0df3200869b26083567778b28f2ff4561
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.promotion.application.PromotionEventTest.xml aaf306855ddf045357ff250228bf155f3c5bc10d7b40332f9f1a317f1e90f34d
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.promotion.domain.ConditionTest.xml b3cca33c480cd1fa53fea64ffb41b0a962575df69156fa486fdd25a6212d09c2
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.promotion.domain.PriceCalculationTest.xml 3ce73962e91208b4ec65e7b81543e86c06f23ce1e8ed6d22543fc1bec33ded6c
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.promotion.domain.PromotionTest.xml 47d7bbb8381308f90aac8d678e16c74771c7e0a9a229d1b59c0449133e8eaa5c
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.search.api.SearchApiTest.xml 72a2265482a9d072eb9461dce0b5392d6eb967672d4d75f02b9d71787e8510c3
harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.shared.MoneyTest.xml 5cf36cfdc57159041e1248e12186f576a8e4c5563321f8c28105639cc412bba3
harness/out/task-S9-booking-lifecycle-R1/step9/test-summary.txt 70607c73cd1a146e231325b9559aeed610e3c4cbc5984a6bc3ee9748d2ffa98c
backend/build/test-results/test/TEST-com.o2o.BackendApplicationTests.xml 372a6ccfa5d1feb8883ccb8bf54dc87da48307d994cb5f4f8b68f20ec12e6340
backend/build/test-results/test/TEST-com.o2o.booking.api.BookingApiTest.xml adc671709d90d985fbf2ae307bbeeda472d6027fc6eb10bff4e1ca97f4b860a6
backend/build/test-results/test/TEST-com.o2o.booking.api.BookingCancelApiTest.xml 7fd48dbfc05232ec4cb302dcabc57e46e44c6aa354f2478464ea0d12ff51b69e
backend/build/test-results/test/TEST-com.o2o.booking.api.BookingPaymentApiTest.xml abd3a827f614fc958ae660d32e4ff26fee47f04baecb4ef6959b81ca9ea46f80
backend/build/test-results/test/TEST-com.o2o.booking.api.BookingQueryApiTest.xml fe81df81c98043bff749632eb2fd10d9805ebdfd30132efb635e67dd2b5795bb
backend/build/test-results/test/TEST-com.o2o.booking.api.RequestBookingRequestTest.xml c1c39cc1e9da789b447d91f789bee41a44e7ecd6b1c2c73c75af23eddc25aa04
backend/build/test-results/test/TEST-com.o2o.booking.application.ApprovalLossRecoveryTest.xml 4c403910cc8d614e308d5ff913395a5573ee604068a3d5f3df4e4be522500942
backend/build/test-results/test/TEST-com.o2o.booking.application.BookingApplicationServiceTest.xml 344bf6b0df0ce4d0951d56c91af39ca6b5483bb0fe4adfce9a1d5488d2df5917
backend/build/test-results/test/TEST-com.o2o.booking.application.BookingEventTest.xml 0093ff26613c5f01acb0566da4881d0d03bdb79df22745d7ae350812710e07bd
backend/build/test-results/test/TEST-com.o2o.booking.application.BookingLifecycleEventTest.xml 90ad3779f77bbae002705080fead02b4b35bfbde310c8b5736d7092a0e79b1f3
backend/build/test-results/test/TEST-com.o2o.booking.application.BookingLockContentionTest.xml c63f06c6747ee5f896647966dd9d6744cac0c5891b1ec1ddea92d7023f528240
backend/build/test-results/test/TEST-com.o2o.booking.application.BookingPaymentServiceTest.xml 99d998d93af24c28ce39f69876bfd2b629754be8daef6d1143abaf7321539662
backend/build/test-results/test/TEST-com.o2o.booking.application.CancelBookingTest.xml b840e44461378f68412188455cace677cc63e59e961e812e004af1a60501326c
backend/build/test-results/test/TEST-com.o2o.booking.application.ExpireDueBookingsTest.xml 8ebddd5d1d5c7e9b7adb2843e7979083cd39b92df03ef062c77d916fe39a6d5b
backend/build/test-results/test/TEST-com.o2o.booking.application.PaymentOutcomeServiceTest.xml 0cd4fe91b9d8c675fd59f5b62d6c30c3c967bbaa36d4b8c9e07037f81b461b8d
backend/build/test-results/test/TEST-com.o2o.booking.domain.BookingTest.xml e4ed77ebd56f9c8732dff4ffa396836a7999fecc992ef68cccc29a82c3c7bca7
backend/build/test-results/test/TEST-com.o2o.booking.domain.BookingTransitionTest.xml 743ac31cff6c3d54432e95878d07082135666fdcf630e793e653d08d2261f096
backend/build/test-results/test/TEST-com.o2o.booking.domain.IdempotencyRecordTest.xml e0e73a026627b484875e2deb592549290eda88b7a727fc6b34d6ded34b2d14ea
backend/build/test-results/test/TEST-com.o2o.booking.domain.PriceSnapshotTest.xml 8eaca560c667d8f3aa1331beb90741d9dc01ffe8d3f7ed70f2b4437765b36155
backend/build/test-results/test/TEST-com.o2o.booking.domain.StayPeriodTest.xml da85007013b137bcfd83e48717e865b8c4858c201d58c89318b045f8b8e7d33c
backend/build/test-results/test/TEST-com.o2o.booking.infrastructure.PricingPriceQuoteAdapterTest.xml bce651566a8425a8066747605d77f6d759ce6e4806637aac4d655d1c1fdef3d0
backend/build/test-results/test/TEST-com.o2o.catalog.api.CatalogApiTest.xml 4f58a4c94be7be97451c18ac9f328d7579a9277ec4bcff7021e6098a3b14d92f
backend/build/test-results/test/TEST-com.o2o.catalog.api.CatalogUpdateAndListApiTest.xml 4b34be5e4b1604cbd32f9d73900da8ce54aa4ba2514d744ab3748a3835c7cb19
backend/build/test-results/test/TEST-com.o2o.catalog.application.CatalogApplicationServiceTest.xml 651d346877e991f8d56767bc1b1ce8d83605fe3a3ef4971c79d6eb7f6f0cb517
backend/build/test-results/test/TEST-com.o2o.catalog.domain.RoomTypeTest.xml 6dcbcd559dec4e8d6f0e5a5130d53363a17295766b2c70da55ce9a6738c13d07
backend/build/test-results/test/TEST-com.o2o.DatabaseConnectionTest.xml eceff9f7cf531536daa180181c13669f85f68e364ae95a45230141a2cb56adc1
backend/build/test-results/test/TEST-com.o2o.inventory.api.InventoryApiTest.xml e6162f2975be30a7ee226d66d621d8900fe9ec4fbe2634d7a9cb51ac33832c6d
backend/build/test-results/test/TEST-com.o2o.inventory.api.InventoryQueryApiTest.xml decfc1fedc2e2161d69aab3a831eb7859642aa7b4dce3c49641fdb862b5ef705
backend/build/test-results/test/TEST-com.o2o.inventory.application.InventoryApplicationServiceTest.xml 6262f9ca16d3ea3f6c05e95590ca39e3b5b80eaad86276f944573f1908f2069a
backend/build/test-results/test/TEST-com.o2o.inventory.application.InventoryEventTest.xml 55602ffe87495f7536228e9a920d01032e9d35a4fdf977fe666c3d9f091edf89
backend/build/test-results/test/TEST-com.o2o.inventory.domain.DailyInventoryAllocationTest.xml 186ddc394154c532d066e328f603c848cc04686ab0fc6e47d6782aa08a7ed38b
backend/build/test-results/test/TEST-com.o2o.inventory.domain.DailyInventoryTest.xml 1d15f44afc7d31f0dd44badc103a02840ddf45bc589aeafdebf73fff80ae3b2a
backend/build/test-results/test/TEST-com.o2o.inventory.domain.DailyRateTest.xml 32f4fe26032fb47f008e2dcff70151520ea77fbca464725bc0afc2052b1f8b1c
backend/build/test-results/test/TEST-com.o2o.inventory.domain.InventoryAllocationServiceTest.xml 7ad65b7bac99d69e659a392a00319e30cd65d7a6c33c770653dba687adc4922d
backend/build/test-results/test/TEST-com.o2o.payment.api.DevProfileBoundaryTest.xml 85e512343a4854f35e0c77b39128bd2b42c3493c0b7d7552398b5b02584bc97a
backend/build/test-results/test/TEST-com.o2o.payment.api.MockPaymentEventApiTest.xml 9f3eb94ddf53bbb3263f8b91b2b66c1d1aa3c0c0342f1a8d0e3aed02242a401a
backend/build/test-results/test/TEST-com.o2o.payment.application.PaymentApplicationServiceTest.xml ce7ceea5e95e2598580af8faadd3acf7ea7d494d35ed1f27e5f6da9583ae88f9
backend/build/test-results/test/TEST-com.o2o.payment.application.PaymentAutoResultTest.xml 4b3e29c25e97fb5a5b0affa1877d79e092534f4f780a82065e0b69195b4c84d4
backend/build/test-results/test/TEST-com.o2o.payment.application.PaymentEventTest.xml fd777c3e8b47348d06ad67c4bf5991a80d88a3f0265b3d5340a0b0182dcb1c7d
backend/build/test-results/test/TEST-com.o2o.payment.domain.PaymentTest.xml c34af0fbad273880f24dcf7301365d7976104a41da685751b9a9e83c2aab90e9
backend/build/test-results/test/TEST-com.o2o.promotion.api.PromotionApiTest.xml 123621325b337ee1014c3f0e3fa69e92b2b0c346699ab509ddc3bbf79fd293aa
backend/build/test-results/test/TEST-com.o2o.promotion.application.PromotionApplicationServiceTest.xml 97421fa2db80d1fd3ceee95e657cdec0df3200869b26083567778b28f2ff4561
backend/build/test-results/test/TEST-com.o2o.promotion.application.PromotionEventTest.xml aaf306855ddf045357ff250228bf155f3c5bc10d7b40332f9f1a317f1e90f34d
backend/build/test-results/test/TEST-com.o2o.promotion.domain.ConditionTest.xml b3cca33c480cd1fa53fea64ffb41b0a962575df69156fa486fdd25a6212d09c2
backend/build/test-results/test/TEST-com.o2o.promotion.domain.PriceCalculationTest.xml 3ce73962e91208b4ec65e7b81543e86c06f23ce1e8ed6d22543fc1bec33ded6c
backend/build/test-results/test/TEST-com.o2o.promotion.domain.PromotionTest.xml 47d7bbb8381308f90aac8d678e16c74771c7e0a9a229d1b59c0449133e8eaa5c
backend/build/test-results/test/TEST-com.o2o.search.api.SearchApiTest.xml 72a2265482a9d072eb9461dce0b5392d6eb967672d4d75f02b9d71787e8510c3
backend/build/test-results/test/TEST-com.o2o.shared.MoneyTest.xml 5cf36cfdc57159041e1248e12186f576a8e4c5563321f8c28105639cc412bba3
```

단계별 JUnit XML과 현재 `backend/build/test-results/test` XML을 전부 읽어 집계했다. 파일별 해시 계산 중 오류는 없었다. 기존 A와 B 리포트, 이전 평가 작업, 과거 리포트, `step9-verification.md`, `harness/reviews/`의 다른 파일은 읽지 않았다.
