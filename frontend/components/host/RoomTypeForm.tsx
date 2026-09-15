"use client";

import { useState } from "react";
import type { RegisterRoomTypeBody, RoomType, UpdateRoomTypeBody } from "@/lib/api/types";
import { hasErrors, integerError, mergeReloaded, textError, type FieldErrors } from "@/lib/forms";
import { Button } from "../Button";
import { Field } from "../Field";
import { Notice } from "../Notice";
import { NumberStepper } from "../NumberStepper";
import { TextArea, TextInput } from "../TextInput";
import { VersionConflictNotice } from "../VersionConflictNotice";

// H3 객실 타입 등록과 수정 폼. 옆 패널 안. 최대 인원 1부터 100(backend RegisterRoomTypeRequest), 이름 100, 설명 2000

export interface RoomTypeValues {
  name: string;
  maxOccupancy: number | null;
  description: string;
}

export type RoomTypeFieldErrors = FieldErrors<keyof RoomTypeValues>;

export function roomTypeValuesOf(roomType: RoomType | null): RoomTypeValues {
  if (!roomType) return { name: "", maxOccupancy: 2, description: "" };
  return { name: roomType.name, maxOccupancy: roomType.maxOccupancy, description: roomType.description ?? "" };
}

export function validateRoomType(v: RoomTypeValues): RoomTypeFieldErrors {
  return {
    name: textError(v.name, { label: "이름", required: true, max: 100 }),
    maxOccupancy: integerError(v.maxOccupancy, { label: "최대 인원", min: 1, max: 100 }),
    description: textError(v.description, { label: "설명", max: 2000 }),
  };
}

export type RoomTypeSubmit = { kind: "create"; body: RegisterRoomTypeBody } | { kind: "update"; body: UpdateRoomTypeBody };

export interface RoomTypeFormProps {
  baseline: RoomType | null;
  saving: boolean;
  serverErrors: Record<string, string>;
  notice: string | null;
  conflict: boolean;
  onReload?: () => void;
  reloading?: boolean;
  onSubmit: (out: RoomTypeSubmit) => void;
  onCancel: () => void;
}

export function RoomTypeForm({ baseline, saving, serverErrors, notice, conflict, onReload, reloading, onSubmit, onCancel }: RoomTypeFormProps) {
  const [values, setValues] = useState<RoomTypeValues>(() => roomTypeValuesOf(baseline));
  const [errors, setErrors] = useState<RoomTypeFieldErrors>({});
  const [unchanged, setUnchanged] = useState(false);

  // 새로 읽기로 기준이 바뀌면 렌더 중에 입력값을 합친다
  const [seenBaseline, setSeenBaseline] = useState(baseline);
  if (baseline !== seenBaseline) {
    setSeenBaseline(baseline);
    setValues((v) => mergeReloaded(v, roomTypeValuesOf(seenBaseline), roomTypeValuesOf(baseline)));
  }

  const set = <K extends keyof RoomTypeValues>(key: K, value: RoomTypeValues[K]) => {
    setValues((v) => ({ ...v, [key]: value }));
    setErrors((e) => ({ ...e, [key]: undefined }));
    setUnchanged(false);
  };

  const errorOf = (key: keyof RoomTypeValues) => errors[key] ?? serverErrors[key];

  const submit = () => {
    const next = validateRoomType(values);
    setErrors(next);
    if (hasErrors(next) || values.maxOccupancy === null) return;
    const name = values.name.trim();
    const description = values.description.trim();
    if (!baseline) {
      const body: RegisterRoomTypeBody = { name, maxOccupancy: values.maxOccupancy };
      if (description) body.description = description;
      onSubmit({ kind: "create", body });
      return;
    }
    const body: UpdateRoomTypeBody = { version: baseline.version };
    if (name !== baseline.name) body.name = name;
    if (values.maxOccupancy !== baseline.maxOccupancy) body.maxOccupancy = values.maxOccupancy;
    if (description !== (baseline.description ?? "")) body.description = description;
    if (Object.keys(body).length === 1) {
      setUnchanged(true);
      return;
    }
    onSubmit({ kind: "update", body });
  };

  return (
    <form
      id="room-type-form"
      noValidate
      onSubmit={(e) => {
        e.preventDefault();
        submit();
      }}
      className="flex flex-col gap-5"
    >
      {conflict && onReload ? <VersionConflictNotice onReload={onReload} busy={reloading} /> : null}
      {notice ? <Notice>{notice}</Notice> : null}
      {unchanged ? <Notice>바뀐 내용이 없습니다.</Notice> : null}

      <Field label="이름" htmlFor="room-type-name" required error={errorOf("name")} hint="100자 이하">
        <TextInput id="room-type-name" value={values.name} onValueChange={(v) => set("name", v)} invalid={!!errorOf("name")} maxLength={100} autoComplete="off" />
      </Field>
      <Field label="최대 인원" htmlFor="room-type-max-occupancy" required error={errorOf("maxOccupancy")} hint="1부터 100">
        <NumberStepper id="room-type-max-occupancy" value={values.maxOccupancy} onChange={(v) => set("maxOccupancy", v)} min={1} max={100} unit="명" invalid={!!errorOf("maxOccupancy")} />
      </Field>
      <Field label="설명" htmlFor="room-type-description" error={errorOf("description")} hint="2,000자 이하">
        <TextArea id="room-type-description" value={values.description} onValueChange={(v) => set("description", v)} invalid={!!errorOf("description")} maxLength={2000} rows={4} />
      </Field>

      <div className="flex justify-end gap-2 border-t border-line pt-4">
        <Button variant="outline" onClick={onCancel} disabled={saving}>
          취소
        </Button>
        <Button type="submit" loading={saving} disabled={conflict}>
          {baseline ? "저장" : "등록"}
        </Button>
      </div>
    </form>
  );
}
