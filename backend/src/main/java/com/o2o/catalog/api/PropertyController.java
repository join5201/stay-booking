package com.o2o.catalog.api;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.o2o.catalog.application.CatalogApplicationService;
import com.o2o.catalog.domain.Property;
import com.o2o.shared.Actor;
import com.o2o.shared.ActorResolver;
import com.o2o.shared.ActorRole;
import com.o2o.shared.PageQuery;
import com.o2o.shared.PropertyId;
import com.o2o.shared.RegionRegistry;

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

    /**
     * CAT-02 숙소 수정. 인증 HOST이고 대상 숙소의 소유자여야 한다.
     * 버전 대조로 동시 수정을 막는다. 불일치는 409 VERSION_CONFLICT다(계약 2-2절).
     */
    @PatchMapping("/{propertyId}")
    public PropertyResponse update(
            @RequestHeader(value = "X-Dev-Actor-Id", required = false) String actorId,
            @PathVariable String propertyId,
            @Valid @RequestBody UpdatePropertyRequest request) {
        Actor actor = actorResolver.require(actorId, ActorRole.HOST);
        return PropertyResponse.from(catalogApplicationService.updateProperty(
                actor.asHostId(), PropertyId.of(propertyId), request.version(),
                request.name(), request.regionCode(), request.address(), request.description()));
    }

    /**
     * CAT-04 숙소 목록 조회. 인증 불필요. 지역 코드는 선택이다.
     * page와 size의 기본값과 상한은 PageQuery가 갖고 있다. 11 공통 목록과 날짜 범위.
     * regionCode의 형식(1자 이상 32자 이하)은 여기서 보고 등록 여부는 앱 서비스가 본다.
     */
    @GetMapping
    public PageResponse<PropertyResponse> list(
            @RequestParam(required = false) String regionCode,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return PageResponse.from(
                catalogApplicationService.listProperties(regionCode(regionCode), PageQuery.of(page, size)),
                PropertyResponse::from);
    }

    /**
     * 11 CAT-04 쿼리표의 regionCode 제약. 공백만이거나 32자를 넘으면 400 INVALID_REQUEST.
     * 검색 SEARCH-01의 같은 검사와 같은 모양이다. 애너테이션 대신 검사문인 이유도 같다.
     * R1 평가 B-02
     */
    private static String regionCode(String regionCode) {
        if (regionCode == null) {
            return null;
        }
        if (regionCode.isBlank() || regionCode.length() > RegionRegistry.CODE_MAX_LENGTH) {
            throw new IllegalArgumentException("regionCode는 1자 이상 " + RegionRegistry.CODE_MAX_LENGTH
                    + "자 이하여야 한다: " + regionCode);
        }
        return regionCode;
    }
}
