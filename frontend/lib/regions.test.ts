import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";
import { REGION_CODES, REGION_LABELS, isRegionCode } from "./regions";

// W07. 지역 코드 상자의 열일곱이 RegionRegistry.java의 값과 같다. 테스트가 백엔드 파일을 읽어 대조(계약 7절 D-6)
const REGISTRY = resolve(__dirname, "../../backend/src/main/java/com/o2o/shared/RegionRegistry.java");

function fixtureCodesFromJava(source: string): string[] {
  const m = /FIXTURE\s*=\s*List\.of\(([\s\S]*?)\);/.exec(source);
  if (!m) throw new Error("RegionRegistry.java에서 FIXTURE 목록을 찾지 못했다");
  return Array.from(m[1].matchAll(/"([A-Z]+)"/g), (x) => x[1]);
}

describe("REGION_CODES (W07)", () => {
  it("백엔드 RegionRegistry의 FIXTURE와 순서까지 같다", () => {
    const backend = fixtureCodesFromJava(readFileSync(REGISTRY, "utf8"));
    expect(backend).toHaveLength(17);
    expect([...REGION_CODES]).toEqual(backend);
  });

  it("열일곱 전부에 한국어 표시가 있다", () => {
    for (const code of REGION_CODES) expect(REGION_LABELS[code]).toBeTruthy();
  });

  it("목록 밖은 지역 코드가 아니다", () => {
    expect(isRegionCode("SEOUL")).toBe(true);
    expect(isRegionCode("seoul")).toBe(false);
    expect(isRegionCode("")).toBe(false);
    expect(isRegionCode(undefined)).toBe(false);
  });
});
