import { cleanup, render, screen, waitFor } from "@testing-library/react";
import { HttpResponse, http } from "msw";
import { afterEach, describe, expect, it, vi } from "vitest";
import { errorBody, property, roomType } from "@/lib/api/mocks/fixtures";
import { requestLog } from "@/lib/api/mocks/handlers";
import { server } from "@/lib/api/mocks/server";
import { queryWrapper, setupMockServer } from "@/lib/api/mocks/test-utils";
import PropertyPage from "./page";

const push = vi.fn();
let search = "";
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push, replace: push }),
  useSearchParams: () => new URLSearchParams(search),
  useParams: () => ({ propertyId: "prop_001" }),
}));

setupMockServer();
afterEach(() => {
  cleanup();
  push.mockReset();
  search = "";
});

const QUERY = "regionCode=SEOUL&checkIn=2026-10-01&checkOut=2026-10-03&guestCount=2";

function renderProperty(query = "") {
  search = query;
  const { Wrapper } = queryWrapper();
  render(
    <Wrapper>
      <PropertyPage />
    </Wrapper>,
  );
}

describe("G2 숙소 상세", () => {
  it("CAT-03과 CAT-09 진입 시. 객실 줄은 G3에 쿼리 그대로", async () => {
    renderProperty(QUERY);
    await screen.findByRole("heading", { name: property.name });
    await screen.findByRole("link", { name: roomType.name });
    await waitFor(() => expect(requestLog).toHaveLength(2));
    expect(requestLog.map((r) => r.path).sort()).toEqual(["/api/v1/properties/prop_001", "/api/v1/properties/prop_001/room-types?page=0&size=20"]);
    expect((screen.getByRole("link", { name: roomType.name }) as HTMLAnchorElement).getAttribute("href")).toBe(`/room-types/${roomType.id}?${QUERY}`);
    expect(screen.getByText("2026-10-01 부터 2026-10-03 전까지 2박, 2명, 서울 (SEOUL)")).toBeTruthy();
  });

  it("없는 id는 전면 Notice와 검색으로", async () => {
    server.use(http.get("*/api/v1/properties/:propertyId", () => HttpResponse.json(errorBody("RESOURCE_NOT_FOUND", "none"), { status: 404 })));
    renderProperty();
    await screen.findByText("찾을 수 없습니다");
    expect(screen.getByRole("button", { name: "검색으로" })).toBeTruthy();
  });
});
