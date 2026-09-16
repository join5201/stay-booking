import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { HttpResponse, http } from "msw";
import { afterEach, describe, expect, it, vi } from "vitest";
import { availability, errorBody, promotion, roomType } from "@/lib/api/mocks/fixtures";
import { requestLog } from "@/lib/api/mocks/handlers";
import { server } from "@/lib/api/mocks/server";
import { queryWrapper, setActorCookie, setupMockServer } from "@/lib/api/mocks/test-utils";
import type { DevActor } from "@/lib/dev-actor";
import { DevActorProvider } from "../DevActorProvider";
import type { SearchValues } from "../SearchForm";
import { dayReasonOf } from "./AvailabilityTable";
import { RoomTypeDetail, reserveBlockReason } from "./RoomTypeDetail";

setupMockServer();
afterEach(() => cleanup());

const STAY: SearchValues = { regionCode: "SEOUL", checkIn: "2026-10-01", checkOut: "2026-10-03", guestCount: 2 };
const QUERY = "?regionCode=SEOUL&checkIn=2026-10-01&checkOut=2026-10-03&guestCount=2";
const NONE: SearchValues = { regionCode: "", checkIn: "", checkOut: "", guestCount: null };

function renderDetail(values: SearchValues, actor: DevActor = "guest_001") {
  if (actor !== "public") setActorCookie(actor);
  const onReserve = vi.fn();
  const { Wrapper } = queryWrapper();
  render(
    <Wrapper>
      <DevActorProvider initialActor={actor} initialMockMode="APPROVE">
        <RoomTypeDetail roomTypeId={roomType.id} values={values} onReserve={onReserve} />
      </DevActorProvider>
    </Wrapper>,
  );
  return { onReserve };
}

const reserveButton = () => screen.getByRole("button", { name: "예약하기" }) as HTMLButtonElement;
const paths = () => requestLog.map((r) => r.path);

describe("G3 진입", () => {
  it("조건 셋이 있으면 CAT-08과 SEARCH-02와 SEARCH-03과 PROMO-05를 동시에. 표 셋과 예상 금액 고정 문구", async () => {
    const { onReserve } = renderDetail(STAY);
    await screen.findByRole("heading", { name: roomType.name });
    await waitFor(() => expect(requestLog).toHaveLength(4));
    const stay = "checkIn=2026-10-01&checkOut=2026-10-03&guestCount=2";
    expect(paths().sort()).toEqual(
      [
        `/api/v1/room-types/${roomType.id}`,
        `/api/v1/room-types/${roomType.id}/applicable-promotions?${stay}`,
        `/api/v1/room-types/${roomType.id}/availability?${stay}`,
        `/api/v1/room-types/${roomType.id}/price-quote?${stay}`,
      ].sort(),
    );
    await screen.findByText("예상 금액이며 확정 금액이 아닙니다.");
    expect(screen.getByText("2박 합계")).toBeTruthy();
    expect(screen.getByText("전 날짜 예약 가능. 남은 객실 최소 2")).toBeTruthy();
    expect(within(screen.getByText(promotion.name).closest("li") as HTMLElement).getByText("적용")).toBeTruthy();

    await waitFor(() => expect(reserveButton().disabled).toBe(false));
    expect(screen.queryByRole("note")).toBeNull();
    fireEvent.click(reserveButton());
    expect(onReserve).toHaveBeenCalledWith(QUERY);
  });

  it("조건이 없으면 CAT-08만. 요청 셋이 없고 예약 버튼은 비활성과 이유", async () => {
    renderDetail(NONE);
    await screen.findByRole("heading", { name: roomType.name });
    await new Promise((r) => setTimeout(r, 30));
    expect(paths()).toEqual([`/api/v1/room-types/${roomType.id}`]);
    expect(reserveButton().disabled).toBe(true);
    expect(screen.getByRole("note").textContent).toBe("날짜와 인원을 정하면 예약할 수 있습니다.");
    expect(screen.getByRole("link", { name: "검색에서 정하기" })).toBeTruthy();
  });
});

