package com.o2o.promotion.infrastructure;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.o2o.promotion.domain.Promotion;
import com.o2o.promotion.domain.PromotionRepository;
import com.o2o.shared.PageQuery;
import com.o2o.shared.PageResult;
import com.o2o.shared.PromotionId;

/**
 * domain이 선언한 PromotionRepository의 구현. 설계 근거: 06-2 6절 CRC 협력자, 06-4 1-4.
 *
 * 이 어댑터가 하는 일은 값 객체와 문자열 식별자, 그리고 쪽 나눔 타입을 오가는 번역뿐이다.
 * 정렬은 11 공통 목록 절의 기본 id 오름차순이다. 카탈로그의 SpringPage가 패키지 안에 갇혀
 * 있어 같은 변환을 여기 둔다. 기존 파일을 열어 공개 범위를 넓히는 것은 이번 범위 밖이다.
 */
@Repository
public class JpaPromotionRepository implements PromotionRepository {

    private final PromotionJpaRepository jpaRepository;

    public JpaPromotionRepository(PromotionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Promotion save(Promotion promotion) {
        return jpaRepository.save(promotion);
    }

    @Override
    public Optional<Promotion> findById(PromotionId promotionId) {
        return jpaRepository.findById(promotionId.value());
    }

    @Override
    public Optional<Promotion> findForUpdate(PromotionId promotionId) {
        return jpaRepository.findOneForUpdate(promotionId.value());
    }

    /** PROMO-04. enabled가 null이면 전체다. 카탈로그 findAll처럼 호출 지점에서 가른다 */
    @Override
    public PageResult<Promotion> findAll(Boolean enabled, PageQuery pageQuery) {
        Pageable pageable = PageRequest.of(pageQuery.page(), pageQuery.size(),
                Sort.by(Sort.Direction.ASC, "id"));
        Page<Promotion> page = enabled == null
                ? jpaRepository.findAll(pageable)
                : jpaRepository.findAllByEnabled(enabled, pageable);
        return new PageResult<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Override
    public List<Promotion> findEnabledOn(LocalDate at) {
        return jpaRepository.findEnabledOn(at);
    }
}
