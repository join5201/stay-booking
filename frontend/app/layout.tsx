import type { Metadata } from "next";
import { cookies } from "next/headers";
import "./globals.css";
import { AppShell } from "@/components/AppShell";
import { DEV_ACTOR_COOKIE, DEV_MOCK_MODE_COOKIE, parseDevActor, parseMockMode } from "@/lib/dev-actor";
import { Providers } from "./providers";

export const metadata: Metadata = {
  title: "O2O 숙박 예약",
  description: "O2O 숙박 예약 MVP 프론트. 로컬 개발용",
};

// 서버 컴포넌트는 이 껍데기뿐. 요청 쿠키를 읽어 첫 그림부터 행위자가 맞게 한다
export default async function RootLayout({ children }: LayoutProps<"/">) {
  const store = await cookies();
  const actor = parseDevActor(store.get(DEV_ACTOR_COOKIE)?.value);
  const mockMode = parseMockMode(store.get(DEV_MOCK_MODE_COOKIE)?.value);
  return (
    <html lang="ko">
      <body>
        <Providers>
          <AppShell actor={actor} mockMode={mockMode}>
            {children}
          </AppShell>
        </Providers>
      </body>
    </html>
  );
}
