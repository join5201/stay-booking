"use client";

import { useState } from "react";
import { useCreateInventory, useInventory, useUpdateInventory } from "@/lib/api/hooks";
import type { AdjustInventoryBody, DailyInventory } from "@/lib/api/types";
import { isApiError } from "@/lib/errors";
import { integerError } from "@/lib/forms";
import { useBanner } from "../Banner";
import { Button } from "../Button";
import { Field } from "../Field";
import { Notice } from "../Notice";
import { NumberStepper } from "../NumberStepper";
import { QueryErrorNotice } from "../QueryErrorNotice";
import { SidePanel } from "../SidePanel";
import { Skeleton } from "../Skeleton";
import { useWriteError } from "../useWriteError";
import { VersionConflictNotice } from "../VersionConflictNotice";

// H4 날짜 칸 패널. 누락이면 INV-01, 있으면 INV-05를 읽은 뒤 INV-03(계약 2절 H4 행)

// 총 재고 0부터 100,000(backend RegisterInventoryRequest와 AdjustInventoryRequest)
export const TOTAL_COUNT_RULE = { label: "총 재고", min: 0, max: 100_000 };

// 최소 재고는 선점 더하기 판매. 화면 검사가 먼저이고 서버의 INVENTORY_BELOW_COMMITTED도 같은 문구에 INV-05의 숫자를 채운다(계약 2-1절 5행)
export function belowCommittedMessage(heldCount: number, soldCount: number): string {
  return `선점 ${heldCount}과 판매 ${soldCount}를 더한 ${heldCount + soldCount} 아래로 줄일 수 없습니다.`;
}

export function minimumTotalError(totalCount: number, committed: Pick<DailyInventory, "heldCount" | "soldCount">): string | undefined {
  return totalCount < committed.heldCount + committed.soldCount ? belowCommittedMessage(committed.heldCount, committed.soldCount) : undefined;
}

export interface InventoryCellPanelProps {
  roomTypeId: string;
  date: string;
  // INV-04의 items에 있으면 수정, 없으면 등록
  exists: boolean;
  // 등록 폼에 미리 채울 직전 날짜 항목
  previous: DailyInventory | null;
  onClose: () => void;
}

export function InventoryCellPanel({ roomTypeId, date, exists, previous, onClose }: InventoryCellPanelProps) {
  const [editing, setEditing] = useState(exists);
  return (
    <SidePanel open title={`${date} 재고 ${editing ? "수정" : "등록"}`} onClose={onClose}>
      {editing ? <EditBody roomTypeId={roomTypeId} date={date} onClose={onClose} /> : <CreateBody roomTypeId={roomTypeId} date={date} previous={previous} onExists={() => setEditing(true)} onClose={onClose} />}
    </SidePanel>
  );
}

