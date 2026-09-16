import { fieldErrorsOf, isApiError, messageOf, placementOf, retryAfterSecOf } from "./errors";

// 쓰기 실패를 화면이 그릴 모양으로. placement 셋에 VERSION_CONFLICT와 RESOURCE_NOT_FOUND를 따로 낸다.
// conflict는 새로 읽기 버튼이 있는 Notice, notFound는 화면 전면 Notice(계약 2절 H2 행)

export type ErrorView =
  | { kind: "field"; fields: Record<string, string>; message: string }
  | { kind: "conflict" }
  | { kind: "notFound"; message: string }
  | { kind: "notice"; message: string }
  | { kind: "banner"; message: string; retryAfterSec: number | null };

export function errorViewOf(error: unknown): ErrorView {
  if (isApiError(error)) {
    if (error.code === "VERSION_CONFLICT") return { kind: "conflict" };
    if (error.code === "RESOURCE_NOT_FOUND") return { kind: "notFound", message: messageOf(error) };
  }
  const placement = placementOf(error);
  if (placement === "field") return { kind: "field", fields: fieldErrorsOf(error), message: messageOf(error) };
  if (placement === "notice") return { kind: "notice", message: messageOf(error) };
  return { kind: "banner", message: messageOf(error), retryAfterSec: retryAfterSecOf(error) };
}
