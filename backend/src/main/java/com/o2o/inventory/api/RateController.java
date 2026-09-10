package com.o2o.inventory.api;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.o2o.inventory.application.InventoryApplicationService;
import com.o2o.inventory.domain.DailyRate;
import com.o2o.shared.Actor;
import com.o2o.shared.ActorResolver;
import com.o2o.shared.ActorRole;
import com.o2o.shared.RoomTypeId;

import jakarta.validation.Valid;

/**
 * RATE-01과 RATE-02. 설계 근거: 11 요금 절.
 *
 * 재고와 컨트롤러를 가른 이유는 자원이 다르기 때문이다. 경로가 inventories와 rates로
 * 갈라지고 응답 모델도 다르다. 애그리거트가 둘인 것과 같은 결이다(06-2 1절).
 * 앱 서비스를 하나로 둔 것과 어긋나지 않는다. 그쪽은 트랜잭션 경계라 함께 묶이고
 * 이쪽은 자원 경로라 갈라진다.
 */
@RestController
public class RateController {

    private final InventoryApplicationService inventoryApplicationService;
    private final ActorResolver actorResolver;

    public RateController(InventoryApplicationService inventoryApplicationService,
                          ActorResolver actorResolver) {
        this.inventoryApplicationService = inventoryApplicationService;
        this.actorResolver = actorResolver;
    }

    /** RATE-01 날짜별 요금 등록. 201과 Location */
    @PostMapping("/api/v1/room-types/{roomTypeId}/rates")
    public ResponseEntity<DailyRateResponse> register(
            @RequestHeader(value = "X-Dev-Actor-Id", required = false) String actorId,
            @PathVariable String roomTypeId,
            @Valid @RequestBody RegisterRateRequest request) {
        Actor actor = actorResolver.require(actorId, ActorRole.HOST);
        DailyRate registered = inventoryApplicationService.registerRate(
                actor.asHostId(), RoomTypeId.of(roomTypeId), request.stayDate(), request.amount());
        return ResponseEntity
                .created(URI.create("/api/v1/room-types/" + roomTypeId + "/rates/"
                        + ApiDate.format(registered.stayDate())))
                .body(DailyRateResponse.from(registered));
    }

    /** RATE-02 날짜별 요금 수정. 200 */
    @PatchMapping("/api/v1/room-types/{roomTypeId}/rates/{date}")
    public DailyRateResponse adjust(
            @RequestHeader(value = "X-Dev-Actor-Id", required = false) String actorId,
            @PathVariable String roomTypeId,
            @PathVariable String date,
            @Valid @RequestBody AdjustRateRequest request) {
        Actor actor = actorResolver.require(actorId, ActorRole.HOST);
        return DailyRateResponse.from(inventoryApplicationService.adjustRate(
                actor.asHostId(), RoomTypeId.of(roomTypeId), ApiDate.parse("date", date),
                request.version(), request.amount()));
    }
}
