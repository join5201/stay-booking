package com.o2o.booking.infrastructure;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import com.o2o.booking.application.PriceQuotePort;
import com.o2o.booking.domain.DailyPrice;
import com.o2o.booking.domain.PriceSnapshot;
import com.o2o.booking.domain.RateNotConfiguredException;
import com.o2o.booking.domain.StayPeriod;
import com.o2o.inventory.domain.DailyRate;
import com.o2o.inventory.domain.DailyRateRepository;
import com.o2o.shared.Money;
import com.o2o.shared.RoomTypeId;

/**
 * 가격 포트의 1차 어댑터. 요금만 합산하고 할인은 0이며 appliedPromotion은 없다.
 * 설계 근거: 계약 7절 D-4 가. 프로모션 컨텍스트의 PricingService는 병렬 세션이 만들고 있어
 * 1차는 그것을 import하지 않는다. 6단계의 실제 서버가 가격을 내야 해서 프로덕션 빈이다.
 *
 * 2차에서 교체한다. 프로모션 세션의 코드가 main에 들어오면 이 클래스를 tmp/_moved/로 옮기고
 * 같은 자리에 PricingService.quote(RoomTypeId, LocalDate, LocalDate)를 감싸는 어댑터를 둔다.
 * 그때 T12와 T13의 프로모션 절반이 닫힌다.
 *
 * 요금 조회는 재고와 요금 컨텍스트의 리포지토리를 읽기로 쓴다(06-1 R1과 R4의 하류).
 * 빠진 날짜는 A6 위반이라 RateNotConfigured로 알린다. 총액 셋을 여기서 만들고 PriceSnapshot이
 * 대조한다(06-2 6절. 만드는 쪽과 검증하는 쪽이 다르다).
 */
@Component
public class RateOnlyPriceQuoteAdapter implements PriceQuotePort {

    private final DailyRateRepository rateRepository;

    public RateOnlyPriceQuoteAdapter(DailyRateRepository rateRepository) {
        this.rateRepository = rateRepository;
    }

    @Override
    public PriceSnapshot quote(RoomTypeId roomTypeId, StayPeriod period) {
        List<DailyRate> rates = rateRepository.findRange(roomTypeId, period.checkIn(),
                period.checkOut());
        List<LocalDate> priced = rates.stream().map(DailyRate::stayDate).toList();
        List<LocalDate> missing = period.dates().stream()
                .filter((d) -> !priced.contains(d)).toList();
        if (!missing.isEmpty()) {
            throw new RateNotConfiguredException(missing);
        }
        List<DailyPrice> days = rates.stream()
                .map((rate) -> new DailyPrice(rate.stayDate(), rate.rate().amount(), 0L))
                .toList();
        long baseTotal = days.stream().mapToLong(DailyPrice::baseAmount).sum();
        return PriceSnapshot.of(period, Money.KRW, days, null, baseTotal, 0L, baseTotal);
    }
}
