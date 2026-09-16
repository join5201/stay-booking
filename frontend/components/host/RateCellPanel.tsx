"use client";

import { useState } from "react";
import { useCreateRate, useRate, useUpdateRate } from "@/lib/api/hooks";
import type { AdjustRateBody, DailyRate } from "@/lib/api/types";
import { isApiError } from "@/lib/errors";
import { integerError } from "@/lib/forms";
import { useBanner } from "../Banner";
import { Button } from "../Button";
import { Field } from "../Field";
import { formatKrw } from "../Money";
import { Notice } from "../Notice";
import { NumberStepper } from "../NumberStepper";
import { QueryErrorNotice } from "../QueryErrorNotice";
import { SidePanel } from "../SidePanel";
import { Skeleton } from "../Skeleton";
import { useWriteError } from "../useWriteError";
import { VersionConflictNotice } from "../VersionConflictNotice";

// H5 날짜 칸 패널. 누락이면 RATE-01(date, amount, currency KRW는 훅이 붙인다), 있으면 RATE-04를 읽은 뒤 RATE-02(version과 amount만. 계약 2-1절 3행)

// 요금 1부터 10억(backend RegisterRateRequest와 AdjustRateRequest)
export const AMOUNT_RULE = { label: "요금", min: 1, max: 1_000_000_000 };

// 범위 문구는 10억을 숫자 그대로 내지 않는다
export function amountError(value: number | null): string | undefined {
  const found = integerError(value, AMOUNT_RULE);
  if (!found) return undefined;
  return value !== null && Number.isInteger(value) ? "요금은 1원부터 10억원까지입니다." : found;
}

export interface RateCellPanelProps {
  roomTypeId: string;
  date: string;
  exists: boolean;
  previous: DailyRate | null;
  onClose: () => void;
}

export function RateCellPanel({ roomTypeId, date, exists, previous, onClose }: RateCellPanelProps) {
  const [editing, setEditing] = useState(exists);
  return (
    <SidePanel open title={`${date} 요금 ${editing ? "수정" : "등록"}`} onClose={onClose}>
      {editing ? <EditBody roomTypeId={roomTypeId} date={date} onClose={onClose} /> : <CreateBody roomTypeId={roomTypeId} date={date} previous={previous} onExists={() => setEditing(true)} onClose={onClose} />}
    </SidePanel>
  );
}

function AmountField({ value, onChange, error, hint, disabled }: { value: number | null; onChange: (v: number | null) => void; error?: string; hint: string; disabled?: boolean }) {
  return (
    <Field label="요금" htmlFor="rate-amount" required error={error} hint={hint}>
      <NumberStepper id="rate-amount" value={value} onChange={onChange} min={AMOUNT_RULE.min} max={AMOUNT_RULE.max} step={1000} unit="원 (KRW)" invalid={!!error} disabled={disabled} />
    </Field>
  );
}

function CreateBody({ roomTypeId, date, previous, onExists, onClose }: { roomTypeId: string; date: string; previous: DailyRate | null; onExists: () => void; onClose: () => void }) {
  const banner = useBanner();
  const create = useCreateRate(roomTypeId);
  const writeError = useWriteError();
  const [amount, setAmount] = useState<number | null>(previous?.amount ?? null);
  const [error, setError] = useState<string | undefined>();

  const save = (value: number) => {
    writeError.reset();
    create.mutate(
      { date, amount: value },
      {
        onSuccess: () => {
          banner.notify("요금을 등록했습니다.");
          onClose();
        },
        onError: (e) => {
          if (isApiError(e) && e.code === "RESOURCE_ALREADY_EXISTS") {
            onExists();
            return;
          }
          writeError.present(e, () => save(value));
        },
      },
    );
  };

  const submit = () => {
    const found = amountError(amount);
    setError(found);
    if (found || amount === null) return;
    save(amount);
  };

  const shown = error ?? writeError.fields.amount;
  return (
    <form
      id="rate-create-form"
      noValidate
      onSubmit={(e) => {
        e.preventDefault();
        submit();
      }}
      className="flex flex-col gap-5"
    >
      {writeError.notice ?? writeError.notFound ? <Notice>{writeError.notice ?? writeError.notFound}</Notice> : null}
      <AmountField
        value={amount}
        onChange={(v) => {
          setAmount(v);
          setError(undefined);
        }}
        error={shown}
        hint={previous ? `직전 날짜 ${previous.date}의 ${formatKrw(previous.amount)}을(를) 미리 채웠습니다. 1원부터 10억원` : "1원부터 10억원. 통화는 KRW 하나"}
      />
      <div className="flex justify-end gap-2 border-t border-line pt-4">
        <Button variant="outline" onClick={onClose} disabled={create.isPending}>
          취소
        </Button>
        <Button type="submit" loading={create.isPending}>
          등록
        </Button>
      </div>
    </form>
  );
}