describe("W13. 예약 버튼 비활성 조건 넷", () => {
  it("셋 중 하나 실패. SEARCH-03 409 RATE_NOT_CONFIGURED", async () => {
    server.use(http.get("*/api/v1/room-types/:id/price-quote", () => HttpResponse.json(errorBody("RATE_NOT_CONFIGURED", "none"), { status: 409 })));
    renderDetail(STAY);
    await screen.findByText("요금이 없는 날짜가 있습니다.");
    expect(reserveButton().disabled).toBe(true);
    expect(screen.getByRole("note").textContent).toBe("가용 수나 예상 금액을 못 읽어 예약할 수 없습니다.");
  });

  it("available이 false. 날짜별 사유(판매 안 함, 요금 없음, 마감)", async () => {
    server.use(
      http.get("*/api/v1/room-types/:id/availability", () =>
        HttpResponse.json({
          ...availability,
          available: false,
          availableCount: 0,
          days: [
            { date: "2026-10-01", availableCount: null },
            { date: "2026-10-02", availableCount: 0 },
          ],
          missingInventoryDates: ["2026-10-01"],
          reasons: ["INVENTORY_NOT_CONFIGURED", "INVENTORY_UNAVAILABLE"],
        }),
      ),
    );
    renderDetail(STAY);
    await screen.findByText("판매 안 함");
    expect(screen.getByText("마감")).toBeTruthy();
    expect(screen.getByText("이 조건으로는 예약할 수 없는 날짜가 있습니다.")).toBeTruthy();
    await waitFor(() => expect(screen.queryByLabelText("불러오는 중")).toBeNull());
    expect(reserveButton().disabled).toBe(true);
    expect(screen.getByRole("note").textContent).toBe("이 조건으로는 예약할 수 없습니다. 날짜별 사유를 보세요.");
  });

  it("public 행위자", async () => {
    renderDetail(STAY, "public");
    await screen.findByText("예상 금액이며 확정 금액이 아닙니다.");
    await waitFor(() => expect(screen.queryByLabelText("불러오는 중")).toBeNull());
    expect(reserveButton().disabled).toBe(true);
    expect(screen.getByRole("note").textContent).toBe("예약하려면 위의 개발용 바에서 게스트 행위자를 고르세요.");
  });

  it("이유의 순서와 날짜별 사유 함수", () => {
    const base = { complete: true, loading: false, failed: false, available: true, role: "guest" as const };
    expect(reserveBlockReason(base)).toBeNull();
    expect(reserveBlockReason({ ...base, complete: false, failed: true })).toBe("날짜와 인원을 정하면 예약할 수 있습니다.");
    expect(reserveBlockReason({ ...base, loading: true })).toBe("가용 수와 예상 금액을 확인하는 중입니다.");
    expect(reserveBlockReason({ ...base, failed: true, available: false })).toBe("가용 수나 예상 금액을 못 읽어 예약할 수 없습니다.");
    expect(reserveBlockReason({ ...base, available: false, role: "public" })).toBe("이 조건으로는 예약할 수 없습니다. 날짜별 사유를 보세요.");
    expect(reserveBlockReason({ ...base, role: "public" })).toBe("예약하려면 위의 개발용 바에서 게스트 행위자를 고르세요.");

    const a = { missingRateDates: ["2026-10-02"], reasons: ["OCCUPANCY_EXCEEDED" as const] };
    expect(dayReasonOf({ date: "2026-10-01", availableCount: null }, a, 2)).toBe("판매 안 함");
    expect(dayReasonOf({ date: "2026-10-02", availableCount: 3 }, a, 2)).toBe("요금 없음");
    expect(dayReasonOf({ date: "2026-10-03", availableCount: 0 }, a, 2)).toBe("마감");
    expect(dayReasonOf({ date: "2026-10-04", availableCount: 3 }, a, 2)).toBe("인원 초과 (최대 2명)");
    expect(dayReasonOf({ date: "2026-10-04", availableCount: 3 }, { missingRateDates: [], reasons: [] }, 2)).toBe("");
  });
});
