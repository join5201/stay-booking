"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import type { Role } from "@/lib/dev-actor";
import { useDevActor } from "./DevActorProvider";

interface Tab {
  href: string;
  label: string;
  disabled?: boolean;
}

// 역할마다 탭. public은 내 예약이 비활성(계약 2절 C1 행)
export function tabsOf(role: Role): Tab[] {
  if (role === "host") return [{ href: "/host/properties", label: "내 숙소" }];
  if (role === "operator") return [{ href: "/operator/promotions", label: "프로모션" }];
  return [
    { href: "/", label: "검색" },
    { href: "/bookings", label: "내 예약", disabled: role === "public" },
  ];
}

function isActive(pathname: string, href: string, tabs: Tab[]): boolean {
  if (href === "/") return !tabs.some((t) => t.href !== "/" && pathname.startsWith(t.href));
  return pathname === href || pathname.startsWith(href + "/");
}

export function RoleTabs() {
  const { role } = useDevActor();
  const pathname = usePathname();
  const tabs = tabsOf(role);
  return (
    <nav aria-label="역할 메뉴" className="h-(--h-roletabs) border-b border-line bg-surface">
      <ul className="mx-auto flex h-full w-(--w-page) items-stretch gap-6 px-6">
        {tabs.map((t) => {
          const active = isActive(pathname, t.href, tabs);
          const base = "flex items-center border-b-2 text-15 font-semibold";
          if (t.disabled) {
            return (
              <li key={t.href} className="flex">
                <button type="button" disabled title="행위자를 게스트로 바꾸면 열립니다" className={`${base} cursor-not-allowed border-transparent text-ink-4`}>
                  {t.label}
                </button>
              </li>
            );
          }
          return (
            <li key={t.href} className="flex">
              <Link href={t.href} aria-current={active ? "page" : undefined} className={`${base} ${active ? "border-devbar text-ink" : "border-transparent text-ink-3 hover:text-ink"}`}>
                {t.label}
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
