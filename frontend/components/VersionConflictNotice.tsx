"use client";

import { Button } from "./Button";
import { Notice } from "./Notice";

export interface VersionConflictNoticeProps {
  // 새로 읽기. 단건 GET을 다시 받아 폼과 version을 바꾼다
  onReload: () => void;
  busy?: boolean;
}

// 409 VERSION_CONFLICT의 자리(인계 문서 83행). H2, H3, H4, H5, O2가 같이 쓴다
export function VersionConflictNotice({ onReload, busy }: VersionConflictNoticeProps) {
  return (
    <Notice
      title="저장하지 못했습니다"
      action={
        <Button variant="outline" onClick={onReload} loading={busy}>
          새로 읽기
        </Button>
      }
    >
      다른 곳에서 먼저 수정되어 저장하지 못했습니다. 새로 읽은 뒤 다시 저장하세요.
    </Notice>
  );
}
