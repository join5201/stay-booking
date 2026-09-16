// 폼 공통. 화면 검사가 서버보다 먼저(계약 8-1절 W08)이고 수정은 바뀐 필드만 보낸다(인계 문서 111행)

export type FieldErrors<K extends string = string> = Partial<Record<K, string>>;

// 글자 수 검사. required가 아니면 빈 값은 통과
export function textError(value: string, rule: { label: string; required?: boolean; max: number }): string | undefined {
  const v = value.trim();
  if (!v) return rule.required ? `${rule.label}을(를) 입력하세요.` : undefined;
  if (v.length > rule.max) return `${rule.label}은(는) ${rule.max}자 이하입니다.`;
  return undefined;
}

// 정수 범위 검사. 빈 칸은 필수 오류
export function integerError(value: number | null, rule: { label: string; min: number; max: number }): string | undefined {
  if (value === null || Number.isNaN(value)) return `${rule.label}을(를) 입력하세요.`;
  if (!Number.isInteger(value)) return `${rule.label}은(는) 정수입니다.`;
  if (value < rule.min || value > rule.max) return `${rule.label}은(는) ${rule.min}부터 ${rule.max}까지입니다.`;
  return undefined;
}

export function hasErrors(errors: FieldErrors): boolean {
  return Object.values(errors).some((e) => !!e);
}

// 기준값과 다른 필드만. 수정 본문은 version과 바뀐 필드만 담는다
export function changedFields<T extends object>(baseline: T, current: T): Partial<T> {
  const out: Partial<T> = {};
  for (const key of Object.keys(current) as (keyof T)[]) {
    if (current[key] !== baseline[key]) out[key] = current[key];
  }
  return out;
}

// 새로 읽기 뒤 입력값 합치기. 내가 안 건드린 필드(옛 기준과 같은 값)는 새 기준을 따르고 내가 고친 필드는 그대로 둔다.
// 그래야 다시 저장할 때 남이 고친 값을 옛 값으로 되돌리지 않는다
export function mergeReloaded<T extends object>(current: T, oldBaseline: T, newBaseline: T): T {
  const out = { ...current };
  for (const key of Object.keys(current) as (keyof T)[]) {
    if (current[key] === oldBaseline[key]) out[key] = newBaseline[key];
  }
  return out;
}
