"use client";

import type { ReactNode } from "react";
import { isApiError, messageOf, placementOf } from "@/lib/errors";
import { Button } from "./Button";
import { Notice } from "./Notice";

export interface QueryErrorNoticeProps {
  error: unknown;
  onRetry?: () => void;
  retrying?: boolean;
  // 404 전면일 때 돌아갈 곳 같은 버튼
  action?: ReactNode;
}

// 읽기 실패의 자리. 401과 403과 404는 화면 안 Notice(404는 전면), 5xx와 네트워크는 같은 Notice에 다시 시도 버튼.
// 화면 위 띠는 쓰기 실패에 쓴다. 읽기가 실패하면 화면에 그릴 것이 없어 그 자리에 낸다
export function QueryErrorNotice({ error, onRetry, retrying, action }: QueryErrorNoticeProps) {
  const notFound = isApiError(error) && error.code === "RESOURCE_NOT_FOUND";
  const retryable = placementOf(error) === "banner" && !!onRetry;
  return (
    <Notice
      title={notFound ? "찾을 수 없습니다" : "불러오지 못했습니다"}
      fullPage={notFound}
      action={
        retryable ? (
          <Button variant="outline" onClick={onRetry} loading={retrying}>
            다시 시도
          </Button>
        ) : (
          action
        )
      }
    >
      {notFound ? "없거나 볼 수 없는 항목입니다. 주소를 확인하세요." : messageOf(error)}
    </Notice>
  );
}
