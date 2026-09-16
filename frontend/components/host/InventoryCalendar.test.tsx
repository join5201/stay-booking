import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { HttpResponse, http } from "msw";
import { afterEach, describe, expect, it } from "vitest";
import { errorBody, inventory, inventoryRange } from "@/lib/api/mocks/fixtures";
import { requestLog } from "@/lib/api/mocks/handlers";
import { server } from "@/lib/api/mocks/server";
import { queryWrapper, setActorCookie, setupMockServer } from "@/lib/api/mocks/test-utils";
import { BannerProvider } from "../Banner";
import { validateBulk } from "./InventoryBulkPanel";
import { InventoryCalendar } from "./InventoryCalendar";

setupMockServer();
afterEach(() => cleanup());

// 화면 기간은 양끝 포함 10-01부터 10-03. fixture는 10-01과 10-02가 있고 10-03이 누락
const PERIOD = { from: "2026-10-01", to: "2026-10-03" };

function renderCalendar() {
  setActorCookie("host_001");
  const { Wrapper } = queryWrapper();
  render(
    <Wrapper>
      <BannerProvider>
        <InventoryCalendar roomTypeId="rt_001" period={PERIOD} />
      </BannerProvider>
    </Wrapper>,
  );
}

const writes = () => requestLog.filter((r) => r.method !== "GET");
const cell = (date: string) => screen.getByRole("button", { name: date });
const totalInput = () => screen.getByLabelText("총 재고", { exact: false }) as HTMLInputElement;

async function openCell(date: string) {
  await waitFor(() => cell(date));
  fireEvent.click(cell(date));
}

