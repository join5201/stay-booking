"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { DEV_ACTORS, MOCK_MODES, ROLE_LABELS, firstScreenOf, parseDevActor, parseMockMode, roleOf } from "@/lib/dev-actor";
import { useDevActor } from "./DevActorProvider";

const SELECT = "h-6 rounded-control border border-white/40 bg-devbar-2 px-2 text-12 text-white";

// C1 개발용 바. 36px. 역할이 바뀌면 캐시를 비우고 역할 첫 화면으로, 같은 역할의 ID 교체면 다시 읽기(계약 2절 C1 행)
export function DevActorBar() {
  const { actor, role, mockMode, setActor, setMockMode } = useDevActor();
  const queryClient = useQueryClient();
  const router = useRouter();

  const changeActor = (value: string) => {
    const next = parseDevActor(value);
    if (next === actor) return;
    const nextRole = roleOf(next);
    setActor(next);
    if (nextRole !== role) {
      queryClient.clear();
      router.replace(firstScreenOf(nextRole));
    } else {
      void queryClient.invalidateQueries();
    }
  };

  return (
    <div className="h-(--h-devbar) bg-devbar text-white">
      <div className="mx-auto flex h-full w-(--w-page) items-center gap-3 px-6 text-12">
        <span className="font-semibold">개발용</span>
        <span className="opacity-80">실제 로그인이 아니라 X-Dev-Actor-Id 헤더로 보내는 행위자</span>
        <label className="ml-auto flex items-center gap-1.5">
          행위자
          <select aria-label="행위자" value={actor} onChange={(e) => changeActor(e.target.value)} className={SELECT}>
            {DEV_ACTORS.map((a) => (
              <option key={a} value={a}>
                {a === "public" ? "public (공개)" : `${a} (${ROLE_LABELS[roleOf(a)]})`}
              </option>
            ))}
          </select>
        </label>
        {role === "public" ? null : (
          <label className="flex items-center gap-1.5">
            Mock 결제
            <select aria-label="Mock 결제" value={mockMode} onChange={(e) => setMockMode(parseMockMode(e.target.value))} className={SELECT}>
              {MOCK_MODES.map((m) => (
                <option key={m} value={m}>
                  {m}
                </option>
              ))}
            </select>
          </label>
        )}
      </div>
    </div>
  );
}
