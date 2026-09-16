import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { HttpResponse, http } from "msw";
import { afterEach, describe, expect, it, vi } from "vitest";
import { errorBody, heldBooking, requestedAttempt } from "@/lib/api/mocks/fixtures";
import { server } from "@/lib/api/mocks/server";
import { queryWrapper, setActorCookie, setupMockServer } from "@/lib/api/mocks/test-utils";
import type { Booking, PaymentAttempt } from "@/lib/api/types";
import type { MockMode } from "@/lib/dev-actor";
import { BannerProvider } from "../Banner";
import { DevActorProvider } from "../DevActorProvider";
import { PaymentScreen, paymentPhaseOf } from "./PaymentScreen";

setupMockServer();
afterEach(() => cleanup());

const G1_QUERY = "?regionCode=SEOUL&checkIn=2026-10-01&checkOut=2026-10-03&guestCount=2";

function attempt(n: number, status: PaymentAttempt["status"], mockMode: MockMode = "APPROVE"): PaymentAttempt {
  return { ...requestedAttempt, id: `pa_00${n}`, attemptNumber: n, status, mockMode, failureCode: status === "FAILED" ? "DECLINED" : null, completedAt: status === "REQUESTED" ? null : heldBooking.serverNow };
}

function withAttempts(base: Booking, attempts: PaymentAttempt[], patch: Partial<Booking> = {}): Booking {
  const approved = attempts.find((a) => a.status === "APPROVED");
  return { ...base, ...patch, payment: { attemptCount: attempts.length, approvedAttemptId: approved?.id ?? null, attempts, refund: null } };
}

// BOOK-03이 돌려줄 예약을 테스트가 바꾼다. PAY-01은 키와 본문을 모으고 다음 상태로 넘긴다
function serveBooking(initial: Booking) {
  const state = { booking: initial, reads: 0 };
  const posts: { key: string; body: unknown }[] = [];
  server.use(
    http.get("*/api/v1/bookings/:id", () => {
      state.reads += 1;
      return HttpResponse.json(state.booking);
    }),
  );
  const onPost = (next: (n: number) => Booking | { error: string; status: number }) => {
    server.use(
      http.post("*/api/v1/bookings/:id/payment-attempts", async ({ request }) => {
        posts.push({ key: request.headers.get("idempotency-key") ?? "", body: await request.json() });
        const result = next(posts.length);
        if ("error" in result) return HttpResponse.json(errorBody(result.error, result.error), { status: result.status });
        state.booking = result;
        return HttpResponse.json({ ...requestedAttempt, attemptNumber: posts.length }, { status: 202 });
      }),
    );
  };
  return { state, posts, onPost };
}

function renderPay(mockMode: MockMode = "APPROVE") {
  setActorCookie("guest_001");
  const onDetail = vi.fn();
  const onReplaceDetail = vi.fn();
  const onNewBooking = vi.fn();
  const { Wrapper } = queryWrapper();
  render(
    <Wrapper>
      <DevActorProvider initialActor="guest_001" initialMockMode={mockMode}>
        <BannerProvider>
          <PaymentScreen bookingId={heldBooking.id} onDetail={onDetail} onReplaceDetail={onReplaceDetail} onNewBooking={onNewBooking} />
        </BannerProvider>
      </DevActorProvider>
    </Wrapper>,
  );
  return { onDetail, onReplaceDetail, onNewBooking };
}

const payButton = () => screen.getByRole("button", { name: /결제하기$/ }) as HTMLButtonElement;

describe("paymentPhaseOf", () => {
  it("상태와 시도 목록으로 여섯 국면", () => {
    expect(paymentPhaseOf(withAttempts(heldBooking, [attempt(1, "APPROVED")], { status: "CONFIRMED" }))).toBe("confirmed");
    expect(paymentPhaseOf({ ...heldBooking, status: "EXPIRED" })).toBe("expired");
    expect(paymentPhaseOf({ ...heldBooking, status: "CANCELED" })).toBe("other");
    expect(paymentPhaseOf(heldBooking)).toBe("payable");
    expect(paymentPhaseOf(withAttempts(heldBooking, [attempt(1, "REQUESTED")]))).toBe("processing");
    expect(paymentPhaseOf(withAttempts(heldBooking, [attempt(1, "FAILED"), attempt(2, "FAILED")]))).toBe("payable");
    expect(paymentPhaseOf(withAttempts(heldBooking, [attempt(1, "FAILED"), attempt(2, "FAILED"), attempt(3, "FAILED")]))).toBe("exhausted");
  });
});

