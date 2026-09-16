import Link from "next/link";
import type { PropertySearchResult } from "@/lib/api/types";
import { regionLabel } from "@/lib/regions";
import { Money } from "../Money";

export interface PropertyCardProps {
  result: PropertySearchResult;
  // 검색 조건 넷의 쿼리 문자열. G2와 G3에 그대로 전달
  query: string;
}

// G1의 숙소 카드. 머리는 G2, 객실 줄은 G3(계약 2절 G1 행). 서버가 준 순서 그대로
export function PropertyCard({ result, query }: PropertyCardProps) {
  const { property, availableRoomTypes } = result;
  return (
    <article className="rounded-card border border-line bg-surface">
      <header className="flex items-start justify-between gap-4 border-b border-line px-5 py-4">
        <div className="flex flex-col gap-1">
          <Link href={`/properties/${property.id}${query}`} className="text-18 font-semibold hover:underline">
            {property.name}
          </Link>
          <p className="text-13 text-ink-3">
            {regionLabel(property.regionCode)}, {property.address}
          </p>
        </div>
        <div className="text-right">
          <p className="text-12 text-ink-3">최저 총액</p>
          <Money amount={result.lowestTotalAmount} className="text-18 font-semibold" />
        </div>
      </header>
      <ul className="divide-y divide-line-soft">
        {availableRoomTypes.map((r) => (
          <li key={r.roomTypeId} className="flex items-center justify-between gap-4 px-5 py-3 text-14">
            <div className="flex items-center gap-3">
              <Link href={`/room-types/${r.roomTypeId}${query}`} className="font-semibold text-devbar hover:underline">
                {r.name}
              </Link>
              <span className="text-13 text-ink-3">최대 {r.maxOccupancy}명</span>
              <span className="text-13 text-ink-3 tabular-nums">남은 객실 {r.availableCount}</span>
            </div>
            <Money amount={r.totalAmount} className="font-semibold" />
          </li>
        ))}
      </ul>
    </article>
  );
}
