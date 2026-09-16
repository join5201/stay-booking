import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { HttpResponse, http } from "msw";
import { afterEach, describe, expect, it, vi } from "vitest";
import { errorBody, heldBooking, page } from "@/lib/api/mocks/fixtures";
import { requestLog } from "@/lib/api/mocks/handlers";
import { server } from "@/lib/api/mocks/server";
import { queryWrapper, setActorCookie, setupMockServer } from "@/lib/api/mocks/test-utils";
import { BookingList } from "./BookingList";
import { type BookingFilter, bookingFilterOf } from "./booking-text";

setupMockServer();
afterEach(() => cleanup());

function renderList(filter: BookingFilter = "ALL", pageNo = 0) {
  setActorCookie("guest_001");
  const onChange = vi.fn();
  const onOpen = vi.fn();
  const onSearch = vi.fn();
  const { Wrapper } = queryWrapper();
  render(
    <Wrapper>
      <BookingList filter={filter} page={pageNo} onChange={onChange} onOpen={onOpen} onSearch={onSearch} />
    </Wrapper>,
  );
  return { onChange, onOpen, onSearch };
}

const listPaths = () => requestLog.filter((r) => r.path.startsWith("/api/v1/bookings")).map((r) => r.path);

describe("bookingFilterOf", () => {
  it("네 상태만 통과하고 나머지는 전체", () => {
    expect(bookingFilterOf("HELD")).toBe("HELD");
    expect(bookingFilterOf("CANCELED")).toBe("CANCELED");
    expect(bookingFilterOf("ALL")).toBe("ALL");
    expect(bookingFilterOf("BOGUS")).toBe("ALL");
    expect(bookingFilterOf(null)).toBe("ALL");
  });
});

describe("G6 예약 목록", () => {
  it("전체는 BOOK-02에 page와 size만. 줄마다 CAT-08과 CAT-03 이름, 숙박과 금액과 배지. 상세와 줄 클릭이 onOpen, 필터가 onChange", async () => {
    const { onChange, onOpen } = renderList();
    await screen.findByText("리버 트윈");
    expect(listPaths()).toEqual(["/api/v1/bookings?page=0&size=20"]);
    await screen.findByText("한강 뷰 스테이");
    expect(requestLog.some((r) => r.path === "/api/v1/room-types/rt_001")).toBe(true);
    expect(requestLog.some((r) => r.path === "/api/v1/properties/prop_001")).toBe(true);
    const table = screen.getByRole("table");
    expect(within(table).getByText("2026-10-01 부터 2026-10-03 전까지 2박, 2명")).toBeTruthy();
    expect(within(table).getByText("216,000원")).toBeTruthy();
    expect(within(table).getByText("선점")).toBeTruthy();

    fireEvent.click(screen.getByRole("button", { name: "상세" }));
    expect(onOpen).toHaveBeenCalledTimes(1);
    expect(onOpen).toHaveBeenCalledWith(heldBooking.id);
    fireEvent.click(within(table).getByText("리버 트윈"));
    expect(onOpen).toHaveBeenCalledTimes(2);

    fireEvent.click(screen.getByRole("button", { name: "확정" }));
    expect(onChange).toHaveBeenCalledWith("CONFIRMED", 0);
    expect(listPaths()).toHaveLength(1);
  });

  it("필터와 페이지가 BOOK-02의 status와 page. 페이지 이동은 필터를 유지", async () => {
    server.use(http.get("*/api/v1/bookings", () => HttpResponse.json({ ...page([heldBooking]), page: 1, totalPages: 3 })));
    const { onChange } = renderList("CONFIRMED", 1);
    await screen.findByText("리버 트윈");
    expect(listPaths()).toEqual([]);
    expect(screen.getByRole("button", { name: "확정" }).getAttribute("aria-pressed")).toBe("true");
    expect(screen.getByText("2 / 3")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "다음" }));
    expect(onChange).toHaveBeenCalledWith("CONFIRMED", 2);
  });

  it("status와 page가 요청 쿼리에 실린다", async () => {
    renderList("EXPIRED", 2);
    await waitFor(() => expect(listPaths()).toEqual(["/api/v1/bookings?page=2&size=20&status=EXPIRED"]));
  });

  it("비어 있으면 빈 상태와 숙소 찾기", async () => {
    server.use(http.get("*/api/v1/bookings", () => HttpResponse.json(page([]))));
    const { onSearch } = renderList();
    await screen.findByText("예약이 없습니다");
    fireEvent.click(screen.getByRole("button", { name: "숙소 찾기" }));
    expect(onSearch).toHaveBeenCalledTimes(1);
  });

  it("401 ACTOR_REQUIRED는 개발용 바를 가리키는 Notice", async () => {
    server.use(http.get("*/api/v1/bookings", () => HttpResponse.json(errorBody("ACTOR_REQUIRED", "actor"), { status: 401 })));
    renderList();
    await screen.findByText("행위자가 필요합니다. 위의 개발용 바에서 행위자를 고르세요.");
    expect(screen.queryByRole("table")).toBeNull();
  });
});
