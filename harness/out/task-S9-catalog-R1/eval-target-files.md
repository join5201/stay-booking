# task-S9-catalog 평가 대상 코드 목록

최초 작성: 2026-09-14
최종 갱신: 2026-09-14

계약 5절 평가 대상 코드 행이 가리키는 파일이다. 계약 표 한 칸에 쉰 줄을 넣을 수 없어 목록을 여기 둔다. 라운드 mvp-eval-2026-09-14의 쌍 1(숙소와 객실) 몫이고 라운드의 조건은 harness/out/mvp-eval-2026-09-14/README.md에 있다. 계약 7절 D-3 나(그 바퀴는 게이트만)를 라운드 README 5절 D-1 나가 덮어 이 묶음이 R1로 들어왔다.

## 1. 버전

| 항목 | 값 |
|---|---|
| 브랜치 | main |
| 기준 커밋 | 1bdadfe5a78722f1e708d96bafc0f728eddfc4a1 (PR 138 병합, 2026-09-13 23:53) |
| 이 묶음의 backend 커밋 | PR 33과 35(feat/task-s9-catalog-skeleton)로 들어온 것. 뼈대 반입 265f767부터 목록 API 947b6c6까지. 뒤에 다른 묶음 셋이 이 묶음 파일 여덟을 고쳤다(2절) |
| 뽑은 방식 | 패키지 단위. catalog 패키지 전체, shared 중 git 이력의 첫 커밋이 이 묶음 PR 안인 파일, 뼈대 BackendApplication.java와 루트 테스트 둘. 표는 harness/out/mvp-eval-2026-09-14/build-target-lists.mjs catalog 가 기준 커밋에서 낸 것이다 |

커밋 범위로 뽑지 않은 이유는 라운드 README 3절에 있다. 묶음 넷이 main에 섞여 들어 범위가 겹친다.

## 2. 파일 표

경로는 저장소 루트 기준이다. sha256은 앞 16자리이고 기준 커밋의 작업 트리 값이다. 만든 묶음은 그 파일을 처음 더한 커밋의 PR, 고친 묶음은 그 뒤 그 파일을 고친 다른 PR이다. 테스트 표의 건수는 main 전체 실행 사본(harness/out/task-S9-booking-lifecycle-R1/step9의 JUnit XML)에서 읽었다.

다른 묶음이 고친 파일은 여덟이다. catalog의 리포지토리 여섯(PropertyRepository, RoomTypeRepository와 그 JPA 구현 넷)은 프로모션과 검색 묶음이 검색용 읽기 메서드 둘을 더했다(888b278). shared의 ActorRegistry는 예약 1차가 GUEST 행위자를 더했고(3d84e83), ActorResolver는 결제가 dev 프로파일 판정을 더했다(e0cc55f). 평가자는 지금 모양을 판정한다. 지적이 그 자리에 떨어지면 그대로 적고, 어느 묶음이 반영할지는 결정표가 가른다.

### backend/src/main/java/com/o2o 1개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/BackendApplication.java | sha256:63d8dd1cd4fc3522 | 숙소 | 없음 | 265f767 |

