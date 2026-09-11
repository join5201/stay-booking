package com.o2o.booking.application;

import com.o2o.booking.domain.PriceSnapshot;
import com.o2o.booking.domain.StayPeriod;
import com.o2o.shared.RoomTypeId;

/**
 * 가격 계산 포트. 설계 근거: 06-2 6절 예약 CRC(RequestBooking이 가격 계산을 맡긴다), 06-1 R5
 * (프로모션이 상류, 예약이 하류. ACL로 스냅샷을 받는다), 11 내부 처리 가격과 프로모션 절.
 *
 * 숙박 기간의 날짜별 단가와 할인을 계산해 예약 컨텍스트의 PriceSnapshot으로 돌려준다.
 * 요금이 없는 날짜가 있으면 RateNotConfigured다(A6). 인원 검사는 여기 없다. 호출자가
 * RoomType.maxOccupancy로 한다(A5, 계약 6절 인원 상한 검사의 자리 행).
 *
 * 1차 어댑터는 요금만 합산하고 할인은 0이다(계약 7절 D-4). 2차가 프로모션 컨텍스트의
 * PricingService.quote(RoomTypeId, LocalDate, LocalDate)를 감싸는 어댑터로 바꾼다. 시그니처를
 * StayPeriod로 둔 이유는 그 어댑터가 checkIn과 checkOut을 꺼내 한 줄로 넘기면 되기 때문이다.
 */
public interface PriceQuotePort {

    PriceSnapshot quote(RoomTypeId roomTypeId, StayPeriod period);
}
