import { ApiError, NetworkError } from "./api/client";

// 오류 표시 세 자리(인계 문서 81행부터 86행, 계약 2-1절 1행과 4행과 5행).
// field는 필드 아래, notice는 화면 안, banner는 화면 위 띠

export type Placement = "field" | "notice" | "banner";

// 다시 시도 규칙(계약 6절 멱등키 행). same-key는 같은 키로 재시도, new-key는 새 키, none은 재시도 버튼 없음
export type RetryPolicy = "same-key" | "new-key" | "none";

interface Rule {
  placement: Placement;
  retry: RetryPolicy;
  message: string;
}

// context.md 8절의 코드 전부. 여기 없는 코드는 banner와 new-key
const RULES: Record<string, Rule> = {
  INVALID_REQUEST: { placement: "field", retry: "none", message: "입력값을 확인하세요." },
  INVALID_DATE_RANGE: { placement: "field", retry: "none", message: "날짜를 확인하세요." },
  INVENTORY_BELOW_COMMITTED: { placement: "field", retry: "none", message: "선점과 판매 합 아래로 줄일 수 없습니다." },
  OCCUPANCY_EXCEEDED: { placement: "field", retry: "none", message: "객실 최대 인원을 넘었습니다. 인원을 줄이세요." },

  ACTOR_REQUIRED: { placement: "notice", retry: "none", message: "행위자가 필요합니다. 위의 개발용 바에서 행위자를 고르세요." },
  ACCESS_DENIED: { placement: "notice", retry: "none", message: "이 역할로는 할 수 없습니다." },
  RESOURCE_NOT_FOUND: { placement: "notice", retry: "none", message: "찾을 수 없습니다." },
  VERSION_CONFLICT: { placement: "notice", retry: "none", message: "다른 곳에서 먼저 수정되어 저장하지 못했습니다. 새로 읽은 뒤 다시 저장하세요." },
  RESOURCE_ALREADY_EXISTS: { placement: "notice", retry: "none", message: "이미 있습니다. 수정으로 이어 갑니다." },
  PRICE_CHANGED: { placement: "notice", retry: "new-key", message: "금액이 바뀌어 예약을 만들지 않았습니다. 새 금액으로 다시 확인하세요." },
  INVENTORY_UNAVAILABLE: { placement: "notice", retry: "none", message: "그사이 객실이 마감됐습니다." },
  INVENTORY_NOT_CONFIGURED: { placement: "notice", retry: "none", message: "판매하지 않는 날짜가 있습니다." },
  RATE_NOT_CONFIGURED: { placement: "notice", retry: "none", message: "요금이 없는 날짜가 있습니다." },
  BOOKING_STATE_CONFLICT: { placement: "notice", retry: "none", message: "지금 상태에서는 할 수 없는 동작입니다. 예약 상태를 다시 읽었습니다." },
  BOOKING_EXPIRED: { placement: "notice", retry: "none", message: "예약이 만료됐습니다. 새 예약으로 진행하세요." },
  CANCELLATION_NOT_ALLOWED: { placement: "notice", retry: "none", message: "취소할 수 있는 날짜가 지났습니다." },
  PAYMENT_ATTEMPTS_EXHAUSTED: { placement: "notice", retry: "none", message: "결제 시도 3회를 다 썼습니다." },
  PAYMENT_IN_PROGRESS: { placement: "notice", retry: "none", message: "앞선 결제가 처리 중입니다. 잠시 뒤 새로 고침하세요." },

  REQUEST_IN_PROGRESS: { placement: "banner", retry: "same-key", message: "같은 요청을 처리 중입니다. 잠시 뒤 다시 시도하세요." },
  IDEMPOTENCY_KEY_REQUIRED: { placement: "banner", retry: "new-key", message: "요청 처리에 문제가 있습니다. 다시 시도하세요." },
  IDEMPOTENCY_KEY_REUSED: { placement: "banner", retry: "new-key", message: "요청 처리에 문제가 있습니다. 다시 시도하세요." },
  TEMPORARY_FAILURE: { placement: "banner", retry: "same-key", message: "일시적인 오류입니다. 다시 시도하세요." },
  INTERNAL_ERROR: { placement: "banner", retry: "same-key", message: "일시적인 오류입니다. 다시 시도하세요." },
};

const NETWORK: Rule = { placement: "banner", retry: "same-key", message: "일시적인 오류입니다. 연결을 확인하고 다시 시도하세요." };
const UNKNOWN: Rule = { placement: "banner", retry: "new-key", message: "알 수 없는 오류입니다." };

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}

export function isNetworkError(error: unknown): error is NetworkError {
  return error instanceof NetworkError;
}

function ruleOf(error: unknown): Rule {
  if (isNetworkError(error)) return NETWORK;
  if (!isApiError(error)) return UNKNOWN;
  const rule = RULES[error.code];
  if (rule) return rule;
  // 코드를 모르는 5xx도 일시 오류로. 그 밖은 알 수 없음
  return error.status >= 500 ? RULES.INTERNAL_ERROR : UNKNOWN;
}

export function placementOf(error: unknown): Placement {
  return ruleOf(error).placement;
}

export function retryPolicyOf(error: unknown): RetryPolicy {
  return ruleOf(error).retry;
}

// 화면 문구. 서버 message를 그대로 내지 않는다. 필드 문구는 fieldErrorsOf가 서버 reason을 쓴다
export function messageOf(error: unknown): string {
  return ruleOf(error).message;
}

// details의 field가 본문 키다. 빈 field는 특정 필드가 아니라서 뺀다
export function fieldErrorsOf(error: unknown): Record<string, string> {
  if (!isApiError(error)) return {};
  const out: Record<string, string> = {};
  for (const d of error.details) {
    if (d.field && !(d.field in out)) out[d.field] = d.reason;
  }
  return out;
}

// 409 REQUEST_IN_PROGRESS만 초가 있다. 5xx와 네트워크는 초 카운트 없이 다시 시도 버튼(계약 2-1절 4행)
export function retryAfterSecOf(error: unknown): number | null {
  return isApiError(error) ? error.retryAfterSec : null;
}
