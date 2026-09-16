import Link from "next/link";
import type { SearchValues } from "../SearchForm";
import { nightsOf, searchQueryOf, stayComplete } from "@/lib/stay";
import { regionLabel } from "@/lib/regions";

export interface StayConditionsProps {
  values: SearchValues;
}

// G2와 G3 머리의 검색 조건 요약. 조건이 없으면 검색으로 안내. 바꾸기는 G1로 돌아가 쿼리를 그대로 채운다
export function StayConditions({ values }: StayConditionsProps) {
  if (!stayComplete(values)) {
    return (
      <p className="text-13 text-ink-3">
        날짜와 인원이 없습니다.{" "}
        <Link href={`/${searchQueryOf(values)}`} className="text-devbar hover:underline">
          검색에서 정하기
        </Link>
      </p>
    );
  }
  return (
    <p className="flex items-center gap-3 text-13 text-ink-2">
      <span className="tabular-nums">
        {values.checkIn} 부터 {values.checkOut} 전까지 {nightsOf(values)}박, {values.guestCount}명{values.regionCode ? `, ${regionLabel(values.regionCode)}` : ""}
      </span>
      <Link href={`/${searchQueryOf(values)}`} className="text-devbar hover:underline">
        조건 바꾸기
      </Link>
    </p>
  );
}
