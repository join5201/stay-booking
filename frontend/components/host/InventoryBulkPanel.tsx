"use client";

import { useState } from "react";
import { useBulkCreateInventory } from "@/lib/api/hooks";
import { apiPeriodOf, MAX_PERIOD_DAYS, overlappingDates, type Period } from "@/lib/calendar";
import { daysBetween, isYmd } from "@/lib/dates";
import { isApiError } from "@/lib/errors";
import { integerError } from "@/lib/forms";
import { useBanner } from "../Banner";
import { Button } from "../Button";
import { DateInput } from "../DateInput";
import { Field } from "../Field";
import { Notice } from "../Notice";
import { NumberStepper } from "../NumberStepper";
import { SidePanel } from "../SidePanel";
import { useWriteError } from "../useWriteError";
import { TOTAL_COUNT_RULE } from "./InventoryCellPanel";

// H4 일괄 등록(INV-02). 백엔드는 한 날짜라도 있으면 전부 409 RESOURCE_ALREADY_EXISTS이고 건너뜀이 없다(계약 2-1절 2행).
// 그래서 달력에 보이는 기간 안에서만 고르게 해 INV-04의 items와 겹치는 날짜를 보내기 전에 막고, 그래도 409면 다시 읽은 목록으로 겹친 날짜를 보여 준다

export interface InventoryBulkPanelProps {
  roomTypeId: string;
  // 달력에 보이는 기간(양끝 포함). 일괄 등록 범위는 이 안이어야 겹침 검사가 완전하다
  period: Period;
  // INV-04의 items 날짜. 409 뒤 재조회로 바뀐다
  existingDates: readonly string[];
  refreshing: boolean;
  onClose: () => void;
}

export interface BulkDraft extends Period {
  totalCount: number | null;
}

export interface BulkErrors {
  period?: string;
  totalCount?: string;
}

// 화면 검사가 서버보다 먼저. 형식, 순서, 366일, 보이는 기간 안, 겹침
export function validateBulk(draft: BulkDraft, period: Period, existingDates: readonly string[]): BulkErrors {
  const errors: BulkErrors = { totalCount: integerError(draft.totalCount, TOTAL_COUNT_RULE) };
  if (!isYmd(draft.from) || !isYmd(draft.to)) errors.period = "시작과 끝 날짜를 입력하세요.";
  else if (daysBetween(draft.from, draft.to) < 0) errors.period = "끝 날짜가 시작 날짜보다 앞입니다.";
  else if (daysBetween(draft.from, draft.to) + 1 > MAX_PERIOD_DAYS) errors.period = `기간은 최대 ${MAX_PERIOD_DAYS}일입니다.`;
  else if (draft.from < period.from || draft.to > period.to) errors.period = `달력에 보이는 기간(${period.from}부터 ${period.to}까지) 안에서 고르세요.`;
  else {
    const overlap = overlappingDates(draft, existingDates);
    if (overlap.length > 0) errors.period = `이미 등록된 날짜가 있습니다: ${overlap.join(", ")}. 그 날짜는 칸을 눌러 수정하세요.`;
  }
  return errors;
}

export function InventoryBulkPanel({ roomTypeId, period, existingDates, refreshing, onClose }: InventoryBulkPanelProps) {
  const banner = useBanner();
  const bulk = useBulkCreateInventory(roomTypeId);
  const writeError = useWriteError();
  const [draft, setDraft] = useState<BulkDraft>({ from: period.from, to: period.to, totalCount: null });
  const [errors, setErrors] = useState<BulkErrors>({});
  // 서버가 409를 냈다. 겹친 날짜는 다시 읽은 existingDates에서 고른다
  const [conflicted, setConflicted] = useState<Period | null>(null);

  const set = <K extends keyof BulkDraft>(key: K, value: BulkDraft[K]) => {
    setDraft((d) => ({ ...d, [key]: value }));
    setErrors((e) => ({ ...e, [key === "totalCount" ? "totalCount" : "period"]: undefined }));
    setConflicted(null);
  };

  const send = (body: BulkDraft & { totalCount: number }) => {
    writeError.reset();
    setConflicted(null);
    bulk.mutate(
      { ...apiPeriodOf(body), totalCount: body.totalCount },
      {
        onSuccess: () => {
          banner.notify("재고를 일괄 등록했습니다.");
          onClose();
        },
        onError: (e) => {
          if (isApiError(e) && e.code === "RESOURCE_ALREADY_EXISTS") {
            // 훅의 onSettled가 INV-04를 무효화해 다시 읽는다. 그 결과가 existingDates로 온다
            setConflicted({ from: body.from, to: body.to });
            return;
          }
          writeError.present(e, () => send(body));
        },
      },
    );
  };

  const submit = () => {
    const found = validateBulk(draft, period, existingDates);
    setErrors(found);
    if (found.period || found.totalCount || draft.totalCount === null) return;
    send({ ...draft, totalCount: draft.totalCount });
  };

  const conflictDates = conflicted ? overlappingDates(conflicted, existingDates) : [];
  const periodError = errors.period;
  const totalError = errors.totalCount ?? writeError.fields.totalCount;

  return (
    <SidePanel open title="재고 일괄 등록" onClose={onClose}>
      <form
        id="inventory-bulk-form"
        noValidate
        onSubmit={(e) => {
          e.preventDefault();
          submit();
        }}
        className="flex flex-col gap-5"
      >
        {conflicted ? (
          <Notice title="전부 등록하지 않았습니다">
            이미 등록된 날짜가 있어 한 날짜도 등록하지 않았습니다.
            {refreshing ? " 겹친 날짜를 다시 읽는 중입니다." : conflictDates.length > 0 ? ` 겹친 날짜: ${conflictDates.join(", ")}. 그 날짜는 칸을 눌러 수정하세요.` : " 달력을 다시 확인하세요."}
          </Notice>
        ) : null}
        {writeError.notice ?? writeError.notFound ? <Notice>{writeError.notice ?? writeError.notFound}</Notice> : null}

        <p className="text-13 text-ink-3">
          기간은 양끝을 포함하고 달력에 보이는 {period.from}부터 {period.to}까지 안에서 고릅니다. 이미 등록된 날짜가 하나라도 있으면 전부 등록하지 않습니다.
        </p>
        <div className="flex gap-3">
          <Field label="시작" htmlFor="bulk-from" required error={periodError}>
            <DateInput id="bulk-from" value={draft.from} onChange={(v) => set("from", v)} min={period.from} max={period.to} invalid={!!periodError} className="w-44" />
          </Field>
          <Field label="끝" htmlFor="bulk-to" required>
            <DateInput id="bulk-to" value={draft.to} onChange={(v) => set("to", v)} min={period.from} max={period.to} invalid={!!periodError} className="w-44" />
          </Field>
        </div>
        <Field label="총 재고" htmlFor="bulk-total-count" required error={totalError} hint="날짜마다 같은 값. 0부터 100,000">
          <NumberStepper id="bulk-total-count" value={draft.totalCount} onChange={(v) => set("totalCount", v)} min={TOTAL_COUNT_RULE.min} max={TOTAL_COUNT_RULE.max} unit="개" invalid={!!totalError} />
        </Field>

        <div className="flex justify-end gap-2 border-t border-line pt-4">
          <Button variant="outline" onClick={onClose} disabled={bulk.isPending}>
            취소
          </Button>
          <Button type="submit" loading={bulk.isPending}>
            일괄 등록
          </Button>
        </div>
      </form>
    </SidePanel>
  );
}
