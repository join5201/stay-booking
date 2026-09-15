"use client";

import { useEffect, useRef, useState } from "react";
import { formatRemaining, remainingMs } from "@/lib/seoul-time";

export interface CountdownProps {
  // 둘 다 예약 응답의 UTC 시각. 남은 시간은 이 둘의 차이다(P01)
  expiresAt: string;
  serverNow: string;
  // 0에 닿으면 한 번. 부모가 BOOK-03을 다시 읽는다
  onZero?: () => void;
  className?: string;
}

// 새 응답(expiresAt, serverNow)마다 통째로 다시 만들어 그 값부터 다시 센다
export function Countdown(props: CountdownProps) {
  return <CountdownClock key={`${props.expiresAt}|${props.serverNow}`} {...props} />;
}

function CountdownClock({ expiresAt, serverNow, onZero, className = "" }: CountdownProps) {
  const total = remainingMs(expiresAt, serverNow);

  // 응답을 받은 시점의 브라우저 시계. 이후 경과만 잰다. 절대 시각은 쓰지 않는다
  const [startedAt] = useState(() => Date.now());
  const [now, setNow] = useState(startedAt);
  const remaining = Math.max(0, total - Math.max(0, now - startedAt));
  const zero = remaining === 0;

  useEffect(() => {
    if (zero) return;
    const id = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(id);
  }, [zero]);

  const onZeroRef = useRef(onZero);
  useEffect(() => {
    onZeroRef.current = onZero;
  }, [onZero]);

  // 한 응답에 한 번만
  useEffect(() => {
    if (zero) onZeroRef.current?.();
  }, [zero]);

  return (
    <span aria-live="polite" className={`tabular-nums ${className}`}>
      {formatRemaining(remaining)}
    </span>
  );
}