describe("G5 결제", () => {
  it("HELD 진입. 남은 시간과 시도 0/3. 결제하기가 PAY-01(키 있음, APPROVE면 본문 비움)이고 재조회가 CONFIRMED면 확정 카드", async () => {
    const { posts, onPost } = serveBooking(heldBooking);
    onPost(() => withAttempts(heldBooking, [attempt(1, "APPROVED")], { status: "CONFIRMED", confirmedAt: heldBooking.serverNow }));
    const { onDetail, onReplaceDetail } = renderPay();
    await screen.findByRole("heading", { name: "결제" });
    expect(screen.getByText("선점")).toBeTruthy();
    expect(screen.getByText("10:00")).toBeTruthy();
    expect(screen.getByText("0/3")).toBeTruthy();
    await screen.findByText("리버 트윈");
    expect(payButton().textContent).toBe("결제하기");
    expect(payButton().disabled).toBe(false);
    expect(screen.queryByText(/개발용:/)).toBeNull();

    fireEvent.click(payButton());
    await screen.findByText("예약이 확정됐습니다");
    expect(posts).toHaveLength(1);
    expect(posts[0].key.length).toBeGreaterThanOrEqual(8);
    expect(posts[0].body).toEqual({});
    expect(screen.getByText("확정")).toBeTruthy();
    expect(screen.getByText("1/3")).toBeTruthy();
    expect(screen.queryByRole("button", { name: /결제하기$/ })).toBeNull();
    fireEvent.click(screen.getByRole("button", { name: "예약 상세로" }));
    expect(onDetail).toHaveBeenCalledTimes(1);
    expect(onReplaceDetail).not.toHaveBeenCalled();
  });

  it("W14. DECLINE 세 번은 클릭마다 새 키와 mockMode 본문. 셋째 뒤 EXPIRED PAYMENT_FAILED 카드와 같은 조건으로 새 예약", async () => {
    const { posts, onPost } = serveBooking(heldBooking);
    onPost((n) => {
      const failed = Array.from({ length: n }, (_, i) => attempt(i + 1, "FAILED", "DECLINE"));
      return n < 3 ? withAttempts(heldBooking, failed) : withAttempts(heldBooking, failed, { status: "EXPIRED", expirationReason: "PAYMENT_FAILED", expiredAt: heldBooking.serverNow });
    });
    const { onNewBooking } = renderPay("DECLINE");
    await screen.findByRole("heading", { name: "결제" });
    expect(screen.getByText("개발용: DECLINE")).toBeTruthy();

    fireEvent.click(payButton());
    await screen.findByText("1/3");
    expect(payButton().textContent).toBe("다시 결제하기");
    await waitFor(() => expect(payButton().disabled).toBe(false));
    fireEvent.click(payButton());
    await screen.findByText("2/3");
    await waitFor(() => expect(payButton().disabled).toBe(false));
    fireEvent.click(payButton());
    await screen.findByText("예약이 만료됐습니다");
    expect(screen.getByText("결제 시도 3회가 모두 실패해 예약이 만료됐습니다.")).toBeTruthy();
    expect(screen.getByText("만료")).toBeTruthy();
    expect(screen.queryByRole("button", { name: /결제하기$/ })).toBeNull();

    expect(posts).toHaveLength(3);
    expect(posts.every((p) => p.key.length >= 8)).toBe(true);
    expect(new Set(posts.map((p) => p.key)).size).toBe(3);
    expect(posts.every((p) => JSON.stringify(p.body) === JSON.stringify({ mockMode: "DECLINE" }))).toBe(true);

    fireEvent.click(screen.getByRole("button", { name: "같은 조건으로 새 예약" }));
    expect(onNewBooking).toHaveBeenCalledWith(G1_QUERY);
  });

  it("W14. DEFER는 REQUESTED가 남아 처리 중 카드와 결제 잠금. 새로 고침이 BOOK-03이고 결과가 오면 확정 카드", async () => {
    const { state, onPost } = serveBooking(heldBooking);
    onPost(() => withAttempts(heldBooking, [attempt(1, "REQUESTED", "DEFER")]));
    renderPay("DEFER");
    await screen.findByRole("heading", { name: "결제" });
    fireEvent.click(payButton());
    await screen.findByText("결제를 처리 중입니다");
    expect(payButton().disabled).toBe(true);

    const before = state.reads;
    state.booking = withAttempts(heldBooking, [attempt(1, "APPROVED", "DEFER")], { status: "CONFIRMED" });
    fireEvent.click(screen.getByRole("button", { name: "새로 고침" }));
    await screen.findByText("예약이 확정됐습니다");
    expect(state.reads).toBe(before + 1);
  });

  it("W14. 남은 시간이 0에 닿으면 BOOK-03을 한 번 다시 읽고 EXPIRED TTL_EXPIRED 카드", async () => {
    // 첫 응답은 남은 시간 0인 HELD. 0초에서 다시 읽은 응답이 만료
    let reads = 0;
    server.use(
      http.get("*/api/v1/bookings/:id", () => {
        reads += 1;
        return HttpResponse.json(reads === 1 ? { ...heldBooking, expiresAt: heldBooking.serverNow } : { ...heldBooking, status: "EXPIRED", expirationReason: "TTL_EXPIRED", expiredAt: heldBooking.serverNow });
      }),
    );
    renderPay();
    await screen.findByText("남은 시간 안에 결제하지 않아 예약이 만료됐습니다.");
    expect(reads).toBe(2);
    expect(screen.getByText("만료")).toBeTruthy();
    expect(screen.queryByText("남은 시간")).toBeNull();
    expect(screen.queryByRole("button", { name: /결제하기$/ })).toBeNull();
  });

  it("PAY-01 409 PAYMENT_ATTEMPTS_EXHAUSTED는 짧은 띠와 재조회. 재조회가 FAILED 셋이면 다 썼습니다 카드", async () => {
    const three = [attempt(1, "FAILED"), attempt(2, "FAILED"), attempt(3, "FAILED")];
    const { state, onPost } = serveBooking(withAttempts(heldBooking, [attempt(1, "FAILED"), attempt(2, "FAILED")]));
    onPost(() => ({ error: "PAYMENT_ATTEMPTS_EXHAUSTED", status: 409 }));
    renderPay();
    await screen.findByRole("heading", { name: "결제" });
    state.booking = withAttempts(heldBooking, three);
    fireEvent.click(payButton());
    await screen.findByText("결제 시도 3회를 다 썼습니다.");
    await screen.findByText("결제 시도를 다 썼습니다");
    expect(screen.getByText("3/3")).toBeTruthy();
    expect(payButton().disabled).toBe(true);
  });

  it("W09. 네트워크 실패의 다시 시도는 같은 키", async () => {
    const keys: string[] = [];
    serveBooking(heldBooking);
    server.use(
      http.post("*/api/v1/bookings/:id/payment-attempts", ({ request }) => {
        keys.push(request.headers.get("idempotency-key") ?? "");
        return keys.length === 1 ? HttpResponse.error() : HttpResponse.json(requestedAttempt, { status: 202 });
      }),
    );
    renderPay();
    await screen.findByRole("heading", { name: "결제" });
    fireEvent.click(payButton());
    await screen.findByText("일시적인 오류입니다. 연결을 확인하고 다시 시도하세요.");
    fireEvent.click(screen.getByRole("button", { name: "다시 시도" }));
    await waitFor(() => expect(keys).toHaveLength(2));
    expect(keys[1]).toBe(keys[0]);
  });

  it("진입 시 HELD가 아니면 상세로 replace하고 화면을 그리지 않는다", async () => {
    serveBooking(withAttempts(heldBooking, [attempt(1, "APPROVED")], { status: "CONFIRMED" }));
    const { onReplaceDetail } = renderPay();
    await waitFor(() => expect(onReplaceDetail).toHaveBeenCalledTimes(1));
    expect(screen.queryByRole("heading", { name: "결제" })).toBeNull();
  });
});
