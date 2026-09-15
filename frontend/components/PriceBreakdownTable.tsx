import type { PriceSnapshot } from "@/lib/api/types";
import { Money } from "./Money";

export interface PriceBreakdownTableProps {
  price: PriceSnapshot;
  // G3와 G4는 예상 금액이라 고정 문구를 붙인다. G7의 priceSnapshot은 확정값이라 뺀다
  estimate?: boolean;
}

// 날짜별 표와 합계. 체크아웃 날은 줄에 없다(context.md 7절)
export function PriceBreakdownTable({ price, estimate }: PriceBreakdownTableProps) {
  const discounted = price.discountTotalAmount > 0;
  return (
    <div className="rounded-card border border-line bg-surface">
      <table className="w-full border-collapse text-14">
        <thead>
          <tr className="bg-surface-2 text-12 text-ink-3">
            <th scope="col" className="border-b border-line px-3 py-2 text-left font-semibold">날짜</th>
            <th scope="col" className="border-b border-line px-3 py-2 text-right font-semibold">기본</th>
            <th scope="col" className="border-b border-line px-3 py-2 text-right font-semibold">할인</th>
            <th scope="col" className="border-b border-line px-3 py-2 text-right font-semibold">금액</th>
          </tr>
        </thead>
        <tbody>
          {price.days.map((d) => (
            <tr key={d.date} className="border-b border-line-soft">
              <td className="px-3 py-2 tabular-nums">{d.date}</td>
              <td className="px-3 py-2 text-right"><Money amount={d.baseAmount} /></td>
              <td className="px-3 py-2 text-right text-ink-3">{d.discountAmount > 0 ? <>- <Money amount={d.discountAmount} /></> : "-"}</td>
              <td className="px-3 py-2 text-right"><Money amount={d.finalAmount} /></td>
            </tr>
          ))}
        </tbody>
        <tfoot>
          {discounted ? (
            <tr className="text-13 text-ink-2">
              <td colSpan={3} className="px-3 py-2">
                할인 합계{price.appliedPromotion ? ` (${price.appliedPromotion.name} ${price.appliedPromotion.discountRate}%)` : ""}
              </td>
              <td className="px-3 py-2 text-right">- <Money amount={price.discountTotalAmount} /></td>
            </tr>
          ) : null}
          <tr className="text-16 font-semibold">
            <td colSpan={3} className="px-3 py-3">
              {price.days.length}박 합계
            </td>
            <td className="px-3 py-3 text-right"><Money amount={price.totalAmount} /></td>
          </tr>
        </tfoot>
      </table>
      {estimate ? <p className="border-t border-line px-3 py-2 text-12 text-ink-3">예상 금액이며 확정 금액이 아닙니다.</p> : null}
    </div>
  );
}
