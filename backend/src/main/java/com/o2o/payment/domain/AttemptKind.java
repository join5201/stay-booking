package com.o2o.payment.domain;

/**
 * 시도의 종류. 설계 근거: 08-3 결정 2(고아 승인 모델), 06-4 v5 1-3 결제 시도 표의 kind 열.
 *
 * NORMAL은 순번을 갖고 I6, I7, I9의 셈 대상이다. ORPHAN은 실 PG에서 모델에 붙지 않은 승인을
 * 환불하기 위한 행이다. 계약 7절 D-4에 따라 v1 코드는 ORPHAN 행을 만들지 않고 표 모양만
 * 08-3대로 둔다. 값이 여기 있는 이유는 실 PG 전환 시 고아 분기를 붙일 자리를 남기기 위해서다.
 */
public enum AttemptKind {
    NORMAL,
    ORPHAN
}
