"use client";

import type { ReactNode } from "react";
import type { DevActor, MockMode } from "@/lib/dev-actor";
import { BannerProvider } from "./Banner";
import { DevActorBar } from "./DevActorBar";
import { DevActorProvider } from "./DevActorProvider";
import { RoleTabs } from "./RoleTabs";

export interface AppShellProps {
  actor: DevActor;
  mockMode: MockMode;
  children: ReactNode;
}

// layout 몸통. 개발용 바 36, 역할 탭 48, 그 아래 화면 위 띠와 1200 폭 본문(좌우 24)
export function AppShell({ actor, mockMode, children }: AppShellProps) {
  return (
    <DevActorProvider initialActor={actor} initialMockMode={mockMode}>
      <DevActorBar />
      <RoleTabs />
      <BannerProvider>
        <main className="mx-auto w-(--w-page) px-6 py-6">{children}</main>
      </BannerProvider>
    </DevActorProvider>
  );
}
