"use client";

import { REGION_CODES, REGION_LABELS, isRegionCode, type RegionCode } from "@/lib/regions";
import { controlClass } from "./Field";

export interface RegionCodeInputProps {
  id?: string;
  // 빈 문자열은 미선택
  value: RegionCode | "";
  onChange: (value: RegionCode | "") => void;
  placeholder?: string;
  invalid?: boolean;
  disabled?: boolean;
  // 이미 고른 코드는 목록에서 뺀다. O2의 regionCodes 추가용
  exclude?: readonly string[];
}

// 열일곱 고정 선택 상자. 자유 입력과 대문자 변환과 최근값 저장은 없다(계약 2-1절 6행). G1, H2, O2가 같은 상자
export function RegionCodeInput({ id, value, onChange, placeholder = "지역 선택", invalid, disabled, exclude = [] }: RegionCodeInputProps) {
  const options = REGION_CODES.filter((c) => !exclude.includes(c));
  return (
    <select
      id={id}
      value={value}
      disabled={disabled}
      aria-invalid={invalid || undefined}
      onChange={(e) => onChange(isRegionCode(e.target.value) ? e.target.value : "")}
      className={controlClass(invalid)}
    >
      <option value="">{placeholder}</option>
      {options.map((code) => (
        <option key={code} value={code}>
          {REGION_LABELS[code]} ({code})
        </option>
      ))}
    </select>
  );
}
