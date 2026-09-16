export interface SkeletonProps {
  // 줄 수. 카드나 표 자리에 회색 줄을 그린다
  lines?: number;
  className?: string;
}

export function Skeleton({ lines = 3, className = "" }: SkeletonProps) {
  return (
    <div aria-busy="true" aria-label="불러오는 중" className={`flex flex-col gap-2 ${className}`}>
      {Array.from({ length: lines }, (_, i) => (
        <div key={i} className="h-4 animate-pulse rounded-badge bg-line-soft" style={{ width: `${100 - (i % 3) * 15}%` }} />
      ))}
    </div>
  );
}
