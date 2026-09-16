import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { HttpResponse, http } from "msw";
import { afterEach, describe, expect, it, vi } from "vitest";
import { SERVER_NOW, errorBody, heldBooking, requestedAttempt } from "@/lib/api/mocks/fixtures";
import { server } from "@/lib/api/mocks/server";
import { queryWrapper, setActorCookie, setupMockServer } from "@/lib/api/mocks/test-utils";
import type { Booking, PaymentAttempt, Refund } from "@/lib/api/types";
import { BannerProvider } from "../Banner";
import { BookingDetail, CANCEL_REASON_MAX, cancelAllowed } from "./BookingDetail";

setupMockServer();
afterEach(() => cleanup());

const approved: PaymentAttempt = { ...requestedAttempt, status: "APPROVED", completedAt: SERVER_NOW };
const confirmedBooking: Booking = { ...heldBooking, status: "CONFIRMED", confirmedAt: SERVER_NOW, payment: { attemptCount: 1, approvedAttemptId: approved.id, attempts: [approved], refund: null } };
const refund: Refund = { id: "rf_001", paymentAttemptId: approved.id, amount: 216000, currency: "KRW", status: "REFUNDED", reason: "BOOKING_CANCELED", refundedAt: SERVER_NOW };
const canceledOf = (reason: string | null): Booking => ({ ...confirmedBooking, status: "CANCELED", canceledAt: SERVER_NOW, cancellationReason: reason, payment: { ...confirmedBooking.payment, refund } });

// BOOK-03이 돌려줄 예약을 테스트가 바꾼다. BOOK-04는 키와 본문을 모은다
function serveBooking(initial: Booking) {
  const state = { booking: initial, reads: 0 };
  const posts: { key: string; body: unknown }[] = [];
  server.use(
    http.get("*/api/v1/bookings/:id", () => {
      state.reads += 1;
      return HttpResponse.json(state.booking);
    }),
  );
  const onCancel = (next: (n: number) => Booking | { error: string; status: number } | "network") => {
    server.use(
      http.post("*/api/v1/bookings/:id/cancellations", async ({ request }) => {
        posts.push({ key: request.headers.get("idempotency-key") ?? "", body: await request.json() });
        const result = next(posts.length);
        if (result === "network") return HttpResponse.error();
        if ("error" in result) return HttpResponse.json(errorBody(result.error, result.error), { status: result.status });
        state.booking = result;
        return HttpResponse.json(result);
      }),
    );
  };
  return { state, posts, onCancel };
}

function renderDetail() {
  setActorCookie("guest_001");
  const onPay = vi.fn();
  const onList = vi.fn();
  const { Wrapper } = queryWrapper();
  render(
    <Wrapper>
      <BannerProvider>
        <BookingDetail bookingId={heldBooking.id} onPay={onPay} onList={onList} />
      </BannerProvider>
    </Wrapper>,
  );
  return { onPay, onList };
}

const cancelButton = () => screen.getByRole("button", { name: "예약 취소" }) as HTMLButtonElement;
const dialog = () => screen.getByRole("dialog");
const confirmInSheet = () => within(dialog()).getByRole("button", { name: "예약 취소" }) as HTMLButtonElement;

describe("cancelAllowed (W15)", () => {
  it("CONFIRMED이고 체크인 서울 날짜가 serverNow 서울 날짜보다 뒤일 때만. UTC 자정 경계", () => {
    // 서울 09-14 23:59:59 → 체크인 09-15가 뒤
    expect(cancelAllowed({ status: "CONFIRMED", checkIn: "2026-09-15", serverNow: "2026-09-14T14:59:59.000Z" })).toBe(true);
    // 서울 09-15 00:00:00 → 같은 날이라 불가(UTC로는 아직 09-14)
    expect(cancelAllowed({ status: "CONFIRMED", checkIn: "2026-09-15", serverNow: "2026-09-14T15:00:00.000Z" })).toBe(false);
    // 서울 09-15 19:00 → 체크인 10-01은 뒤
    expect(cancelAllowed({ status: "CONFIRMED", checkIn: "2026-10-01", serverNow: SERVER_NOW })).toBe(true);
    expect(cancelAllowed({ status: "HELD", checkIn: "2026-10-01", serverNow: SERVER_NOW })).toBe(false);
    expect(cancelAllowed({ status: "CANCELED", checkIn: "2026-10-01", serverNow: SERVER_NOW })).toBe(false);
  });
});

