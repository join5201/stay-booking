package com.o2o.promotion.infrastructure;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.o2o.promotion.domain.Promotion;

import jakarta.persistence.LockModeType;

/**
 * 스프링 데이터가 구현을 만들어 주는 인터페이스. 설계 근거: 06-4 1-4, 06-4 0절 락 선언.
 *
 * domain의 PromotionRepository와 이름이 다른 이유는 카탈로그와 같다. 저쪽은 도메인이 선언한
 * 항구이고 이것은 어댑터의 부품이다. 조회 조건은 이름 규칙이 아니라 JPQL로 적는다. 임베더블
 * 속성(condition.campaignStartDate)을 타는 조건이라 이름 규칙 해석이 갈릴 수 있기 때문이다.
 */
public interface PromotionJpaRepository extends JpaRepository<Promotion, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Promotion p where p.id = :id")
    Optional<Promotion> findOneForUpdate(@Param("id") String id);

    // PROMO-04. enabled 필터. 정렬은 Pageable이 준다
    @Query("select p from Promotion p where p.enabled = :enabled")
    Page<Promotion> findAllByEnabled(@Param("enabled") boolean enabled, Pageable pageable);

    // 11 내부 처리 가격과 프로모션 절 첫 줄. 오늘이 시작 이상 끝 미만이고 enabled=true
    @Query("select p from Promotion p where p.enabled = true "
            + "and p.condition.campaignStartDate <= :at and p.condition.campaignEndDate > :at "
            + "order by p.id")
    List<Promotion> findEnabledOn(@Param("at") LocalDate at);
}
