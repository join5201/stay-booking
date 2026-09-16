import { readBrowserCookie } from "@/lib/cookies";
import { DEV_ACTOR_COOKIE, parseDevActor } from "@/lib/dev-actor";
import { noteServerNow } from "./server-clock";

// fetch 래퍼(인계 문서 34행부터 39행, 계약 2-1절 1행과 4행).
// 쿠키 dev_actor를 X-Dev-Actor-Id로(public이면 생략), 멱등키는 인자로, Idempotency-Replayed는 성공,
// 오류 본문은 code, message, traceId, details(field, reason), Retry-After는 409 REQUEST_IN_PROGRESS에서만

export const API_BASE = "/api/v1";

export interface ApiErrorDetail {
  field: string;
  reason: string;
}

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly traceId: string | null;
  readonly details: ApiErrorDetail[];
  // 409 REQUEST_IN_PROGRESS의 Retry-After 초. 다른 응답은 null
  readonly retryAfterSec: number | null;

  constructor(args: { status: number; code: string; message: string; traceId?: string | null; details?: ApiErrorDetail[]; retryAfterSec?: number | null }) {
    super(args.message);
    this.name = "ApiError";
    this.status = args.status;
    this.code = args.code;
    this.traceId = args.traceId ?? null;
    this.details = args.details ?? [];
    this.retryAfterSec = args.retryAfterSec ?? null;
  }

  // 필드 오류는 details의 field로 찾는다. 첫 번째 이유를 낸다
  fieldError(field: string): string | undefined {
    return this.details.find((d) => d.field === field)?.reason;
  }
}

// fetch 자체가 실패(연결 불가, 끊김). 응답이 없다
export class NetworkError extends Error {
  constructor(cause: unknown) {
    super("네트워크 오류");
    this.name = "NetworkError";
    this.cause = cause;
  }
}

export type QueryValue = string | number | boolean | null | undefined;

export interface RequestOptions {
  method?: "GET" | "POST" | "PATCH";
  query?: Record<string, QueryValue>;
  body?: unknown;
  // 쓰기 중 멱등 대상(BOOK-01, BOOK-04, PAY-01)만. 화면이 useRef에 든 값을 넘긴다
  idempotencyKey?: string;
  signal?: AbortSignal;
}

export interface ApiResult<T> {
  data: T;
  status: number;
  // Idempotency-Replayed: true. 성공이지만 최초 스냅샷이라 뒤에 다시 읽는다
  replayed: boolean;
  // 본문 serverNow가 우선, 없으면 Date 헤더. 둘 다 없으면 null
  serverNow: string | null;
}

function buildUrl(path: string, query?: Record<string, QueryValue>): string {
  const origin = typeof window !== "undefined" && window.location ? window.location.origin : "http://localhost";
  const url = new URL(`${API_BASE}${path}`, origin);
  if (query) {
    for (const [k, v] of Object.entries(query)) {
      if (v !== undefined && v !== null && v !== "") url.searchParams.set(k, String(v));
    }
  }
  return url.toString();
}

function actorHeader(): Record<string, string> {
  const actor = parseDevActor(readBrowserCookie(DEV_ACTOR_COOKIE));
  return actor === "public" ? {} : { "X-Dev-Actor-Id": actor };
}

async function readJson(response: Response): Promise<unknown> {
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

function serverNowOf(body: unknown, response: Response): string | null {
  if (body && typeof body === "object" && typeof (body as { serverNow?: unknown }).serverNow === "string") {
    return (body as { serverNow: string }).serverNow;
  }
  const date = response.headers.get("date");
  if (!date) return null;
  const ms = Date.parse(date);
  return Number.isNaN(ms) ? null : new Date(ms).toISOString();
}

function toApiError(status: number, body: unknown, response: Response): ApiError {
  const b = (body && typeof body === "object" ? body : {}) as Partial<{ code: string; message: string; traceId: string; details: ApiErrorDetail[] }>;
  const code = typeof b.code === "string" ? b.code : "HTTP_ERROR";
  const retryAfterSec = status === 409 && code === "REQUEST_IN_PROGRESS" ? Number(response.headers.get("retry-after") ?? "1") || 1 : null;
  return new ApiError({
    status,
    code,
    message: typeof b.message === "string" ? b.message : `HTTP ${status}`,
    traceId: typeof b.traceId === "string" ? b.traceId : null,
    details: Array.isArray(b.details) ? b.details.filter((d): d is ApiErrorDetail => !!d && typeof d.field === "string" && typeof d.reason === "string") : [],
    retryAfterSec,
  });
}

export async function api<T>(path: string, options: RequestOptions = {}): Promise<ApiResult<T>> {
  const headers: Record<string, string> = { Accept: "application/json", ...actorHeader() };
  if (options.body !== undefined) headers["Content-Type"] = "application/json";
  if (options.idempotencyKey) headers["Idempotency-Key"] = options.idempotencyKey;

  let response: Response;
  try {
    response = await fetch(buildUrl(path, options.query), {
      method: options.method ?? "GET",
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      signal: options.signal,
      credentials: "same-origin",
    });
  } catch (cause) {
    throw new NetworkError(cause);
  }

  const body = await readJson(response);
  const serverNow = serverNowOf(body, response);
  if (serverNow) noteServerNow(serverNow);

  if (!response.ok) throw toApiError(response.status, body, response);

  return {
    data: body as T,
    status: response.status,
    replayed: response.headers.get("idempotency-replayed") === "true",
    serverNow,
  };
}

// 화면 상태(useRef)에 보관할 새 멱등키. URL과 저장소에 두지 않는다(계약 6절 멱등키 행)
export function newIdempotencyKey(): string {
  return crypto.randomUUID();
}
