package com.o2o.catalog.api;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.o2o.catalog.application.CatalogApplicationService;
import com.o2o.catalog.domain.RoomType;
import com.o2o.shared.Actor;
import com.o2o.shared.ActorResolver;
import com.o2o.shared.ActorRole;
import com.o2o.shared.PageQuery;
import com.o2o.shared.PropertyId;
import com.o2o.shared.RoomTypeId;

import jakarta.validation.Valid;

/**
 * CAT-06과 CAT-08. 설계 근거: 11 객실 타입 절.
 *
 * 두 경로의 앞부분이 다르다. 등록은 숙소 밑에 걸리고 조회는 객실 타입 단독이다. 그래서
 * 클래스 수준 RequestMapping을 두지 않고 메서드마다 전체 경로를 적는다. 11이 그렇게 적는다.
 */
@RestController
public class RoomTypeController {

    private final CatalogApplicationService catalogApplicationService;
    private final ActorResolver actorResolver;

    public RoomTypeController(CatalogApplicationService catalogApplicationService,
                              ActorResolver actorResolver) {
        this.catalogApplicationService = catalogApplicationService;
        this.actorResolver = actorResolver;
    }

    /**
     * CAT-06 객실 타입 등록. 인증 HOST이고 대상 숙소의 소유자여야 한다.
     * 소유자 검사는 앱 서비스가 한다. 06-4 1-4가 컨텍스트를 넘는 선행조건을 그쪽에 둔다.
     */
    @PostMapping("/api/v1/properties/{propertyId}/room-types")
    public ResponseEntity<RoomTypeResponse> register(
            @RequestHeader(value = "X-Dev-Actor-Id", required = false) String actorId,
            @PathVariable String propertyId,
            @Valid @RequestBody RegisterRoomTypeRequest request) {
        Actor actor = actorResolver.require(actorId, ActorRole.HOST);
        RoomType roomType = catalogApplicationService.registerRoomType(
                actor.asHostId(), PropertyId.of(propertyId), request.name(),
                request.maxOccupancy(), request.descriptionOrEmpty());
        return ResponseEntity
                .created(URI.create("/api/v1/room-types/" + roomType.id().value()))
                .body(RoomTypeResponse.from(roomType));
    }

    /** CAT-08 객실 타입 상세 조회. 인증 불필요 */
    @GetMapping("/api/v1/room-types/{roomTypeId}")
    public RoomTypeResponse get(@PathVariable String roomTypeId) {
        return RoomTypeResponse.from(
                catalogApplicationService.getRoomType(RoomTypeId.of(roomTypeId)));
    }

    /**
     * CAT-07 객실 타입 수정. 인증 HOST이고 부모 숙소의 소유자여야 한다.
     * 최대 인원 하향을 막지 않는다. 11 CAT-07 처리 규칙이 신규 예약에만 적용한다고 적는다.
     */
    @PatchMapping("/api/v1/room-types/{roomTypeId}")
    public RoomTypeResponse update(
            @RequestHeader(value = "X-Dev-Actor-Id", required = false) String actorId,
            @PathVariable String roomTypeId,
            @Valid @RequestBody UpdateRoomTypeRequest request) {
        Actor actor = actorResolver.require(actorId, ActorRole.HOST);
        return RoomTypeResponse.from(catalogApplicationService.updateRoomType(
                actor.asHostId(), RoomTypeId.of(roomTypeId), request.version(),
                request.name(), request.maxOccupancy(), request.description()));
    }

    /**
     * CAT-09 숙소의 객실 타입 목록 조회. 인증 불필요.
     * 없는 숙소 아래를 조회하면 빈 목록이 아니라 404다. 11 인증과 접근 제어가 중첩 경로의
     * 부모와 자식 관계도 확인하라고 적는다.
     */
    @GetMapping("/api/v1/properties/{propertyId}/room-types")
    public PageResponse<RoomTypeResponse> list(
            @PathVariable String propertyId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return PageResponse.from(
                catalogApplicationService.listRoomTypes(PropertyId.of(propertyId), PageQuery.of(page, size)),
                RoomTypeResponse::from);
    }
}
