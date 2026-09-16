import { describe, expect, it } from "vitest";
import { searchComplete, searchQueryOf, searchValuesOf, stayComplete, validateSearch, validateStay } from "./stay";

const valid = { regionCode: "SEOUL" as const, checkIn: "2026-10-01", checkOut: "2026-10-03", guestCount: 2 };

describe("URL 쿼리와 값", () => {
  it("넷을 읽고 모양이 틀린 것은 빈 값", () => {
    expect(searchValuesOf(new URLSearchParams("regionCode=SEOUL&checkIn=2026-10-01&checkOut=2026-10-03&guestCount=2"))).toEqual(valid);
    expect(searchValuesOf(new URLSearchParams("regionCode=MARS&checkIn=2026-13-01&checkOut=abc&guestCount=0"))).toEqual({ regionCode: "", checkIn: "", checkOut: "", guestCount: null });
    expect(searchValuesOf(new URLSearchParams("guestCount=2.5"))).toEqual({ regionCode: "", checkIn: "", checkOut: "", guestCount: null });
  });

  it("쿼리 문자열은 있는 것만. 없으면 빈 문자열. 추가 칸은 뒤에", () => {
    expect(searchQueryOf(valid)).toBe("?regionCode=SEOUL&checkIn=2026-10-01&checkOut=2026-10-03&guestCount=2");
    expect(searchQueryOf({ ...valid, regionCode: "" })).toBe("?checkIn=2026-10-01&checkOut=2026-10-03&guestCount=2");
    expect(searchQueryOf({ regionCode: "", checkIn: "", checkOut: "", guestCount: null })).toBe("");
    expect(searchQueryOf(valid, { page: 2, rtPage: undefined })).toBe("?regionCode=SEOUL&checkIn=2026-10-01&checkOut=2026-10-03&guestCount=2&page=2");
  });
});

describe("W08의 G1 몫. 날짜 순서, 30박, 인원 1부터 100, 지역", () => {
  it("체크아웃은 체크인보다 뒤. 같은 날은 안 된다", () => {
    expect(validateStay({ ...valid, checkOut: "2026-10-01" }).checkOut).toBe("체크아웃은 체크인보다 뒤여야 합니다.");
    expect(validateStay({ ...valid, checkOut: "2026-09-30" }).checkOut).toBe("체크아웃은 체크인보다 뒤여야 합니다.");
    expect(validateStay(valid).checkOut).toBeUndefined();
  });

  it("최대 30박. 31박은 막고 30박은 통과", () => {
    expect(validateStay({ ...valid, checkOut: "2026-11-01" }).checkOut).toBe("최대 30박입니다.");
    expect(validateStay({ ...valid, checkOut: "2026-10-31" }).checkOut).toBeUndefined();
  });

  it("인원 1부터 100. 빈 칸은 필수 문구", () => {
    expect(validateStay({ ...valid, guestCount: 0 }).guestCount).toBe("인원은(는) 1부터 100까지입니다.");
    expect(validateStay({ ...valid, guestCount: 101 }).guestCount).toBe("인원은(는) 1부터 100까지입니다.");
    expect(validateStay({ ...valid, guestCount: null }).guestCount).toBe("인원을(를) 입력하세요.");
    expect(validateStay({ ...valid, guestCount: 100 }).guestCount).toBeUndefined();
  });

  it("빈 날짜는 필수 문구이고 순서 검사는 하지 않는다", () => {
    const e = validateStay({ ...valid, checkIn: "", checkOut: "" });
    expect(e.checkIn).toBe("체크인을(를) 입력하세요.");
    expect(e.checkOut).toBe("체크아웃을(를) 입력하세요.");
  });

  it("지역은 G1에서만 필수", () => {
    expect(validateSearch({ ...valid, regionCode: "" }).regionCode).toBe("지역을(를) 고르세요.");
    expect(validateStay(valid).regionCode).toBeUndefined();
    expect(searchComplete({ ...valid, regionCode: "" })).toBe(false);
    expect(stayComplete({ ...valid, regionCode: "" })).toBe(true);
    expect(searchComplete(valid)).toBe(true);
  });
});
