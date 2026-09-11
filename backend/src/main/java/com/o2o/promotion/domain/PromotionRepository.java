package com.o2o.promotion.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.o2o.shared.PageQuery;
import com.o2o.shared.PageResult;
import com.o2o.shared.PromotionId;

/**
 * 설계 근거: 06-2 6절 프로모션 CRC의 협력자 PromotionRepository, 06-2 1절 Promotion 애그리거트
 * 루트. 루트에만 리포지토리를 두고 Condition은 루트를 통해 저장된다.
 *
 * 스프링 데이터 타입이 여기 없는 이유는 카탈로그와 같다. 쪽 나눔은 shared의 PageQuery와
 * PageResult다. 이 인터페이스를 구현하는 것은 infrastructure의 JpaPromotionRepository다.
 */
public interface PromotionRepository {

    Promotion save(Promotion promotion);

    Optional<Promotion> findById(PromotionId promotionId);

    /**
     * PROMO-02. 06-4 1-2 update 행의 Pre 잠금. 앞 묶음의 계약 7절 D-2가 수정 경로에 비관 잠금과
     * 버전 대조를 함께 두었고 이 묶음도 같은 규칙이다. 잠금 행이 없으면 빈 Optional이다.
     */
    Optional<Promotion> findForUpdate(PromotionId promotionId);

    /**
     * PROMO-04. 설계 근거: 11 PROMO-04 쿼리표. enabled가 null이면 전체다. 정렬은 ID 오름차순이다.
     * 11 명세가 정렬을 적지 않아서 재현 가능한 순서 하나를 고정한다.
     */
    PageResult<Promotion> findAll(Boolean enabled, PageQuery pageQuery);

    /**
     * PROMO-05와 검색. 설계 근거: 11 내부 처리 가격과 프로모션 절 첫 줄. 서버의 오늘이
     * campaignStartDate 이상 campaignEndDate 미만이고 enabled=true인 프로모션이 후보다.
     * 지역과 박수와 숙박 기간은 Promotion.isApplicable이 메모리에서 본다. 정렬은 ID 오름차순이다.
     */
    List<Promotion> findEnabledOn(LocalDate at);
}
