// 개발용 행위자와 Mock 모드. 쿠키 이름과 값은 인계 문서 9행, ID는 backend ActorRegistry와 같다.
// 서버(proxy.ts, layout)와 브라우저가 같이 쓰므로 순수 함수만 둔다

export const DEV_ACTOR_COOKIE = "dev_actor";
export const DEV_MOCK_MODE_COOKIE = "dev_mock_mode";

export const DEV_ACTORS = ["public", "guest_001", "guest_002", "host_001", "host_002", "operator_001"] as const;
export type DevActor = (typeof DEV_ACTORS)[number];

export const MOCK_MODES = ["APPROVE", "DECLINE", "DEFER"] as const;
export type MockMode = (typeof MOCK_MODES)[number];

export type Role = "public" | "guest" | "host" | "operator";

export function parseDevActor(value: string | null | undefined): DevActor {
  return (DEV_ACTORS as readonly string[]).includes(value ?? "") ? (value as DevActor) : "public";
}

export function parseMockMode(value: string | null | undefined): MockMode {
  return (MOCK_MODES as readonly string[]).includes(value ?? "") ? (value as MockMode) : "APPROVE";
}

// 모르는 값은 public. 백엔드가 401을 내지만 화면은 미리 공개 역할로 본다
export function roleOf(actor: string | null | undefined): Role {
  const a = parseDevActor(actor);
  if (a.startsWith("guest_")) return "guest";
  if (a.startsWith("host_")) return "host";
  if (a.startsWith("operator_")) return "operator";
  return "public";
}

export const ROLE_LABELS: Record<Role, string> = {
  public: "공개", guest: "게스트", host: "호스트", operator: "운영자",
};

// 역할의 첫 화면. G1, H1, O1
export function firstScreenOf(role: Role): string {
  if (role === "host") return "/host/properties";
  if (role === "operator") return "/operator/promotions";
  return "/";
}