### backend/src/main/java/com/o2o/catalog/api 13개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/catalog/api/ApiTime.java | sha256:28bd8f96d46b14d7 | 숙소 | 없음 | a83a975 |
| backend/src/main/java/com/o2o/catalog/api/CatalogExceptionHandler.java | sha256:8b151584497dd6a3 | 숙소 | 없음 | a83a975 |
| backend/src/main/java/com/o2o/catalog/api/HostPropertyController.java | sha256:888443528f5a7b76 | 숙소 | 없음 | 400dc03 |
| backend/src/main/java/com/o2o/catalog/api/PageResponse.java | sha256:abb54058fe65684f | 숙소 | 없음 | 400dc03 |
| backend/src/main/java/com/o2o/catalog/api/PropertyController.java | sha256:e8a2d47f1695a43b | 숙소 | 없음 | 400dc03 |
| backend/src/main/java/com/o2o/catalog/api/PropertyResponse.java | sha256:f11a5e9d1d3576f2 | 숙소 | 없음 | a83a975 |
| backend/src/main/java/com/o2o/catalog/api/RegisterPropertyRequest.java | sha256:8ee42f870f97b09d | 숙소 | 없음 | a83a975 |
| backend/src/main/java/com/o2o/catalog/api/RegisterRoomTypeRequest.java | sha256:fb15201b56b16ced | 숙소 | 없음 | a83a975 |
| backend/src/main/java/com/o2o/catalog/api/RoomTypeController.java | sha256:cc8e89bfe80ef6ee | 숙소 | 없음 | 400dc03 |
| backend/src/main/java/com/o2o/catalog/api/RoomTypeResponse.java | sha256:be2e84cefe5e2629 | 숙소 | 없음 | a83a975 |
| backend/src/main/java/com/o2o/catalog/api/UpdatePropertyRequest.java | sha256:e050b49cfbcc350e | 숙소 | 없음 | 400dc03 |
| backend/src/main/java/com/o2o/catalog/api/UpdateRoomTypeRequest.java | sha256:09db488c44a7d42d | 숙소 | 없음 | 400dc03 |
| backend/src/main/java/com/o2o/catalog/api/package-info.java | sha256:dfc17890ea93502e | 숙소 | 없음 | e37d0a2 |

### backend/src/main/java/com/o2o/catalog/application 2개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/catalog/application/CatalogApplicationService.java | sha256:545943809fe3ba1e | 숙소 | 없음 | 400dc03 |
| backend/src/main/java/com/o2o/catalog/application/package-info.java | sha256:58767ad768782efa | 숙소 | 없음 | e37d0a2 |

### backend/src/main/java/com/o2o/catalog/domain 14개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/catalog/domain/Address.java | sha256:6e17f88d97c67d42 | 숙소 | 없음 | 9e51a29 |
| backend/src/main/java/com/o2o/catalog/domain/InvalidOccupancyException.java | sha256:11279b782e8f50d1 | 숙소 | 없음 | 9e51a29 |
| backend/src/main/java/com/o2o/catalog/domain/Property.java | sha256:e73cf584b8fbb36e | 숙소 | 없음 | 400dc03 |
| backend/src/main/java/com/o2o/catalog/domain/PropertyNotFoundException.java | sha256:06087bac1c0fd056 | 숙소 | 없음 | 9e51a29 |
| backend/src/main/java/com/o2o/catalog/domain/PropertyRegistered.java | sha256:0decefabfd0a435f | 숙소 | 없음 | e2a0ada |
| backend/src/main/java/com/o2o/catalog/domain/PropertyRepository.java | sha256:16e33bf00a476b7d | 숙소 | 프로모션과 검색 | 888b278 |
| backend/src/main/java/com/o2o/catalog/domain/PropertyUpdated.java | sha256:f55adf67ef1edcb4 | 숙소 | 없음 | 400dc03 |
| backend/src/main/java/com/o2o/catalog/domain/Region.java | sha256:6ca668bd794aec19 | 숙소 | 없음 | 9e51a29 |
| backend/src/main/java/com/o2o/catalog/domain/RoomType.java | sha256:d1636604e01bc6c6 | 숙소 | 없음 | 400dc03 |
| backend/src/main/java/com/o2o/catalog/domain/RoomTypeNotFoundException.java | sha256:d6150d55acd65a4e | 숙소 | 없음 | 9e51a29 |
| backend/src/main/java/com/o2o/catalog/domain/RoomTypeRegistered.java | sha256:b8df7d1afa2adecb | 숙소 | 없음 | e2a0ada |
| backend/src/main/java/com/o2o/catalog/domain/RoomTypeRepository.java | sha256:4bc9fb2944398c87 | 숙소 | 프로모션과 검색 | 888b278 |
| backend/src/main/java/com/o2o/catalog/domain/RoomTypeUpdated.java | sha256:b70df072e84ec19a | 숙소 | 없음 | 400dc03 |
| backend/src/main/java/com/o2o/catalog/domain/package-info.java | sha256:ac3f49e1fb65f026 | 숙소 | 없음 | e37d0a2 |

