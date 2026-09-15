"use client";

import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from "react";
import { writeBrowserCookie } from "@/lib/cookies";
import { DEV_ACTOR_COOKIE, DEV_MOCK_MODE_COOKIE, roleOf, type DevActor, type MockMode, type Role } from "@/lib/dev-actor";

export interface DevActorState {
  actor: DevActor;
  role: Role;
  mockMode: MockMode;
  // 쿠키를 쓰고 상태를 바꾼다. 역할 첫 화면 이동과 캐시 비우기는 DevActorBar가 한다
  setActor: (actor: DevActor) => void;
  setMockMode: (mode: MockMode) => void;
}

const DevActorContext = createContext<DevActorState | null>(null);

// 첫 값은 layout이 요청 쿠키에서 읽어 준다. 그래서 첫 그림부터 쿠키와 같다
export function DevActorProvider({ initialActor, initialMockMode, children }: { initialActor: DevActor; initialMockMode: MockMode; children: ReactNode }) {
  const [actor, setActorState] = useState<DevActor>(initialActor);
  const [mockMode, setMockModeState] = useState<MockMode>(initialMockMode);

  const setActor = useCallback((next: DevActor) => {
    writeBrowserCookie(DEV_ACTOR_COOKIE, next);
    setActorState(next);
  }, []);

  const setMockMode = useCallback((next: MockMode) => {
    writeBrowserCookie(DEV_MOCK_MODE_COOKIE, next);
    setMockModeState(next);
  }, []);

  const value = useMemo<DevActorState>(
    () => ({ actor, role: roleOf(actor), mockMode, setActor, setMockMode }),
    [actor, mockMode, setActor, setMockMode],
  );
  return <DevActorContext.Provider value={value}>{children}</DevActorContext.Provider>;
}

export function useDevActor(): DevActorState {
  const state = useContext(DevActorContext);
  if (!state) throw new Error("useDevActor는 DevActorProvider 안에서만 쓴다");
  return state;
}
