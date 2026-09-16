package com.o2o.booking.application;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.o2o.booking.domain.Booking;
import com.o2o.booking.domain.BookingExpiredException;
import com.o2o.booking.domain.BookingId;
import com.o2o.booking.domain.BookingNotFoundException;
import com.o2o.booking.domain.BookingRepository;
import com.o2o.booking.domain.BookingStatus;
import com.o2o.booking.domain.InvalidStateTransitionException;
import com.o2o.booking.domain.PaymentAttemptsExhaustedException;
import com.o2o.booking.domain.PaymentInProgressException;
import com.o2o.booking.domain.UserId;
import com.o2o.payment.application.PaymentApplicationService;
import com.o2o.payment.application.PaymentAttemptView;
import com.o2o.payment.application.PaymentSummaryView;
import com.o2o.payment.domain.AlreadyApprovedException;
import com.o2o.payment.domain.AttemptInProgressException;
import com.o2o.payment.domain.AttemptLimitExceededException;
import com.o2o.shared.Money;

/**
 * 결제 컨텍스트와의 접점. RequestPayment 중계(PAY-01)와 시도 목록 조회(PAY-02)와 예약 응답의
 * payment 채움. 설계 근거: 06-4 1-2 예약 표의 RequestPayment 중계 행, 06-4 v5 2-6(조회는 결제가
 * 제공하고 예약이 부른다), 06-1 R6(예약이 결제를 아는 고객 공급자), 11 PAY-01과 PAY-02, 11 결제
 * 접수와 환불 절의 새 시도 처리 순서, 2차 계약 2절 PAY-01 표 6부터 13.
 *
 * 청구액은 예약 스냅샷의 총액이다(I4, R5). 결제가 첫 청구액과 대조해 이후 시도를 묶는다. 결제
 * 예외 넷 중 사람에게 답할 둘은 예약 예외로 감싼다(2차 계약 6절 오류 코드 매핑 행). 핸들러가
 * 결제 예외 클래스를 모르게 하기 위해서다. 금액 불일치와 미승인은 불변 위반이라 감싸지 않는다.
 *
 * 순서가 잠금 순서다. 소유 확인(잠금 없음), 선만료(별도 트랜잭션. 7절 D-2 가), Booking 잠금,
 * 상태 재확인, openAttempt(결제가 Payment를 잠근다). 재고는 이 경로에 없다. 커밋 뒤 결제의 자동
 * 결과 어댑터가 돌고 그 커밋 뒤 P1 또는 P3가 돈다. 응답은 접수 결과(REQUESTED)다.
 */
@Service
@Transactional
public class BookingPaymentService {

    private final BookingRepository bookingRepository;
    private final BookingExpirationService expirationService;
    private final PaymentApplicationService paymentService;
    private final Clock clock;

    public BookingPaymentService(BookingRepository bookingRepository,
                                 BookingExpirationService expirationService,
                                 PaymentApplicationService paymentService, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.expirationService = expirationService;
        this.paymentService = paymentService;
        this.clock = clock;
    }

    /** PAY-01. 설계 근거: 11 PAY-01 처리 규칙, T14와 T15, 2차 계약 2절 PAY-01 표 */
    public PaymentAttemptView requestPayment(RequestPaymentCommand command) {
        BookingId bookingId = command.bookingId();
        requireOwned(command.userId(), bookingId);

        // 7절 D-2 가. 만료 시각을 지난 HELD는 잠그기 전에 별도 트랜잭션으로 먼저 끝내고 저장한다
        switch (expirationService.expireIfDue(bookingId)) {
            case EXPIRED -> throw new BookingExpiredException(bookingId);
            case CONFIRMED -> throw new InvalidStateTransitionException(bookingId,
                    BookingStatus.CONFIRMED, BookingStatus.CONFIRMED, "선만료 자리에서 확정 우선이 닫았다");
            case SKIPPED -> {
            }
        }

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        Instant now = Instant.now(clock);
        switch (booking.status()) {
            case CONFIRMED, CANCELED -> throw new InvalidStateTransitionException(bookingId,
                    booking.status(), BookingStatus.CONFIRMED);
            case EXPIRED -> throw new BookingExpiredException(bookingId);
            case HELD -> {
                // 선만료 뒤 잠그기까지의 좁은 창. 409만 내고 만료 저장은 T1에 맡긴다(D-2 가)
                if (booking.isDue(now)) {
                    throw new BookingExpiredException(bookingId);
                }
            }
        }

        Money charge = new Money(booking.priceSnapshot().totalAmount(),
                booking.priceSnapshot().currency());
        try {
            return paymentService.openAttempt(bookingId.value(), charge, command.mockMode());
        } catch (AttemptInProgressException e) {
            throw new PaymentInProgressException(bookingId, e);
        } catch (AttemptLimitExceededException e) {
            throw new PaymentAttemptsExhaustedException(bookingId, e);
        } catch (AlreadyApprovedException e) {
            // HELD인데 승인 이력이 있다. P1 유실 창이고 T1의 확정 우선이 닫는다(7절 D-1 나)
            throw new InvalidStateTransitionException(bookingId, BookingStatus.HELD,
                    BookingStatus.CONFIRMED, "승인 이력이 이미 있다");
        }
    }

    /** PAY-02. 본인 예약의 시도 목록. 없거나 남의 예약은 자원 정보 없이 404다(T02) */
    @Transactional(readOnly = true)
    public PaymentSummaryView paymentAttempts(UserId userId, BookingId bookingId) {
        requireOwned(userId, bookingId);
        return paymentService.attemptsOf(bookingId.value());
    }

    /**
     * 예약 응답의 payment. BOOK-01부터 BOOK-04가 같은 변환을 쓴다. 소유 확인은 호출자가 이미
     * 했다. 목록은 항목마다 한 번이라 최대 100번이다(2차 계약 6절 Booking 응답의 payment 채움 행).
     */
    @Transactional(readOnly = true)
    public PaymentSummaryView paymentSummaryOf(BookingId bookingId) {
        return paymentService.attemptsOf(bookingId.value());
    }

    private Booking requireOwned(UserId userId, BookingId bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        if (!booking.userId().equals(userId)) {
            throw new BookingNotFoundException(bookingId);
        }
        return booking;
    }
}