### backend/src/main/java/com/o2o/catalog/infrastructure 6개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/catalog/infrastructure/JpaPropertyRepository.java | sha256:13e9019d988f9ddc | 숙소 | 프로모션과 검색 | 888b278 |
| backend/src/main/java/com/o2o/catalog/infrastructure/JpaRoomTypeRepository.java | sha256:1e8c20f8aedbe0af | 숙소 | 프로모션과 검색 | 888b278 |
| backend/src/main/java/com/o2o/catalog/infrastructure/PropertyJpaRepository.java | sha256:d4031307cddeb409 | 숙소 | 프로모션과 검색 | 888b278 |
| backend/src/main/java/com/o2o/catalog/infrastructure/RoomTypeJpaRepository.java | sha256:f04526290d1cd175 | 숙소 | 프로모션과 검색 | 888b278 |
| backend/src/main/java/com/o2o/catalog/infrastructure/SpringPage.java | sha256:f50f329323f90e73 | 숙소 | 없음 | 400dc03 |
| backend/src/main/java/com/o2o/catalog/infrastructure/package-info.java | sha256:abc8e180c3586037 | 숙소 | 없음 | e37d0a2 |

### backend/src/main/java/com/o2o/catalog 1개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/catalog/package-info.java | sha256:d66c8ce0b0e03e7b | 숙소 | 없음 | e37d0a2 |

### backend/src/main/java/com/o2o/shared 15개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/shared/Actor.java | sha256:7f7e903397728fe8 | 숙소 | 없음 | b8cd38a |
| backend/src/main/java/com/o2o/shared/ActorAccessDeniedException.java | sha256:0f965fc018766d2a | 숙소 | 없음 | b8cd38a |
| backend/src/main/java/com/o2o/shared/ActorRegistry.java | sha256:9b0e000f80967372 | 숙소 | 예약 1차 | 3d84e83 |
| backend/src/main/java/com/o2o/shared/ActorRequiredException.java | sha256:25053079edb39cbe | 숙소 | 없음 | b8cd38a |
| backend/src/main/java/com/o2o/shared/ActorResolver.java | sha256:c40ff8e6d5a4547b | 숙소 | 결제 | e0cc55f |
| backend/src/main/java/com/o2o/shared/ActorRole.java | sha256:3676bc3d906743a2 | 숙소 | 없음 | b8cd38a |
| backend/src/main/java/com/o2o/shared/ErrorResponse.java | sha256:c62440297db7ef67 | 숙소 | 없음 | b8cd38a |
| backend/src/main/java/com/o2o/shared/HostId.java | sha256:4aee2c3a7e435bc9 | 숙소 | 없음 | 9e51a29 |
| backend/src/main/java/com/o2o/shared/PageQuery.java | sha256:6a328d62d68a4748 | 숙소 | 없음 | ab271e0 |
| backend/src/main/java/com/o2o/shared/PageResult.java | sha256:ebe19b03ab5c8263 | 숙소 | 없음 | ab271e0 |
| backend/src/main/java/com/o2o/shared/PropertyId.java | sha256:e589d1cf50597b75 | 숙소 | 없음 | 9e51a29 |
| backend/src/main/java/com/o2o/shared/RoomTypeId.java | sha256:b0c4ca9058044fb9 | 숙소 | 없음 | 9e51a29 |
| backend/src/main/java/com/o2o/shared/SharedExceptionHandler.java | sha256:bdde2ead0ce4c920 | 숙소 | 없음 | ab271e0 |
| backend/src/main/java/com/o2o/shared/VersionConflictException.java | sha256:68dfc27ff3545de6 | 숙소 | 없음 | ab271e0 |
| backend/src/main/java/com/o2o/shared/package-info.java | sha256:85dc998fba6350be | 숙소 | 없음 | e37d0a2 |

