import { firstScreenOf, roleOf, type Role } from "./dev-actor";

// 경로 앞부분과 그 경로를 쓸 수 있는 역할. 인계 문서 32행. 나머지 경로는 누구나
const GATES: ReadonlyArray<readonly [prefix: string, role: Role]> = [
  ["/host", "host"],
  ["/operator", "operator"],
  ["/bookings", "guest"],
];

function underPrefix(pathname: string, prefix: string): boolean {
  return pathname === prefix || pathname.startsWith(prefix + "/");
}

export function requiredRoleOf(pathname: string): Role | null {
  const gate = GATES.find(([prefix]) => underPrefix(pathname, prefix));
  return gate ? gate[1] : null;
}

// 쿠키 역할과 경로가 어긋나면 그 역할의 첫 화면. 맞으면 null
export function redirectTargetFor(pathname: string, actor: string | null | undefined): string | null {
  const required = requiredRoleOf(pathname);
  if (required === null) return null;
  const role = roleOf(actor);
  return role === required ? null : firstScreenOf(role);
}
