"use client";

import { useState } from "react";
import type { Property, RegisterPropertyBody, UpdatePropertyBody } from "@/lib/api/types";
import { changedFields, hasErrors, mergeReloaded, textError, type FieldErrors } from "@/lib/forms";
import { isRegionCode, type RegionCode } from "@/lib/regions";
import { Button } from "../Button";
import { Field } from "../Field";
import { Notice } from "../Notice";
import { RegionCodeInput } from "../RegionCodeInput";
import { TextArea, TextInput } from "../TextInput";
import { VersionConflictNotice } from "../VersionConflictNotice";

// H2 숙소 등록과 수정 폼. 화면 검사가 서버보다 먼저(W08). 수정은 version을 숨겨 두고 바뀐 필드만(인계 문서 111행).
// 글자 수는 backend RegisterPropertyRequest와 같다. 이름 100, 주소 300, 설명 2000. 지역 코드는 열일곱 고정 선택(계약 2-1절 6행)

export interface PropertyValues {
  name: string;
  regionCode: RegionCode | "";
  address: string;
  description: string;
}

export type PropertyFieldErrors = FieldErrors<keyof PropertyValues>;

export function propertyValuesOf(property: Property | null): PropertyValues {
  if (!property) return { name: "", regionCode: "", address: "", description: "" };
  return {
    name: property.name,
    regionCode: isRegionCode(property.regionCode) ? property.regionCode : "",
    address: property.address,
    description: property.description ?? "",
  };
}

export function validateProperty(v: PropertyValues): PropertyFieldErrors {
  return {
    name: textError(v.name, { label: "이름", required: true, max: 100 }),
    regionCode: v.regionCode ? undefined : "지역을 고르세요.",
    address: textError(v.address, { label: "주소", required: true, max: 300 }),
    description: textError(v.description, { label: "설명", max: 2000 }),
  };
}

export type PropertySubmit = { kind: "create"; body: RegisterPropertyBody } | { kind: "update"; body: UpdatePropertyBody };

export interface PropertyFormProps {
  // null이면 등록. 있으면 수정이고 version은 여기서만 읽는다. 새로 읽기 뒤 바뀌면 입력값은 두고 기준만 바뀐다
  baseline: Property | null;
  saving: boolean;
  serverErrors: Record<string, string>;
  notice: string | null;
  conflict: boolean;
  onReload?: () => void;
  reloading?: boolean;
  onSubmit: (out: PropertySubmit) => void;
  onCancel: () => void;
}

export function PropertyForm({ baseline, saving, serverErrors, notice, conflict, onReload, reloading, onSubmit, onCancel }: PropertyFormProps) {
  const [values, setValues] = useState<PropertyValues>(() => propertyValuesOf(baseline));
  const [errors, setErrors] = useState<PropertyFieldErrors>({});
  const [unchanged, setUnchanged] = useState(false);

  // 새로 읽기로 기준이 바뀌면 렌더 중에 입력값을 합친다(앞 렌더의 값을 기억하는 방식)
  const [seenBaseline, setSeenBaseline] = useState(baseline);
  if (baseline !== seenBaseline) {
    setSeenBaseline(baseline);
    setValues((v) => mergeReloaded(v, propertyValuesOf(seenBaseline), propertyValuesOf(baseline)));
  }

  const set = <K extends keyof PropertyValues>(key: K, value: PropertyValues[K]) => {
    setValues((v) => ({ ...v, [key]: value }));
    setErrors((e) => ({ ...e, [key]: undefined }));
    setUnchanged(false);
  };

  const errorOf = (key: keyof PropertyValues) => errors[key] ?? serverErrors[key];

  const submit = () => {
    const next = validateProperty(values);
    setErrors(next);
    if (hasErrors(next)) return;
    const trimmed: PropertyValues = { ...values, name: values.name.trim(), address: values.address.trim(), description: values.description.trim() };
    if (!baseline) {
      const body: RegisterPropertyBody = { name: trimmed.name, regionCode: trimmed.regionCode, address: trimmed.address };
      if (trimmed.description) body.description = trimmed.description;
      onSubmit({ kind: "create", body });
      return;
    }
    const diff = changedFields(propertyValuesOf(baseline), trimmed);
    if (Object.keys(diff).length === 0) {
      setUnchanged(true);
      return;
    }
    onSubmit({ kind: "update", body: { version: baseline.version, ...diff } });
  };

  return (
    <form
      noValidate
      onSubmit={(e) => {
        e.preventDefault();
        submit();
      }}
      className="flex w-(--w-form) flex-col gap-5"
    >
      {conflict && onReload ? <VersionConflictNotice onReload={onReload} busy={reloading} /> : null}
      {notice ? <Notice>{notice}</Notice> : null}
      {unchanged ? <Notice>바뀐 내용이 없습니다.</Notice> : null}

      <Field label="이름" htmlFor="property-name" required error={errorOf("name")} hint="100자 이하">
        <TextInput id="property-name" value={values.name} onValueChange={(v) => set("name", v)} invalid={!!errorOf("name")} maxLength={100} autoComplete="off" />
      </Field>
      <Field label="지역" htmlFor="property-region" required error={errorOf("regionCode")}>
        <RegionCodeInput id="property-region" value={values.regionCode} onChange={(v) => set("regionCode", v)} invalid={!!errorOf("regionCode")} />
      </Field>
      <Field label="주소" htmlFor="property-address" required error={errorOf("address")} hint="300자 이하">
        <TextInput id="property-address" value={values.address} onValueChange={(v) => set("address", v)} invalid={!!errorOf("address")} maxLength={300} autoComplete="off" />
      </Field>
      <Field label="설명" htmlFor="property-description" error={errorOf("description")} hint="2,000자 이하">
        <TextArea id="property-description" value={values.description} onValueChange={(v) => set("description", v)} invalid={!!errorOf("description")} maxLength={2000} rows={5} />
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
