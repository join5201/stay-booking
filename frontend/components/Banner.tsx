"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";

// 오류 표시 셋째 자리. 화면 위 띠. 성공 알림도 여기(5초 뒤 닫힘). 배경은 파랑 하나(인계 문서 86행)
export interface BannerContent {
  message: string;
  // 다시 시도 같은 버튼 하나
  actionLabel?: string;
  onAction?: () => void;
  // Retry-After 초. 409 REQUEST_IN_PROGRESS에서만 온다(계약 2-1절 4행)
  retryAfterSec?: number;
  // 성공 알림은 5000. 오류는 없음(닫기 버튼)
  autoCloseMs?: number;
}

export interface BannerProps extends BannerContent {
  onClose: () => void;
}

export function Banner({ message, actionLabel, onAction, retryAfterSec, autoCloseMs, onClose }: BannerProps) {
  // 새 retryAfterSec가 오면 그 값부터 다시 센다. 렌더 중 파생 상태 갱신
  const [tracked, setTracked] = useState(retryAfterSec);
  const [left, setLeft] = useState(retryAfterSec ?? 0);
  if (tracked !== retryAfterSec) {
    setTracked(retryAfterSec);
    setLeft(retryAfterSec ?? 0);
  }

  useEffect(() => {
    if (left <= 0) return;
    const id = setTimeout(() => setLeft((v) => v - 1), 1000);
    return () => clearTimeout(id);
  }, [left]);

  useEffect(() => {
    if (!autoCloseMs) return;
    const id = setTimeout(onClose, autoCloseMs);
    return () => clearTimeout(id);
  }, [autoCloseMs, onClose]);

  return (
    <div role="status" className="flex items-center gap-3 bg-devbar px-6 py-2 text-14 text-white">
      <span className="flex-1">{message}</span>
      {left > 0 ? <span className="tabular-nums text-13 opacity-90">{left}초 뒤</span> : null}
      {actionLabel && onAction ? (
        <button type="button" onClick={onAction} disabled={left > 0} className="rounded-control border border-white/70 px-3 py-1 text-13 font-semibold disabled:opacity-60">
          {actionLabel}
        </button>
      ) : null}
      <button type="button" onClick={onClose} aria-label="닫기" className="px-1 text-16 leading-none opacity-90">
        x
      </button>
    </div>
  );
}

interface BannerApi {
  show: (content: BannerContent) => void;
  // 성공 알림. 5초 뒤 닫힘
  notify: (message: string) => void;
  clear: () => void;
}

const BannerContext = createContext<BannerApi | null>(null);

// 화면 위 띠는 한 번에 하나. 새 띠가 앞 띠를 대체한다
export function BannerProvider({ children }: { children: ReactNode }) {
  const [content, setContent] = useState<BannerContent | null>(null);
  const clear = useCallback(() => setContent(null), []);
  const api = useMemo<BannerApi>(
    () => ({
      show: (c) => setContent(c),
      notify: (message) => setContent({ message, autoCloseMs: 5000 }),
      clear,
    }),
    [clear],
  );
  return (
    <BannerContext.Provider value={api}>
      {content ? <Banner {...content} onClose={clear} /> : null}
      {children}
    </BannerContext.Provider>
  );
}

export function useBanner(): BannerApi {
  const api = useContext(BannerContext);
  if (!api) throw new Error("useBanner는 BannerProvider 안에서만 쓴다");
  return api;
}