function CreateBody({ roomTypeId, date, previous, onExists, onClose }: { roomTypeId: string; date: string; previous: DailyInventory | null; onExists: () => void; onClose: () => void }) {
  const banner = useBanner();
  const create = useCreateInventory(roomTypeId);
  const writeError = useWriteError();
  const [totalCount, setTotalCount] = useState<number | null>(previous?.totalCount ?? null);
  const [error, setError] = useState<string | undefined>();

  const save = (value: number) => {
    writeError.reset();
    create.mutate(
      { date, totalCount: value },
      {
        onSuccess: () => {
          banner.notify("재고를 등록했습니다.");
          onClose();
        },
        onError: (e) => {
          // 그사이 등록된 날짜면 수정 폼으로 자동 전환하고 INV-05를 읽는다(인계 문서 83행)
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
    const found = integerError(totalCount, TOTAL_COUNT_RULE);
    setError(found);
    if (found || totalCount === null) return;
    save(totalCount);
  };

  const shown = error ?? writeError.fields.totalCount;
  return (
    <form
      id="inventory-create-form"
      noValidate
      onSubmit={(e) => {
        e.preventDefault();
        submit();
      }}
      className="flex flex-col gap-5"
    >
      {writeError.notice ?? writeError.notFound ? <Notice>{writeError.notice ?? writeError.notFound}</Notice> : null}
      <Field label="총 재고" htmlFor="inventory-total-count" required error={shown} hint={previous ? `직전 날짜 ${previous.date}의 값 ${previous.totalCount}을(를) 미리 채웠습니다. 0부터 100,000` : "0부터 100,000"}>
        <NumberStepper
          id="inventory-total-count"
          value={totalCount}
          onChange={(v) => {
            setTotalCount(v);
            setError(undefined);
          }}
          min={TOTAL_COUNT_RULE.min}
          max={TOTAL_COUNT_RULE.max}
          unit="개"
          invalid={!!shown}
        />
      </Field>
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
  const inventory = useInventory(roomTypeId, date);
  const update = useUpdateInventory(roomTypeId);
  const writeError = useWriteError();
  // 서버가 INVENTORY_BELOW_COMMITTED를 냈다. 문구의 숫자는 다시 읽은 INV-05 값
  const [belowCommitted, setBelowCommitted] = useState(false);

  const save = (body: AdjustInventoryBody) => {
    writeError.reset();
    setBelowCommitted(false);
    update.mutate(
      { date, ...body },
      {
        onSuccess: () => {
          banner.notify("재고를 저장했습니다.");
          onClose();
        },
        onError: (e) => {
          if (isApiError(e) && e.code === "INVENTORY_BELOW_COMMITTED") {
            setBelowCommitted(true);
            void inventory.refetch();
            return;
          }
          writeError.present(e, () => save(body));
        },
      },
    );
  };

  const reload = () => {
    writeError.reset();
    setBelowCommitted(false);
    void inventory.refetch();
  };

  if (inventory.isPending) return <Skeleton lines={5} />;
  if (inventory.isError) return <QueryErrorNotice error={inventory.error} onRetry={() => void inventory.refetch()} retrying={inventory.isFetching} />;

  const data = inventory.data;
  return (
    <InventoryEditForm
      baseline={data}
      saving={update.isPending}
      serverError={belowCommitted ? belowCommittedMessage(data.heldCount, data.soldCount) : writeError.fields.totalCount}
      notice={writeError.notice ?? writeError.notFound}
      conflict={writeError.conflict}
      onReload={reload}
      reloading={inventory.isFetching}
      onSubmit={save}
      onCancel={onClose}
    />
  );
}

interface InventoryEditFormProps {
  baseline: DailyInventory;
  saving: boolean;
  serverError?: string;
  notice: string | null;
  conflict: boolean;
  onReload: () => void;
  reloading: boolean;
  onSubmit: (body: AdjustInventoryBody) => void;
  onCancel: () => void;
}

function InventoryEditForm({ baseline, saving, serverError, notice, conflict, onReload, reloading, onSubmit, onCancel }: InventoryEditFormProps) {
  const [totalCount, setTotalCount] = useState<number | null>(baseline.totalCount);
  const [error, setError] = useState<string | undefined>();
  const [unchanged, setUnchanged] = useState(false);

  // 새로 읽기로 기준이 바뀌면 내가 안 건드린 값만 새 기준을 따른다
  const [seenBaseline, setSeenBaseline] = useState(baseline);
  if (baseline !== seenBaseline) {
    setSeenBaseline(baseline);
    if (totalCount === seenBaseline.totalCount) setTotalCount(baseline.totalCount);
  }

  const submit = () => {
    const found = integerError(totalCount, TOTAL_COUNT_RULE) ?? (totalCount === null ? undefined : minimumTotalError(totalCount, baseline));
    setError(found);
    if (found || totalCount === null) return;
    if (totalCount === baseline.totalCount) {
      setUnchanged(true);
      return;
    }
    onSubmit({ version: baseline.version, totalCount });
  };

  const shown = error ?? serverError;
  return (
    <form
      id="inventory-edit-form"
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

      <dl className="grid grid-cols-3 gap-3 rounded-card border border-line bg-surface-2 px-4 py-3 text-13">
        <div>
          <dt className="text-ink-3">선점</dt>
          <dd className="text-15 font-semibold tabular-nums">{baseline.heldCount}</dd>
        </div>
        <div>
          <dt className="text-ink-3">판매</dt>
          <dd className="text-15 font-semibold tabular-nums">{baseline.soldCount}</dd>
        </div>
        <div>
          <dt className="text-ink-3">가용</dt>
          <dd className="text-15 font-semibold tabular-nums">{baseline.availableCount}</dd>
        </div>
      </dl>

      <Field label="총 재고" htmlFor="inventory-total-count" required error={shown} hint={`선점 ${baseline.heldCount}과 판매 ${baseline.soldCount}를 더한 ${baseline.heldCount + baseline.soldCount} 이상. 100,000 이하`}>
        <NumberStepper
          id="inventory-total-count"
          value={totalCount}
          onChange={(v) => {
            setTotalCount(v);
            setError(undefined);
            setUnchanged(false);
          }}
          min={TOTAL_COUNT_RULE.min}
          max={TOTAL_COUNT_RULE.max}
          unit="개"
          invalid={!!shown}
          disabled={conflict}
        />
      </Field>

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