describe("H4 재고 달력", () => {
  it("INV-04를 to 제외로 부르고 칸에 재고 넷과 미등록을 그린다", async () => {
    renderCalendar();
    await waitFor(() => cell("2026-10-01"));
    expect(requestLog[0].path).toBe("/api/v1/room-types/rt_001/inventories?from=2026-10-01&to=2026-10-04");
    expect(within(cell("2026-10-01")).getByText("/ 총 5")).toBeTruthy();
    expect(within(cell("2026-10-01")).getByText("선점 1 판매 2")).toBeTruthy();
    expect(within(cell("2026-10-03")).getByText("미등록")).toBeTruthy();
    expect(screen.getByText("등록 2일, 미등록 1일.", { exact: false })).toBeTruthy();
  });

  it("누락 칸은 등록 폼. 직전 날짜 값을 미리 채우고 화면 검사 뒤 INV-01 본문", async () => {
    renderCalendar();
    await openCell("2026-10-03");
    screen.getByRole("dialog", { name: "2026-10-03 재고 등록" });
    expect(totalInput().value).toBe("5");
    expect(screen.getByText("직전 날짜 2026-10-02의 값 5을(를) 미리 채웠습니다.", { exact: false })).toBeTruthy();

    fireEvent.change(totalInput(), { target: { value: "" } });
    fireEvent.click(screen.getByRole("button", { name: "등록" }));
    expect(screen.getByText("총 재고을(를) 입력하세요.")).toBeTruthy();
    expect(writes()).toHaveLength(0);

    fireEvent.change(totalInput(), { target: { value: "7" } });
    fireEvent.click(screen.getByRole("button", { name: "등록" }));
    await screen.findByText("재고를 등록했습니다.");
    expect(writes()).toHaveLength(1);
    expect(writes()[0].method).toBe("POST");
    expect(writes()[0].path).toBe("/api/v1/room-types/rt_001/inventories");
    expect(writes()[0].body).toEqual({ date: "2026-10-03", totalCount: 7 });
    expect(screen.queryByRole("dialog")).toBeNull();
  });

  it("있는 칸은 INV-05를 읽고 최소 재고(선점 더하기 판매)가 먼저 막는다. 그 뒤 INV-03은 version과 totalCount", async () => {
    renderCalendar();
    await openCell("2026-10-01");
    screen.getByRole("dialog", { name: "2026-10-01 재고 수정" });
    await waitFor(() => expect(totalInput().value).toBe("5"));
    expect(requestLog.some((r) => r.method === "GET" && r.path === "/api/v1/room-types/rt_001/inventories/2026-10-01")).toBe(true);

    fireEvent.change(totalInput(), { target: { value: "2" } });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    expect(screen.getByText("선점 1과 판매 2를 더한 3 아래로 줄일 수 없습니다.")).toBeTruthy();
    expect(writes()).toHaveLength(0);

    fireEvent.change(totalInput(), { target: { value: "4" } });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    await screen.findByText("재고를 저장했습니다.");
    expect(writes()[0].method).toBe("PATCH");
    expect(writes()[0].path).toBe("/api/v1/room-types/rt_001/inventories/2026-10-01");
    expect(writes()[0].body).toEqual({ version: inventory.version, totalCount: 4 });
  });

  it("서버가 INVENTORY_BELOW_COMMITTED를 내면 INV-05를 다시 읽어 그 숫자로 문구를 채운다", async () => {
    let reads = 0;
    server.use(
      http.get("*/api/v1/room-types/:roomTypeId/inventories/:date", () => {
        reads += 1;
        // 두 번째 읽기부터 선점이 하나 늘어 있다
        return HttpResponse.json(reads === 1 ? inventory : { ...inventory, heldCount: 2, availableCount: 1 });
      }),
      http.patch("*/api/v1/room-types/:roomTypeId/inventories/:date", () => HttpResponse.json(errorBody("INVENTORY_BELOW_COMMITTED", "below committed"), { status: 409 })),
    );
    renderCalendar();
    await openCell("2026-10-01");
    await waitFor(() => expect(totalInput().value).toBe("5"));
    // 화면 검사(1 더하기 2)는 통과하는 3
    fireEvent.change(totalInput(), { target: { value: "3" } });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    await screen.findByText("선점 2과 판매 2를 더한 4 아래로 줄일 수 없습니다.");
    expect(reads).toBe(2);
    expect(totalInput().value).toBe("3");
  });

  it("일괄 등록. 겹치는 날짜는 보내기 전에 막고, 겹침이 없으면 INV-02 본문의 to는 하루 뒤", async () => {
    renderCalendar();
    await waitFor(() => cell("2026-10-01"));
    fireEvent.click(screen.getByRole("button", { name: "일괄 등록" }));
    const dialog = screen.getByRole("dialog", { name: "재고 일괄 등록" });
    fireEvent.change(within(dialog).getByLabelText("총 재고", { exact: false }), { target: { value: "3" } });
    fireEvent.click(within(dialog).getByRole("button", { name: "일괄 등록" }));
    expect(within(dialog).getByText("이미 등록된 날짜가 있습니다: 2026-10-01, 2026-10-02.", { exact: false })).toBeTruthy();
    expect(writes()).toHaveLength(0);

    fireEvent.change(within(dialog).getByLabelText("시작", { exact: false }), { target: { value: "2026-10-03" } });
    fireEvent.click(within(dialog).getByRole("button", { name: "일괄 등록" }));
    await screen.findByText("재고를 일괄 등록했습니다.");
    expect(writes()).toHaveLength(1);
    expect(writes()[0].path).toBe("/api/v1/room-types/rt_001/inventories/bulk");
    expect(writes()[0].body).toEqual({ from: "2026-10-03", to: "2026-10-04", totalCount: 3 });
  });

  it("W11. 일괄 등록이 409 RESOURCE_ALREADY_EXISTS면 INV-04를 다시 읽고 겹친 날짜를 보여 준다", async () => {
    let reads = 0;
    server.use(
      http.get("*/api/v1/room-types/:roomTypeId/inventories", () => {
        reads += 1;
        // 첫 읽기는 10-03 누락. 그사이 남이 등록해 두 번째 읽기부터는 10-03이 있다
        if (reads === 1) return HttpResponse.json(inventoryRange);
        return HttpResponse.json({ ...inventoryRange, items: [...inventoryRange.items, { ...inventory, date: "2026-10-03", version: 0 }], missingDates: [] });
      }),
      http.post("*/api/v1/room-types/:roomTypeId/inventories/bulk", () => HttpResponse.json(errorBody("RESOURCE_ALREADY_EXISTS", "exists"), { status: 409 })),
    );
    renderCalendar();
    await waitFor(() => cell("2026-10-01"));
    fireEvent.click(screen.getByRole("button", { name: "일괄 등록" }));
    const dialog = screen.getByRole("dialog", { name: "재고 일괄 등록" });
    fireEvent.change(within(dialog).getByLabelText("시작", { exact: false }), { target: { value: "2026-10-03" } });
    fireEvent.change(within(dialog).getByLabelText("총 재고", { exact: false }), { target: { value: "3" } });
    fireEvent.click(within(dialog).getByRole("button", { name: "일괄 등록" }));

    await within(dialog).findByText("겹친 날짜: 2026-10-03.", { exact: false });
    expect(reads).toBe(2);
    expect(within(dialog).getByText("전부 등록하지 않았습니다")).toBeTruthy();
    // 달력도 다시 읽은 값으로 바뀌어 10-03이 미등록이 아니다
    expect(within(cell("2026-10-03")).queryByText("미등록")).toBeNull();
  });

  it("등록이 RESOURCE_ALREADY_EXISTS면 수정 폼으로 자동 전환하고 INV-05를 읽는다", async () => {
    server.use(http.post("*/api/v1/room-types/:roomTypeId/inventories", () => HttpResponse.json(errorBody("RESOURCE_ALREADY_EXISTS", "exists"), { status: 409 })));
    renderCalendar();
    await openCell("2026-10-03");
    fireEvent.click(screen.getByRole("button", { name: "등록" }));
    await screen.findByRole("dialog", { name: "2026-10-03 재고 수정" });
    await waitFor(() => expect(requestLog.some((r) => r.method === "GET" && r.path === "/api/v1/room-types/rt_001/inventories/2026-10-03")).toBe(true));
    await waitFor(() => expect(totalInput().value).toBe("5"));
  });
});

describe("일괄 등록 화면 검사", () => {
  const period = { from: "2026-10-01", to: "2026-10-31" };

  it("형식, 순서, 366일, 보이는 기간 안, 겹침 순서로 막는다", () => {
    expect(validateBulk({ from: "", to: "2026-10-05", totalCount: 3 }, period, []).period).toBe("시작과 끝 날짜를 입력하세요.");
    expect(validateBulk({ from: "2026-10-05", to: "2026-10-01", totalCount: 3 }, period, []).period).toBe("끝 날짜가 시작 날짜보다 앞입니다.");
    expect(validateBulk({ from: "2026-10-01", to: "2027-10-02", totalCount: 3 }, { from: "2026-10-01", to: "2027-10-02" }, []).period).toBe("기간은 최대 366일입니다.");
    expect(validateBulk({ from: "2026-10-01", to: "2026-11-01", totalCount: 3 }, period, []).period).toBe("달력에 보이는 기간(2026-10-01부터 2026-10-31까지) 안에서 고르세요.");
    expect(validateBulk({ from: "2026-10-01", to: "2026-10-05", totalCount: 3 }, period, ["2026-10-04"]).period).toContain("2026-10-04");
    expect(validateBulk({ from: "2026-10-01", to: "2026-10-05", totalCount: null }, period, [])).toEqual({ totalCount: "총 재고을(를) 입력하세요." });
  });
});
