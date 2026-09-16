import type { SearchValues, SearchErrors } from "@/components/SearchForm";
import type { SearchParams, StayParams } from "./api/types";
import { daysBetween, isYmd } from "./dates";
import { integerError } from "./forms";
import { isRegionCode } from "./regions";

// 게스트 화면의 검색 조건 넷. G1이 URL에 두고 G2와 G3과 G4가 그대로 이어받는다(계약 2절 G1부터 G4 행).
// 화면 검사가 서버보다 먼저(W08의 G1 몫). 날짜 순서, 30박, 인원 1부터 100, 지역 코드

export const MAX_NIGHTS = 30;
export const GUEST_RULE = { label: "인원", min: 1, max: 100 };

const KEYS = ["regionCode", "checkIn", "checkOut", "guestCount"] as const;

// URL 쿼리에서 넷을 읽는다. 없거나 모양이 틀리면 빈 값
export function searchValuesOf(params: URLSearchParams): SearchValues {
  const region = params.get("regionCode") ?? "";
  const checkIn = params.get("checkIn") ?? "";
  const checkOut = params.get("checkOut") ?? "";
  const guests = Number(params.get("guestCount"));
  return {
    regionCode: isRegionCode(region) ? region : "",
    checkIn: isYmd(checkIn) ? checkIn : "",
    checkOut: isYmd(checkOut) ? checkOut : "",
    guestCount: Number.isInteger(guests) && guests > 0 ? guests : null,
  };
}

// 넷을 쿼리 문자열로. 있는 것만. 없으면 빈 문자열이라 경로 뒤에 그대로 붙인다
export function searchQueryOf(values: Partial<SearchValues>, extra: Record<string, string | number | undefined> = {}): string {
  const q = new URLSearchParams();
  for (const key of KEYS) {
    const v = values[key];
    if (v !== undefined && v !== null && v !== "") q.set(key, String(v));
  }
  for (const [k, v] of Object.entries(extra)) {
    if (v !== undefined && v !== "") q.set(k, String(v));
  }
  const s = q.toString();
  return s ? `?${s}` : "";
}

// 검사 없이 값이 다 있는지만. 훅의 enabled 판단은 훅 쪽 complete 함수가 한다
export function stayParamsOf(values: SearchValues): Partial<StayParams> {
  return {
    checkIn: values.checkIn || undefined,
    checkOut: values.checkOut || undefined,
    guestCount: values.guestCount ?? undefined,
  };
}

export function searchParamsOf(values: SearchValues): Partial<SearchParams> {
  return { regionCode: values.regionCode || undefined, ...stayParamsOf(values) };
}

// 숙박 조건 셋의 화면 검사. 순서와 30박과 인원
export function validateStay(values: Pick<SearchValues, "checkIn" | "checkOut" | "guestCount">): SearchErrors {
  const errors: SearchErrors = {};
  if (!isYmd(values.checkIn)) errors.checkIn = "체크인을(를) 입력하세요.";
  if (!isYmd(values.checkOut)) errors.checkOut = "체크아웃을(를) 입력하세요.";
  if (!errors.checkIn && !errors.checkOut) {
    const nights = daysBetween(values.checkIn, values.checkOut);
    if (nights <= 0) errors.checkOut = "체크아웃은 체크인보다 뒤여야 합니다.";
    else if (nights > MAX_NIGHTS) errors.checkOut = `최대 ${MAX_NIGHTS}박입니다.`;
  }
  errors.guestCount = integerError(values.guestCount, GUEST_RULE);
  return errors;
}

// G1의 검사. 지역 코드까지
export function validateSearch(values: SearchValues): SearchErrors {
  const errors = validateStay(values);
  if (!values.regionCode) errors.regionCode = "지역을(를) 고르세요.";
  return errors;
}

// 값이 넷 다 있고 검사도 통과인가. G1이 SEARCH-01을 부르는 조건
export function searchComplete(values: SearchValues): boolean {
  return Object.values(validateSearch(values)).every((e) => !e);
}

export function stayComplete(values: SearchValues): boolean {
  return Object.values(validateStay(values)).every((e) => !e);
}

export function nightsOf(values: Pick<SearchValues, "checkIn" | "checkOut">): number {
  return isYmd(values.checkIn) && isYmd(values.checkOut) ? daysBetween(values.checkIn, values.checkOut) : 0;
}
