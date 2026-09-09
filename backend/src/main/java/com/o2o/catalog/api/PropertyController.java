package com.o2o.catalog.api;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.o2o.catalog.application.CatalogApplicationService;
import com.o2o.catalog.domain.Property;
import com.o2o.shared.Actor;
import com.o2o.shared.ActorResolver;
import com.o2o.shared.ActorRole;
import com.o2o.shared.PropertyId;

import jakarta.validation.Valid;

/**
 * CAT-01과 CAT-03. 설계 근거: 11 숙소 절.
 *
 * 경로와 상태 코드가 그 절 그대로다. 등록은 201에 Location이고 조회는 200이다.
 * 11 공통 요청과 응답 규칙이 그 둘을 못박는다.
 *
 * 이 층의 책임은 형식 검증과 번역뿐이다. 규칙은 도메인이, 컨텍스트를 넘는 선행조건은
 * 앱 서비스가 지킨다(06-4 1-4).
 */
@RestController
@RequestMapping("/api/v1/properties")
public class PropertyController {

    private final CatalogApplicationService catalogApplicationService;
    private final ActorResolver actorResolver;

    public PropertyController(CatalogApplicationService catalogApplicationService,
                              ActorResolver actorResolver) {
        this.catalogApplicationService = catalogApplicationService;
        this.actorResolver = actorResolver;
    }

    /**
     * CAT-01 숙소 등록. 인증 HOST.
     * hostId를 헤더 행위자에서 채운다. 11 CAT-01 처리 규칙이 body로 소유자를 받지 말라고 적는다.
     */
    @PostMapping
    public ResponseEntity<PropertyResponse> register(
            @RequestHeader(value = "X-Dev-Actor-Id", required = false) String actorId,
            @Valid @RequestBody RegisterPropertyRequest request) {
        Actor actor = actorResolver.require(actorId, ActorRole.HOST);
        Property property = catalogApplicationService.registerProperty(
                actor.asHostId(), request.name(), request.regionCode(),
                request.address(), request.descriptionOrEmpty());
        return ResponseEntity
                .created(URI.create("/api/v1/properties/" + property.id().value()))
                .body(PropertyResponse.from(property));
    }

    /**
     * CAT-03 숙소 상세 조회. 인증 불필요.
     * 11 인증과 접근 제어의 공개 행이 숙소와 객실 조회를 공개로 적는다. 헤더를 읽지 않는다.
     */
    @GetMapping("/{propertyId}")
    public PropertyResponse get(@PathVariable String propertyId) {
        return PropertyResponse.from(
                catalogApplicationService.getProperty(PropertyId.of(propertyId)));
    }
}
