import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { HttpResponse, http } from "msw";
import { afterEach, describe, expect, it, vi } from "vitest";
import { errorBody, promotion } from "@/lib/api/mocks/fixtures";
import { requestLog } from "@/lib/api/mocks/handlers";
import { server } from "@/lib/api/mocks/server";
import { queryWrapper, setActorCookie, setupMockServer } from "@/lib/api/mocks/test-utils";
import PromotionsPage from "./page";

// 페이지는 라우터와 URL 쿼리를 읽는다. 둘만 흉내 낸다
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

function renderList(query = "") {
  search = query;
  setActorCookie("operator_001");
  const { Wrapper } = queryWrapper();
  render(
    <Wrapper>
      <PromotionsPage />
    </Wrapper>,
  );
}

describe("O1 목록", () => {
  it("전체는 enabled 없이 PROMO-04. 행에 할인율과 캠페인 기간과 상태", async () => {
    renderList();
    await screen.findByText(promotion.name);
    expect(requestLog[0].path).toBe("/api/v1/promotions?page=0&size=20");
    expect(screen.getByText(`${promotion.discountRate}%`)).toBeTruthy();
    expect(screen.getByText(`${promotion.campaignStartDate} 부터 ${promotion.campaignEndDate} 전까지`)).toBeTruthy();
    expect(within(screen.getByRole("table")).getByText("사용 중")).toBeTruthy();
    expect((screen.getByRole("link", { name: "수정" }) as HTMLAnchorElement).getAttribute("href")).toBe(`/operator/promotions/${promotion.id}/edit`);
  });

  it("URL의 enabled=false는 그대로 PROMO-04에 가고, 필터 버튼은 URL을 바꾼다", async () => {
    renderList("enabled=false");
    await screen.findByText(promotion.name);
    expect(requestLog[0].path).toBe("/api/v1/promotions?page=0&size=20&enabled=false");
    fireEvent.click(screen.getByRole("button", { name: "사용 중" }));
    expect(push).toHaveBeenCalledWith("/operator/promotions?enabled=true");
    fireEvent.click(screen.getByRole("button", { name: "전체" }));
    expect(push).toHaveBeenCalledWith("/operator/promotions");
  });

  it("403은 화면 안 Notice", async () => {
    server.use(http.get("*/api/v1/promotions", () => HttpResponse.json(errorBody("ACCESS_DENIED", "denied"), { status: 403 })));
    renderList();
    await screen.findByText("이 역할로는 할 수 없습니다.");
    await waitFor(() => expect(screen.queryByRole("button", { name: "다시 시도" })).toBeNull());
  });
});