describe("G7 예약 상세", () => {
  it("HELD 진입. BOOK-03과 이름 둘, 남은 시간, 결제하기가 onPay, 새로 고침이 BOOK-03 한 번 더", async () => {
    const { state } = serveBooking(heldBooking);
    const { onPay } = renderDetail();
    await screen.findByRole("heading", { name: "예약 상세" });
    expect(screen.getByText("선점")).toBeTruthy();
    await screen.findByText("리버 트윈");
    await screen.findByText("한강 뷰 스테이");
    expect(screen.getByText("2026-10-01 부터 2026-10-03 전까지 2박, 2명")).toBeTruthy();
    expect(screen.getByText("10:00")).toBeTruthy();
    expect(screen.getByText("결제를 기다리는 예약입니다")).toBeTruthy();
    expect(screen.getByText("0/3")).toBeTruthy();
    expect(screen.queryByRole("button", { name: "예약 취소" })).toBeNull();

    fireEvent.click(screen.getByRole("button", { name: "결제하기" }));
    expect(onPay).toHaveBeenCalledTimes(1);

    const before = state.reads;
    fireEvent.click(screen.getByRole("button", { name: "새로 고침" }));
    await waitFor(() => expect(state.reads).toBe(before + 1));
  });

  it("남은 시간 0에서 BOOK-03 한 번. EXPIRED면 만료 문구와 읽기만", async () => {
    let reads = 0;
    server.use(
      http.get("*/api/v1/bookings/:id", () => {
        reads += 1;
        // 첫 응답은 남은 시간 1초. 0에 닿아 다시 읽은 응답이 만료
        return HttpResponse.json(reads === 1 ? { ...heldBooking, expiresAt: new Date(Date.parse(heldBooking.serverNow) + 1000).toISOString() } : { ...heldBooking, status: "EXPIRED", expirationReason: "TTL_EXPIRED", expiredAt: SERVER_NOW });
      }),
    );
    renderDetail();
    await screen.findByText("00:01");
    await screen.findByText("남은 시간 안에 결제하지 않아 예약이 만료됐습니다.", {}, { timeout: 3000 });
    expect(reads).toBe(2);
    expect(screen.getByText("만료")).toBeTruthy();
    expect(screen.queryByText("남은 시간")).toBeNull();
    expect(screen.queryByRole("button", { name: "결제하기" })).toBeNull();
    expect(screen.queryByRole("button", { name: "예약 취소" })).toBeNull();
    expect(screen.getByText("만료 시각")).toBeTruthy();
  });

  it("CONFIRMED이고 취소 가능. 시트가 열릴 때 키 하나, 사유 300자 초과는 확인 잠금, 확인이 BOOK-04이고 CANCELED면 취소 카드와 환불 줄", async () => {
    const { posts, onCancel } = serveBooking(confirmedBooking);
    onCancel(() => canceledOf("사정이 생겼습니다"));
    renderDetail();
    await screen.findByText("예약이 확정됐습니다");
    expect(screen.getByText("확정")).toBeTruthy();
    expect(cancelButton().disabled).toBe(false);
    expect(screen.queryByRole("note")).toBeNull();

    fireEvent.click(cancelButton());
    const textarea = within(dialog()).getByRole("textbox") as HTMLTextAreaElement;
    fireEvent.change(textarea, { target: { value: "가".repeat(CANCEL_REASON_MAX + 1) } });
    expect(confirmInSheet().disabled).toBe(true);
    expect(within(dialog()).getByText("취소 사유는 300자 이하입니다.")).toBeTruthy();
    fireEvent.change(textarea, { target: { value: " 사정이 생겼습니다 " } });
    expect(confirmInSheet().disabled).toBe(false);
    expect(within(dialog()).getByText("11/300. 비워도 됩니다.")).toBeTruthy();

    fireEvent.click(confirmInSheet());
    await screen.findByText("예약이 취소됐습니다");
    expect(posts).toHaveLength(1);
    expect(posts[0].key.length).toBeGreaterThanOrEqual(8);
    expect(posts[0].body).toEqual({ reason: "사정이 생겼습니다" });
    expect(screen.queryByRole("dialog")).toBeNull();
    expect(screen.getByText("취소")).toBeTruthy();
    expect(screen.getByText(/사유: 사정이 생겼습니다/)).toBeTruthy();
    expect(screen.getAllByText("환불").length).toBeGreaterThan(0);
    expect(screen.getByText("예약 취소")).toBeTruthy();
    expect(screen.getByText("예약을 취소했습니다.")).toBeTruthy();
    expect(screen.queryByRole("button", { name: "예약 취소" })).toBeNull();
  });

  it("사유를 비우면 본문이 빈 객체", async () => {
    const { posts, onCancel } = serveBooking(confirmedBooking);
    onCancel(() => canceledOf(null));
    renderDetail();
    await screen.findByText("예약이 확정됐습니다");
    fireEvent.click(cancelButton());
    fireEvent.click(confirmInSheet());
    await screen.findByText("예약이 취소됐습니다");
    expect(posts[0].body).toEqual({});
    expect(screen.getByText(/사유 없음/)).toBeTruthy();
  });

  it("W15. 체크인 서울 날짜가 오늘이면 예약 취소 비활성과 이유", async () => {
    // serverNow 서울 09-15 19:00, 체크인 09-15
    serveBooking({ ...confirmedBooking, checkIn: "2026-09-15", checkOut: "2026-09-17" });
    renderDetail();
    await screen.findByText("예약이 확정됐습니다");
    expect(cancelButton().disabled).toBe(true);
    expect(screen.getByRole("note").textContent).toBe("체크인 날짜가 지나 취소할 수 없습니다.");
  });

  it("409 CANCELLATION_NOT_ALLOWED는 시트를 닫고 띠, 재조회. 다시 열면 새 키", async () => {
    const { state, posts, onCancel } = serveBooking(confirmedBooking);
    onCancel((n) => (n === 1 ? { error: "CANCELLATION_NOT_ALLOWED", status: 409 } : canceledOf(null)));
    renderDetail();
    await screen.findByText("예약이 확정됐습니다");
    const before = state.reads;
    fireEvent.click(cancelButton());
    fireEvent.click(confirmInSheet());
    await screen.findByText("취소할 수 있는 날짜가 지났습니다.");
    expect(screen.queryByRole("dialog")).toBeNull();
    await waitFor(() => expect(state.reads).toBe(before + 1));

    fireEvent.click(cancelButton());
    fireEvent.click(confirmInSheet());
    await screen.findByText("예약이 취소됐습니다");
    expect(posts).toHaveLength(2);
    expect(posts[1].key).not.toBe(posts[0].key);
  });

  it("W09. 네트워크 실패의 다시 시도는 같은 키", async () => {
    const { posts, onCancel } = serveBooking(confirmedBooking);
    onCancel((n) => (n === 1 ? "network" : canceledOf(null)));
    renderDetail();
    await screen.findByText("예약이 확정됐습니다");
    fireEvent.click(cancelButton());
    fireEvent.click(confirmInSheet());
    await screen.findByText("일시적인 오류입니다. 연결을 확인하고 다시 시도하세요.");
    fireEvent.click(screen.getByRole("button", { name: "다시 시도" }));
    await screen.findByText("예약이 취소됐습니다");
    expect(posts).toHaveLength(2);
    expect(posts[1].key).toBe(posts[0].key);
  });

  it("404는 전면 Notice와 목록으로", async () => {
    server.use(http.get("*/api/v1/bookings/:id", () => HttpResponse.json(errorBody("RESOURCE_NOT_FOUND", "none"), { status: 404 })));
    const { onList } = renderDetail();
    await screen.findByText("찾을 수 없습니다");
    fireEvent.click(screen.getByRole("button", { name: "목록으로" }));
    expect(onList).toHaveBeenCalledTimes(1);
  });
});
