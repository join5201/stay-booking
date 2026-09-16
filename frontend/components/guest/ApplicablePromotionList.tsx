import type { ApplicablePromotions } from "@/lib/api/types";
import { Money } from "../Money";
import { StatusBadge } from "../StatusBadge";

export interface ApplicablePromotionListProps {
  promotions: ApplicablePromotions;
}

// G3의 적용 가능 프로모션. 선택은 서버가 한다(할인액 최대, 동률이면 ID 오름차순. P04). 선택된 것에 표시
export function ApplicablePromotionList({ promotions }: ApplicablePromotionListProps) {
  if (promotions.items.length === 0) {
    return <p className="text-13 text-ink-3">적용 가능한 프로모션이 없습니다.</p>;
  }
  return (
    <ul className="divide-y divide-line-soft rounded-card border border-line bg-surface">
      {promotions.items.map((p) => (
        <li key={p.id} className="flex items-center justify-between gap-4 px-4 py-3 text-14">
          <div className="flex items-center gap-2">
            <span className={p.selected ? "font-semibold" : "text-ink-2"}>{p.name}</span>
            <span className="text-13 text-ink-3 tabular-nums">{p.discountRate}%</span>
            {p.selected ? <StatusBadge tone="confirmed">적용</StatusBadge> : null}
          </div>
          <span className="tabular-nums text-ink-2">
            - <Money amount={p.discountAmount} />
          </span>
        </li>
      ))}
    </ul>
  );
}
