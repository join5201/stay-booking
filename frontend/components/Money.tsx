const KRW = new Intl.NumberFormat("ko-KR");

// KRW 정수를 천 단위 구분과 원으로(P03). 통화는 KRW뿐이라 기호를 따로 안 붙인다
export function formatKrw(amount: number): string {
  return `${KRW.format(amount)}원`;
}

export interface MoneyProps {
  amount: number;
  className?: string;
}

export function Money({ amount, className = "" }: MoneyProps) {
  return <span className={`tabular-nums ${className}`}>{formatKrw(amount)}</span>;
}