function EditBody({ roomTypeId, date, onClose }: { roomTypeId: string; date: string; onClose: () => void }) {
  const banner = useBanner();
  const rate = useRate(roomTypeId, date);
  const update = useUpdateRate(roomTypeId);
  const writeError = useWriteError();

  const save = (body: AdjustRateBody) => {
    writeError.reset();
    update.mutate(
      { date, ...body },
      {
        onSuccess: () => {
          banner.notify("요금을 저장했습니다.");
          onClose();
        },
        onError: (e) => writeError.present(e, () => save(body)),
      },
    );
  };

  const reload = () => {
    writeError.reset();
    void rate.refetch();
  };

  if (rate.isPending) return <Skeleton lines={4} />;
  if (rate.isError) return <QueryErrorNotice error={rate.error} onRetry={() => void rate.refetch()} retrying={rate.isFetching} />;

  return <RateEditForm baseline={rate.data} saving={update.isPending} serverError={writeError.fields.amount} notice={writeError.notice ?? writeError.notFound} conflict={writeError.conflict} onReload={reload} reloading={rate.isFetching} onSubmit={save} onCancel={onClose} />;
}

interface RateEditFormProps {
  baseline: DailyRate;
  saving: boolean;
  serverError?: string;
  notice: string | null;
  conflict: boolean;
  onReload: () => void;
  reloading: boolean;
  onSubmit: (body: AdjustRateBody) => void;
  onCancel: () => void;
}

function RateEditForm({ baseline, saving, serverError, notice, conflict, onReload, reloading, onSubmit, onCancel }: RateEditFormProps) {
  const [amount, setAmount] = useState<number | null>(baseline.amount);
  const [error, setError] = useState<string | undefined>();
  const [unchanged, setUnchanged] = useState(false);

  // 새로 읽기로 기준이 바뀌면 내가 안 건드린 값만 새 기준을 따른다
  const [seenBaseline, setSeenBaseline] = useState(baseline);
  if (baseline !== seenBaseline) {
    setSeenBaseline(baseline);
    if (amount === seenBaseline.amount) setAmount(baseline.amount);
  }

  const submit = () => {
    const found = amountError(amount);
    setError(found);
    if (found || amount === null) return;
    if (amount === baseline.amount) {
      setUnchanged(true);
      return;
    }
    onSubmit({ version: baseline.version, amount });
  };

  const shown = error ?? serverError;
  return (
    <form
      id="rate-edit-form"
      noValidate
      onSubmit={(e) => {
        e.preventDefault();
        submit();
      }}
      className="flex flex-col gap-5"
    >
      {conflict ? <VersionConflictNotice onReload={onReload} busy={reloading} /> : null}
      {notice ? <Notice>{notice}</Notice> : null}
      {unchanged ? <Notice>바뀐 내용이 없습니다.</Notice> : null}
      <p className="text-13 text-ink-3">
        지금 요금 <span className="font-semibold text-ink tabular-nums">{formatKrw(baseline.amount)}</span>. 통화는 바꾸지 않습니다.
      </p>
      <AmountField
        value={amount}
        onChange={(v) => {
          setAmount(v);
          setError(undefined);
          setUnchanged(false);
        }}
        error={shown}
        hint="1원부터 10억원"
        disabled={conflict}
      />
      <div className="flex justify-end gap-2 border-t border-line pt-4">
        <Button variant="outline" onClick={onCancel} disabled={saving}>
          취소
        </Button>
        <Button type="submit" loading={saving} disabled={conflict}>
          저장
        </Button>
      </div>
    </form>
  );
}
