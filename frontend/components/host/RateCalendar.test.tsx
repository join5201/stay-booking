import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { HttpResponse, http } from "msw";
import { afterEach, describe, expect, it } from "vitest";
import { errorBody, rate } from "@/lib/api/mocks/fixtures";
import { requestLog } from "@/lib/api/mocks/handlers";
import { server } from "@/lib/api/mocks/server";
import { queryWrapper, setActorCookie, setupMockServer } from "@/lib/api/mocks/test-utils";
import { BannerProvider } from "../Banner";
import { RateCalendar } from "./RateCalendar";
import { amountError } from "./RateCellPanel";

setupMockServer();
afterEach(() => cleanup());

const PERIOD = { from: "2026-10-01", to: "2026-10-03" };

function renderCalendar() {
  setActorCookie("host_001");
  const { Wrapper } = queryWrapper();
  render(
    <Wrapper>
      <BannerProvider>
        <RateCalendar roomTypeId="rt_001" period={PERIOD} />
      </BannerProvider>
    </Wrapper>,
  );
}

const writes = () => requestLog.filter((r) => r.method !== "GET");
const cell = (date: string) => screen.getByRole("button", { name: date });
const amountInput = () => screen.getByLabelText(/^요금/) as HTMLInputElement;

async function openCell(date: string) {
  await waitFor(() => cell(date));
  fireEvent.click(cell(date));
}

describe("H5 요금 달력", () => {
  it("RATE-03을 to 제외로 부르고 칸에 금액과 미등록을 그린다. 일괄 등록 안내", async () => {
    renderCalendar();
    await waitFor(() => cell("2026-10-01"));
    expect(requestLog[0].path).toBe("/api/v1/room-types/rt_001/rates?from=2026-10-01&to=2026-10-04");
    expect(within(cell("2026-10-01")).getByText("120,000원")).toBeTruthy();
    expect(within(cell("2026-10-03")).getByText("미등록")).toBeTruthy();
    expect(screen.getByText("요금은 날짜별로 등록합니다. 일괄 등록은 없습니다.", { exact: false })).toBeTruthy();
    expect(screen.queryByRole("button", { name: "일괄 등록" })).toBeNull();
  });

  it("누락 칸은 RATE-01. 직전 날짜 금액을 미리 채우고 본문에 currency KRW가 붙는다", async () => {
    renderCalendar();
    await openCell("2026-10-03");
    screen.getByRole("dialog", { name: "2026-10-03 요금 등록" });
    expect(amountInput().value).toBe("120000");

    fireEvent.change(amountInput(), { target: { value: "0" } });
    fireEvent.click(screen.getByRole("button", { name: "등록" }));
    expect(screen.getByText("요금은 1원부터 10억원까지입니다.")).toBeTruthy();
    expect(writes()).toHaveLength(0);

    fireEvent.change(amountInput(), { target: { value: "130000" } });
    fireEvent.click(screen.getByRole("button", { name: "등록" }));
    await screen.findByText("요금을 등록했습니다.");
    expect(writes()[0].method).toBe("POST");
    expect(writes()[0].path).toBe("/api/v1/room-types/rt_001/rates");
    expect(writes()[0].body).toEqual({ date: "2026-10-03", amount: 130000, currency: "KRW" });
  });

  it("있는 칸은 RATE-04를 읽고 RATE-02에 version과 amount만 보낸다(currency 없음)", async () => {
    renderCalendar();
    await openCell("2026-10-01");
    screen.getByRole("dialog", { name: "2026-10-01 요금 수정" });
    await waitFor(() => expect(amountInput().value).toBe("120000"));
    expect(requestLog.some((r) => r.method === "GET" && r.path === "/api/v1/room-types/rt_001/rates/2026-10-01")).toBe(true);

    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    expect(screen.getByText("바뀐 내용이 없습니다.")).toBeTruthy();
    expect(writes()).toHaveLength(0);

    fireEvent.change(amountInput(), { target: { value: "150000" } });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    await screen.findByText("요금을 저장했습니다.");
    expect(writes()[0].method).toBe("PATCH");
    expect(writes()[0].path).toBe("/api/v1/room-types/rt_001/rates/2026-10-01");
    expect(writes()[0].body).toEqual({ version: rate.version, amount: 150000 });
  });

  it("VERSION_CONFLICT면 새로 읽기 뒤 오른 version으로 저장한다", async () => {
    let reads = 0;
    const patchBodies: unknown[] = [];
    server.use(
      http.get("*/api/v1/room-types/:roomTypeId/rates/:date", () => {
        reads += 1;
        return HttpResponse.json(reads === 1 ? rate : { ...rate, version: 2, amount: 125000 });
      }),
      http.patch("*/api/v1/room-types/:roomTypeId/rates/:date", async ({ request }) => {
        const body = (await request.json()) as { version: number };
        patchBodies.push(body);
        if (body.version === rate.version) return HttpResponse.json(errorBody("VERSION_CONFLICT", "conflict"), { status: 409 });
        return HttpResponse.json({ ...rate, ...body, version: body.version + 1 });
      }),
    );
    renderCalendar();
    await openCell("2026-10-01");
    await waitFor(() => expect(amountInput().value).toBe("120000"));
    fireEvent.change(amountInput(), { target: { value: "150000" } });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    await screen.findByText("다른 곳에서 먼저 수정되어 저장하지 못했습니다. 새로 읽은 뒤 다시 저장하세요.");

    fireEvent.click(screen.getByRole("button", { name: "새로 읽기" }));
    await waitFor(() => expect(screen.queryByText("새로 읽기")).toBeNull());
    // 내가 고친 값은 남는다
    expect(amountInput().value).toBe("150000");
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    await screen.findByText("요금을 저장했습니다.");
    expect(patchBodies).toEqual([
      { version: 1, amount: 150000 },
      { version: 2, amount: 150000 },
    ]);
  });

  it("등록이 RESOURCE_ALREADY_EXISTS면 수정 폼으로 전환하고 RATE-04를 읽는다", async () => {
    server.use(http.post("*/api/v1/room-types/:roomTypeId/rates", () => HttpResponse.json(errorBody("RESOURCE_ALREADY_EXISTS", "exists"), { status: 409 })));
    renderCalendar();
    await openCell("2026-10-03");
    fireEvent.click(screen.getByRole("button", { name: "등록" }));
    await screen.findByRole("dialog", { name: "2026-10-03 요금 수정" });
    await waitFor(() => expect(requestLog.some((r) => r.method === "GET" && r.path === "/api/v1/room-types/rt_001/rates/2026-10-03")).toBe(true));
  });
});

describe("요금 화면 검사", () => {
  it("빈 칸, 소수, 범위 밖", () => {
    expect(amountError(null)).toBe("요금을(를) 입력하세요.");
    expect(amountError(1000.5)).toBe("요금은(는) 정수입니다.");
    expect(amountError(0)).toBe("요금은 1원부터 10억원까지입니다.");
    expect(amountError(1_000_000_001)).toBe("요금은 1원부터 10억원까지입니다.");
    expect(amountError(1)).toBeUndefined();
    expect(amountError(1_000_000_000)).toBeUndefined();
  });
});
