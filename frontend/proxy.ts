import { NextResponse, type NextRequest } from "next/server";
import { DEV_ACTOR_COOKIE } from "@/lib/dev-actor";
import { redirectTargetFor } from "@/lib/role-route";

// Next 16은 middleware를 proxy로 부른다. 쿠키 역할과 경로 앞부분이 어긋나면 역할 첫 화면으로(W10)
export function proxy(request: NextRequest) {
  const actor = request.cookies.get(DEV_ACTOR_COOKIE)?.value;
  const target = redirectTargetFor(request.nextUrl.pathname, actor);
  if (target === null) return NextResponse.next();
  return NextResponse.redirect(new URL(target, request.url));
}

export const config = {
  matcher: ["/host/:path*", "/operator/:path*", "/bookings/:path*"],
};