### 테스트 6개

| 경로 | sha256 | 건수 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|---|
| backend/src/test/java/com/o2o/BackendApplicationTests.java | sha256:164e5536219a1ab9 | 1 | 숙소 | 없음 | 265f767 |
| backend/src/test/java/com/o2o/DatabaseConnectionTest.java | sha256:3883835eddebf137 | 1 | 숙소 | 없음 | fd9c8f1 |
| backend/src/test/java/com/o2o/catalog/api/CatalogApiTest.java | sha256:700d593eed9f3741 | 24 | 숙소 | 없음 | 664c510 |
| backend/src/test/java/com/o2o/catalog/api/CatalogUpdateAndListApiTest.java | sha256:1273258f096fc746 | 16 | 숙소 | 없음 | 947b6c6 |
| backend/src/test/java/com/o2o/catalog/application/CatalogApplicationServiceTest.java | sha256:33eb39e10a7f18a9 | 9 | 숙소 | 없음 | 664c510 |
| backend/src/test/java/com/o2o/catalog/domain/RoomTypeTest.java | sha256:2c50f1c299c1dbc2 | 4 | 숙소 | 없음 | d95b09e |

## 3. 합계

프로덕션 52개다. catalog 36(api 13, application 2, domain 14, infrastructure 6, package-info 1), shared 15, 뼈대 1. 테스트 6파일 55건이다. catalog 4파일 53건(CatalogApiTest 24, CatalogUpdateAndListApiTest 16, CatalogApplicationServiceTest 9, RoomTypeTest 4)과 루트 2파일 2건(기동과 DB 연결). 저장소 전체는 56파일 419건이다.

## 4. 평가 대상이 아닌 것

| 무엇 | 왜 |
|---|---|
| inventory, promotion, search, booking, payment 패키지 | 다른 쌍의 몫이다. 라운드 README 3절 |
| shared 중 다른 묶음이 만든 아홉(Money, SeoulDate, ClockConfiguration, ApiDate, ApiTime, GuestCount, InvalidDateFormatException, PageResponse, PromotionId) | 그 묶음의 쌍이 본다 |
| 설정 파일 둘(backend/src/main/resources와 test/resources의 application.properties) | 뼈대가 만들었으나 이 계약 5절의 허용 입력이 아니다. 결제 쌍이 본다 |
| harness/out/task-S9-catalog-R1/ 아래 결과 사본과 plan-step3.md와 step9-verification.md | 산출물의 증거이지 평가 대상 코드가 아니다. step9-verification.md는 생성자의 자기 판정이라 읽지 않는다 |
| backend/CLAUDE.md, backend/AGENTS.md, build.gradle, docker-compose.yml | 코드가 아니다 |
| 이 계약 파일 harness/tasks/task-S9-catalog.md | 5절 표의 별도 행이다 |

## 5. 실행 결과 파일

| 단계 | 경로 | 무엇 |
|---|---|---|
| 3 | harness/out/task-S9-catalog-R1/step3/ | 기동과 DB 연결 |
| 4 | harness/out/task-S9-catalog-R1/step4/ | 도메인 |
| 5 | harness/out/task-S9-catalog-R1/step5/ | 불변식과 앱 서비스 |
| 6 | harness/out/task-S9-catalog-R1/step6/ | API 넷. http-calls.txt |
| 6-2 | harness/out/task-S9-catalog-R1/step6-2/ | API 다섯. JUnit XML만 |
| main 전체 | harness/out/task-S9-booking-lifecycle-R1/step9/ | JUnit XML 48개, 419건. 이 묶음 몫은 catalog 넷과 루트 둘 |

단계 사본은 그 단계 시점의 코드로 돈 결과다. 기준 커밋의 코드로 돈 것은 마지막 행이다.
