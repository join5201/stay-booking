// 개발용 쿠키 읽기와 쓰기. Path /, SameSite Lax, httpOnly 아님, 30일(계약 6절 개발용 쿠키 행)

const THIRTY_DAYS_SEC = 30 * 24 * 60 * 60;

export function readCookie(name: string, cookieString: string): string | undefined {
  for (const part of cookieString.split(";")) {
    const [key, ...rest] = part.trim().split("=");
    if (key === name) return decodeURIComponent(rest.join("="));
  }
  return undefined;
}

export function serializeCookie(name: string, value: string): string {
  return `${name}=${encodeURIComponent(value)}; Path=/; Max-Age=${THIRTY_DAYS_SEC}; SameSite=Lax`;
}

export function readBrowserCookie(name: string): string | undefined {
  if (typeof document === "undefined") return undefined;
  return readCookie(name, document.cookie);
}

export function writeBrowserCookie(name: string, value: string): void {
  if (typeof document === "undefined") return;
  document.cookie = serializeCookie(name, value);
}
