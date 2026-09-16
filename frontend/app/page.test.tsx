import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { HttpResponse, http } from "msw";
import { afterEach, describe, expect, it, vi } from "vitest";
import { errorBody, page as pageOf, property, roomType, searchResult } from "@/lib/api/mocks/fixtures";
import { requestLog } from "@/lib/api/mocks/handlers";
import { server } from "@/lib/api/mocks/server";
import { queryWrapper, setupMockServer } from "@/lib/api/mocks/test-utils";
import SearchPage from "./page";

const push = vi.fn();
let search = "";
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push, replace: push }),
  useSearchParams: () => new URLSearchParams(search),
}));

setupMockServer();
afterEach(() => {
  cleanup();
  push.mockReset();
  search = "";
});

const QUERY = "regionCode=SEOUL&checkIn=2026-10-01&checkOut=2026-10-03&guestCount=2";

function renderSearch(query = "") {
  search = query;
  const { Wrapper } = queryWrapper();
  render(
    <Wrapper>
      <SearchPage />
    </Wrapper>,
  );
}

const input = (label: string) => screen.getByLabelText(label, { exact: false }) as HTMLInputElement;
const submit = () => fireEvent.click(screen.getByRole("button", { name: "검색" }));

describe("G1 검색", () => {
  it("쿼리가 없으면 안내만 있고 요청이 없다", async () => {
    renderSearch();
    expect(screen.getByText("지역과 체크인과 체크아웃과 인원을 넣고 검색하세요.")).toBeTruthy();
    await new Promise((r) => setTimeout(r, 30));
    expect(requestLog).toHaveLength(0);
  });

  it("W08의 G1 몫. 체크아웃이 체크인보다 앞이면 필드 문구이고 URL도 요청도 없다", () => {
    renderSearch();
    fireEvent.change(screen.getByLabelText("지역", { exact: false }), { target: { value: "SEOUL" } });
    fireEvent.change(input("체크인"), { target: { value: "2026-10-03" } });
    fireEvent.change(input("체크아웃"), { target: { value: "2026-10-01" } });
    submit();
    expect(screen.getByText("체크아웃은 체크인보다 뒤여야 합니다.")).toBeTruthy();
    expect(push).not.toHaveBeenCalled();
    expect(requestLog).toHaveLength(0);
  });

  it("검색 버튼은 URL만 바꾼다. 넷과 page 없이", () => {
    renderSearch();
    fireEvent.change(screen.getByLabelText("지역", { exact: false }), { target: { value: "SEOUL" } });
    fireEvent.change(input("체크인"), { target: { value: "2026-10-01" } });
    fireEvent.change(input("체크아웃"), { target: { value: "2026-10-03" } });
    fireEvent.change(input("인원"), { target: { value: "2" } });
    submit();
    expect(push).toHaveBeenCalledWith(`/?${QUERY}`);
  });

  it("URL의 넷이 다 있으면 SEARCH-01. 카드 머리는 G2, 객실 줄은 G3에 쿼리 그대로", async () => {
    renderSearch(QUERY);
    await screen.findByText(property.name);
    expect(requestLog[0].path).toBe(`/api/v1/search/properties?${QUERY}&page=0&size=20`);
    expect((screen.getByRole("link", { name: property.name }) as HTMLAnchorElement).getAttribute("href")).toBe(`/properties/${property.id}?${QUERY}`);
    expect((screen.getByRole("link", { name: roomType.name }) as HTMLAnchorElement).getAttribute("href")).toBe(`/room-types/${roomType.id}?${QUERY}`);
    expect(screen.getByText("남은 객실 2")).toBeTruthy();
    expect(screen.getByText("2026-10-01 부터 2026-10-03 전까지 2박, 2명. 숙소 1곳")).toBeTruthy();
  });

  it("빈 결과는 빈 상태", async () => {
    server.use(http.get("*/api/v1/search/properties", () => HttpResponse.json(pageOf([]))));
    renderSearch(QUERY);
    await screen.findByText("조건에 맞는 숙소가 없습니다");
  });

  it("서버 400의 details field는 필드 아래", async () => {
    server.use(http.get("*/api/v1/search/properties", () => HttpResponse.json(errorBody("INVALID_DATE_RANGE", "past", [{ field: "checkIn", reason: "체크인은 오늘 이후여야 합니다" }]), { status: 400 })));
    renderSearch("regionCode=SEOUL&checkIn=2020-10-01&checkOut=2020-10-03&guestCount=2");
    await screen.findByText("체크인은 오늘 이후여야 합니다");
    expect(screen.queryByText("불러오지 못했습니다")).toBeNull();
  });

  it("field 없는 400은 화면 안 Notice", async () => {
    server.use(http.get("*/api/v1/search/properties", () => HttpResponse.json(errorBody("INVALID_DATE_RANGE", "past"), { status: 400 })));
    renderSearch(QUERY);
    await screen.findByText("날짜를 확인하세요.");
  });

  it("페이지 버튼은 같은 넷에 page만", async () => {
    server.use(http.get("*/api/v1/search/properties", () => HttpResponse.json({ ...pageOf([searchResult]), totalPages: 3, totalElements: 41 })));
    renderSearch(QUERY);
    await screen.findByText(property.name);
    fireEvent.click(screen.getByRole("button", { name: "다음" }));
    await waitFor(() => expect(push).toHaveBeenCalledWith(`/?${QUERY}&page=1`));
  });
});
