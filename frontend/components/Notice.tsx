import type { ReactNode } from "react";

// 오류 표시 둘째 자리. 화면 안. 진한 회색 테두리에 흰 배경. 빨간색 없음(인계 문서 86행)
export interface NoticeProps {
  title?: string;
  children: ReactNode;
  // 새로 읽기, 행위자 선택 같은 버튼
  action?: ReactNode;
  // 404처럼 화면 전체를 대신할 때
  fullPage?: boolean;
  className?: string;
}

export function Notice({ title, children, action, fullPage, className = "" }: NoticeProps) {
  return (
    <div role="alert" className={`rounded-card border border-ink-3 bg-surface px-5 py-4 ${fullPage ? "mx-auto my-16 max-w-[560px] text-center" : ""} ${className}`}>
      {title ? <p className="mb-1 text-15 font-semibold">{title}</p> : null}
      <div className="text-14 text-ink-2">{children}</div>
      {action ? <div className={`mt-3 ${fullPage ? "flex justify-center" : ""}`}>{action}</div> : null}
    </div>
  );
}
