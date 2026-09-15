import { describe, expect, it } from "vitest";
import { addDays, daysBetween, eachDay, isYmd, weekdayOf } from "./dates";
import { formatRemaining, formatSeoul, remainingMs, seoulDateOf } from "./seoul-time";

describe("서울 시각", () => {
  it("UTC 시각을 서울로 바꾼다. 자정 경계에서 날짜가 넘어간다", () => {
    expect(formatSeoul("2026-09-15T15:30:00Z")).toBe("2026-09-16 00:30");
    expect(formatSeoul("2026-09-15T14:59:00Z", "date")).toBe("2026-09-15");
    expect(seoulDateOf("2026-09-15T15:00:00Z")).toBe("2026-09-16");
    expect(seoulDateOf("2026-09-15T14:59:59Z")).toBe("2026-09-15");
  });

  it("남은 시간은 두 시각의 차이이고 음수는 0", () => {
    expect(remainingMs("2026-09-15T10:10:00Z", "2026-09-15T10:00:00Z")).toBe(600_000);
    expect(remainingMs("2026-09-15T10:00:00Z", "2026-09-15T10:10:00Z")).toBe(0);
    expect(formatRemaining(600_000)).toBe("10:00");
    expect(formatRemaining(59_001)).toBe("01:00");
    expect(formatRemaining(0)).toBe("00:00");
    expect(formatRemaining(3_600_000)).toBe("1:00:00");
  });
});

describe("날짜 문자열", () => {
  it("형식과 실존 날짜", () => {
    expect(isYmd("2026-02-28")).toBe(true);
    expect(isYmd("2026-02-30")).toBe(false);
    expect(isYmd("2026-9-1")).toBe(false);
    expect(isYmd("")).toBe(false);
  });

  it("더하기와 차이와 나열", () => {
    expect(addDays("2026-12-31", 1)).toBe("2027-01-01");
    expect(daysBetween("2026-09-01", "2026-09-03")).toBe(2);
    expect(eachDay("2026-09-29", "2026-10-02")).toEqual(["2026-09-29", "2026-09-30", "2026-10-01", "2026-10-02"]);
    expect(weekdayOf("2026-09-15")).toBe(2);
  });
});
