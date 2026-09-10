package com.o2o.inventory.api;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.o2o.inventory.application.InventoryApplicationService;
import com.o2o.inventory.application.RangeResult;
import com.o2o.inventory.domain.DailyInventory;
import com.o2o.shared.Actor;
import com.o2o.shared.ActorResolver;
import com.o2o.shared.ActorRole;
import com.o2o.shared.RoomTypeId;

import jakarta.validation.Valid;

/**
 * INV-01과 INV-02와 INV-03. 설계 근거: 11 재고 절.
 *
 * 세 경로가 모두 객실 타입 밑에 걸린다. 그래도 클래스 수준 RequestMapping을 두지 않는 것은
 * RoomTypeController와 같은 이유다. 11이 전체 경로를 적으므로 코드도 전체 경로를 적어야
 * 명세와 눈으로 대조된다.
 *
 * 세 경로 다 인증이 HOST이고 대상 숙소의 소유자다. 역할 확인은 여기가 하고 소유자 확인은
 * 앱 서비스가 한다. 역할은 요청 헤더만 보면 되고 소유자는 카탈로그를 읽어야 한다.
 * 06-4 1-4가 컨텍스트를 넘는 선행조건을 앱 서비스에 둔다.
 */
@RestController
public class InventoryController {

    private final InventoryApplicationService inventoryApplicationService;
    private final ActorResolver actorResolver;

    public InventoryController(InventoryApplicationService inventoryApplicationService,
                               ActorResolver actorResolver) {
        this.inventoryApplicationService = inventoryApplicationService;
        this.actorResolver = actorResolver;
    }

    /** INV-01 날짜별 재고 등록. 201과 Location */
    @PostMapping("/api/v1/room-types/{roomTypeId}/inventories")
    public ResponseEntity<DailyInventoryResponse> register(
            @RequestHeader(value = "X-Dev-Actor-Id", required = false) String actorId,
            @PathVariable String roomTypeId,
            @Valid @RequestBody RegisterInventoryRequest request) {
        Actor actor = actorResolver.require(actorId, ActorRole.HOST);
        DailyInventory opened = inventoryApplicationService.openInventory(
                actor.asHostId(), RoomTypeId.of(roomTypeId), request.stayDate(),
                request.totalCount());
        return ResponseEntity
                .created(URI.create("/api/v1/room-types/" + roomTypeId + "/inventories/"
                        + ApiDate.format(opened.stayDate())))
                .body(DailyInventoryResponse.from(opened));
    }

    /**
     * INV-02 기간 재고 일괄 등록. 201과 Location이고 Location은 기간 조회 경로다.
     * 11 INV-02의 Location 예시가 단건이 아니라 from과 to가 붙은 목록 경로다.
     */
    @PostMapping("/api/v1/room-types/{roomTypeId}/inventories/bulk")
    public ResponseEntity<InventoryRangeResponse> registerBulk(
            @RequestHeader(value = "X-Dev-Actor-Id", required = false) String actorId,
            @PathVariable String roomTypeId,
            @Valid @RequestBody BulkInventoryRequest request) {
        Actor actor = actorResolver.require(actorId, ActorRole.HOST);
        LocalDate from = request.fromDate();
        LocalDate to = request.toDate();
        List<DailyInventory> opened = inventoryApplicationService.openInventories(
                actor.asHostId(), RoomTypeId.of(roomTypeId), from, to, request.totalCount());
        return ResponseEntity
                .created(URI.create("/api/v1/room-types/" + roomTypeId + "/inventories"
                        + "?from=" + ApiDate.format(from) + "&to=" + ApiDate.format(to)))
                // 등록 성공이면 빠진 날짜가 없다. 하나라도 빠지면 여기까지 오지 않고 전부 실패다
                .body(InventoryRangeResponse.of(roomTypeId, from, to, opened, List.of()));
    }

    /**
     * INV-04 기간 재고 조회. 200.
     * 쓰기와 달리 과거 기간도 200이다. 명세의 처리 규칙이 과거 조회를 허용한다고 적는다.
     */
    @GetMapping("/api/v1/room-types/{roomTypeId}/inventories")
    public InventoryRangeResponse findRange(
            @RequestHeader(value = "X-Dev-Actor-Id", required = false) String actorId,
            @PathVariable String roomTypeId,
            @RequestParam String from,
            @RequestParam String to) {
        Actor actor = actorResolver.require(actorId, ActorRole.HOST);
        LocalDate fromDate = ApiDate.parse("from", from);
        LocalDate toDate = ApiDate.parse("to", to);
        RangeResult<DailyInventory> found = inventoryApplicationService.findInventories(
                actor.asHostId(), RoomTypeId.of(roomTypeId), fromDate, toDate);
        return InventoryRangeResponse.of(roomTypeId, fromDate, toDate,
                found.items(), found.missingDates());
    }

    /** INV-05 날짜별 재고 조회. 200이고 없으면 404 */
    @GetMapping("/api/v1/room-types/{roomTypeId}/inventories/{date}")
    public DailyInventoryResponse get(
            @RequestHeader(value = "X-Dev-Actor-Id", required = false) String actorId,
            @PathVariable String roomTypeId,
            @PathVariable String date) {
        Actor actor = actorResolver.require(actorId, ActorRole.HOST);
        return DailyInventoryResponse.from(inventoryApplicationService.getInventory(
                actor.asHostId(), RoomTypeId.of(roomTypeId), ApiDate.parse("date", date)));
    }

    /** INV-03 날짜별 재고 수정. 200 */
    @PatchMapping("/api/v1/room-types/{roomTypeId}/inventories/{date}")
    public DailyInventoryResponse adjust(
            @RequestHeader(value = "X-Dev-Actor-Id", required = false) String actorId,
            @PathVariable String roomTypeId,
            @PathVariable String date,
            @Valid @RequestBody AdjustInventoryRequest request) {
        Actor actor = actorResolver.require(actorId, ActorRole.HOST);
        return DailyInventoryResponse.from(inventoryApplicationService.adjustInventory(
                actor.asHostId(), RoomTypeId.of(roomTypeId), ApiDate.parse("date", date),
                request.version(), request.totalCount()));
    }
}
