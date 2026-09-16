"use client";

import { useState } from "react";
import { usePromotion, useUpdatePromotion } from "@/lib/api/hooks";
import type { Promotion, UpdatePromotionBody } from "@/lib/api/types";
import { useBanner } from "../Banner";
import { Button } from "../Button";
import { ConfirmSheet } from "../ConfirmSheet";
import { Notice } from "../Notice";
import { QueryErrorNotice } from "../QueryErrorNotice";
import { Skeleton } from "../Skeleton";
import { StatusBadge, promotionBadge } from "../StatusBadge";
import { useWriteError } from "../useWriteError";
import { PromotionForm, type PromotionSubmit } from "./PromotionForm";

export interface PromotionEditorProps {
  promotionId: string;
  // 저장 뒤 O1로. 취소도 같다
  onDone: () => void;
}

// O2 수정. 진입 시 PROMO-03, 저장은 PROMO-02(version 숨김, 바뀐 필드만). 머리의 사용 끄기와 사용 켜기는 확인 시트 뒤
// PROMO-02에 enabled와 version만(P11). 409면 새로 읽기(PROMO-03), 404는 전면
export function PromotionEditor({ promotionId, onDone }: PromotionEditorProps) {
  const banner = useBanner();
  const promotion = usePromotion(promotionId);
  const update = useUpdatePromotion(promotionId);
  const writeError = useWriteError();
  const [sheetFor, setSheetFor] = useState<boolean | null>(null);

  const save = (body: UpdatePromotionBody) => {
    writeError.reset();
    update.mutate(body, {
      onSuccess: () => {
        banner.notify("프로모션을 저장했습니다.");
        onDone();
      },
      onError: (error) => writeError.present(error, () => save(body)),
    });
  };

  // 사용 끄기와 켜기. 훅의 setQueryData가 기준(version 포함)을 바꾸고 폼은 내 입력값을 지킨다
  const toggle = (enabled: boolean, version: number) => {
    writeError.reset();
    update.mutate(
      { version, enabled },
      {
        onSuccess: () => {
          setSheetFor(null);
          banner.notify(enabled ? "프로모션을 켰습니다." : "프로모션을 껐습니다.");
        },
        onError: (error) => {
          setSheetFor(null);
          writeError.present(error, () => toggle(enabled, version));
        },
      },
    );
  };

  const reload = () => {
    writeError.reset();
    void promotion.refetch();
  };

  if (promotion.isPending) return <Skeleton lines={8} />;
  if (promotion.isError) return <QueryErrorNotice error={promotion.error} onRetry={() => void promotion.refetch()} retrying={promotion.isFetching} action={<Button variant="outline" onClick={onDone}>프로모션 목록으로</Button>} />;
  if (writeError.notFound) {
    return (
      <Notice fullPage title="찾을 수 없습니다" action={<Button variant="outline" onClick={onDone}>프로모션 목록으로</Button>}>
        {writeError.notFound}
      </Notice>
    );
  }

  const data: Promotion = promotion.data;
  const badge = promotionBadge(data.enabled);
  return (
    <div className="flex flex-col gap-5">
      <header className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <h1 className="text-22 font-semibold">{data.name}</h1>
          <StatusBadge tone={badge.tone}>{badge.label}</StatusBadge>
        </div>
        <Button variant="outline" onClick={() => setSheetFor(!data.enabled)} disabled={update.isPending || writeError.conflict}>
          {data.enabled ? "사용 끄기" : "사용 켜기"}
        </Button>
      </header>

      <PromotionForm
        baseline={data}
        saving={update.isPending}
        serverErrors={writeError.fields}
        notice={writeError.notice}
        conflict={writeError.conflict}
        onReload={reload}
        reloading={promotion.isFetching}
        onSubmit={(out: PromotionSubmit) => {
          if (out.kind === "update") save(out.body);
        }}
        onCancel={onDone}
      />

      <ConfirmSheet
        open={sheetFor !== null}
        title={sheetFor ? "프로모션 사용 켜기" : "프로모션 사용 끄기"}
        description={sheetFor ? "켜면 캠페인 기간 안의 견적에 다시 적용됩니다." : "끄면 새 견적과 예약에 적용되지 않습니다. 이미 만든 예약의 할인은 바뀌지 않습니다."}
        confirmLabel={sheetFor ? "사용 켜기" : "사용 끄기"}
        onConfirm={() => toggle(sheetFor === true, data.version)}
        onCancel={() => setSheetFor(null)}
        busy={update.isPending}
      />
    </div>
  );
}
