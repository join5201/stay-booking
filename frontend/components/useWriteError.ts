"use client";

import { useCallback, useState } from "react";
import { errorViewOf } from "@/lib/error-view";
import { useBanner } from "./Banner";

export interface WriteErrorState {
  // 서버 details의 field와 reason. 폼이 필드 아래에 그린다
  fields: Record<string, string>;
  // 409 VERSION_CONFLICT. 새로 읽기 버튼이 있는 Notice
  conflict: boolean;
  // 화면 전면 Notice
  notFound: string | null;
  // 화면 안 Notice
  notice: string | null;
}

const EMPTY: WriteErrorState = { fields: {}, conflict: false, notFound: null, notice: null };

// 쓰기 실패를 자리대로 보낸다. banner 자리는 화면 위 띠(다시 시도는 같은 요청), 나머지는 상태로 두어 폼이 그린다
export function useWriteError() {
  const banner = useBanner();
  const [state, setState] = useState<WriteErrorState>(EMPTY);

  const reset = useCallback(() => setState(EMPTY), []);

  const present = useCallback(
    (error: unknown, retry?: () => void) => {
      const view = errorViewOf(error);
      if (view.kind === "banner") {
        banner.show({ message: view.message, actionLabel: retry ? "다시 시도" : undefined, onAction: retry, retryAfterSec: view.retryAfterSec ?? undefined });
        setState(EMPTY);
        return;
      }
      if (view.kind === "field") {
        // 어느 필드인지 모르는 400은 화면 안 문구로
        setState({ ...EMPTY, fields: view.fields, notice: Object.keys(view.fields).length === 0 ? view.message : null });
        return;
      }
      if (view.kind === "conflict") {
        setState({ ...EMPTY, conflict: true });
        return;
      }
      if (view.kind === "notFound") {
        setState({ ...EMPTY, notFound: view.message });
        return;
      }
      setState({ ...EMPTY, notice: view.message });
    },
    [banner],
  );

  return { ...state, present, reset };
}
