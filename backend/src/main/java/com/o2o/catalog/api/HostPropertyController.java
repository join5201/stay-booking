package com.o2o.catalog.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.o2o.catalog.application.CatalogApplicationService;
import com.o2o.shared.Actor;
import com.o2o.shared.ActorResolver;
import com.o2o.shared.ActorRole;
import com.o2o.shared.PageQuery;

/**
 * CAT-05 본인 숙소 목록 조회. 설계 근거: 11 숙소 CAT-05.
 *
 * PropertyController와 클래스를 가른 이유는 경로가 다르기 때문이다. 이쪽은 /api/v1/host 아래이고
 * 저쪽은 /api/v1/properties 아래다. 한 클래스에 두면 클래스 수준 경로를 못 쓴다.
 *
 * hostId 쿼리를 받지 않는다. 같은 절의 처리 규칙이 행위자의 hostId로 범위를 제한하라고 적는다.
 * 받을 칸이 없으면 남의 목록을 볼 통로도 없다.
 */
@RestController
public class HostPropertyController {

    private final CatalogApplicationService catalogApplicationService;
    private final ActorResolver actorResolver;

    public HostPropertyController(CatalogApplicationService catalogApplicationService,
                                  ActorResolver actorResolver) {
        this.catalogApplicationService = catalogApplicationService;
        this.actorResolver = actorResolver;
    }

    @GetMapping("/api/v1/host/properties")
    public PageResponse<PropertyResponse> list(
            @RequestHeader(value = "X-Dev-Actor-Id", required = false) String actorId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        Actor actor = actorResolver.require(actorId, ActorRole.HOST);
        return PageResponse.from(
                catalogApplicationService.listHostProperties(actor.asHostId(), PageQuery.of(page, size)),
                PropertyResponse::from);
    }
}
