import { describe, expect, it } from "vitest";
import { apiPeriodOf, isValidPeriod, overlappingDates, periodFromParams, previousItem } from "./calendar";

describe("달력 기간", () => {
  it("API에는 to에 하루를 더한다. 화면은 양끝 포함, 백엔드는 끝 날짜 제외", () => {
    expect(apiPeriodOf({ from: "2026-10-01", to: "2026-10-31" })).toEqual({ from: "2026-10-01", to: "2026-11-01" });
    expect(apiPeriodOf({ from: "2026-12-31", to: "2026-12-31" })).toEqual({ from: "2026-12-31", to: "2027-01-01" });
  });

  it("형식과 순서와 366일 상한", () => {
    expect(isValidPeriod({ from: "2026-10-01", to: "2026-10-01" })).toBe(true);
    expect(isValidPeriod({ from: "2026-10-01", to: "2027-10-01" })).toBe(true);
    expect(isValidPeriod({ from: "2026-10-01", to: "2027-10-02" })).toBe(false);
    expect(isValidPeriod({ from: "2026-10-02", to: "2026-10-01" })).toBe(false);
    expect(isValidPeriod({ from: "2026-13-01", to: "2026-10-01" })).toBe(false);
  });

  it("URL의 from과 to가 맞으면 그대로, 없거나 틀리면 오늘부터 60일", () => {
    expect(periodFromParams(new URLSearchParams("from=2026-10-01&to=2026-10-31"), "2026-09-16")).toEqual({ from: "2026-10-01", to: "2026-10-31" });
    expect(periodFromParams(new URLSearchParams(""), "2026-09-16")).toEqual({ from: "2026-09-16", to: "2026-11-14" });
    expect(periodFromParams(new URLSearchParams("from=2026-10-31&to=2026-10-01"), "2026-09-16")).toEqual({ from: "2026-09-16", to: "2026-11-14" });
  });

  it("겹치는 날짜는 기간 안에서 이미 있는 날짜만 오름차순", () => {
    expect(overlappingDates({ from: "2026-10-01", to: "2026-10-05" }, ["2026-10-03", "2026-10-01", "2026-10-09"])).toEqual(["2026-10-01", "2026-10-03"]);
    expect(overlappingDates({ from: "2026-10-06", to: "2026-10-08" }, ["2026-10-03"])).toEqual([]);
  });

  it("직전 날짜 항목. 바로 전날이 없으면 그 앞에서 가장 가까운 날짜", () => {
    const items = [{ date: "2026-10-01", v: 1 }, { date: "2026-10-02", v: 2 }, { date: "2026-10-05", v: 5 }];
    expect(previousItem(items, "2026-10-03")?.v).toBe(2);
    expect(previousItem(items, "2026-10-09")?.v).toBe(5);
    expect(previousItem(items, "2026-10-01")).toBeNull();
  });
});
