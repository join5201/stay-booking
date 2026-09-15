// 등록 지역 코드 열일곱. backend/src/main/java/com/o2o/shared/RegionRegistry.java의 FIXTURE와 같다(계약 7절 D-6).
// W07 테스트가 그 파일을 읽어 대조한다. 바꿀 때는 백엔드와 같이 바꾼다
export const REGION_CODES = [
  "SEOUL", "BUSAN", "DAEGU", "INCHEON", "GWANGJU", "DAEJEON", "ULSAN", "SEJONG",
  "GYEONGGI", "GANGWON", "CHUNGBUK", "CHUNGNAM", "JEONBUK", "JEONNAM",
  "GYEONGBUK", "GYEONGNAM", "JEJU",
] as const;

export type RegionCode = (typeof REGION_CODES)[number];

export const REGION_LABELS: Record<RegionCode, string> = {
  SEOUL: "서울", BUSAN: "부산", DAEGU: "대구", INCHEON: "인천", GWANGJU: "광주",
  DAEJEON: "대전", ULSAN: "울산", SEJONG: "세종", GYEONGGI: "경기", GANGWON: "강원",
  CHUNGBUK: "충북", CHUNGNAM: "충남", JEONBUK: "전북", JEONNAM: "전남",
  GYEONGBUK: "경북", GYEONGNAM: "경남", JEJU: "제주",
};

export function isRegionCode(value: string | null | undefined): value is RegionCode {
  return (REGION_CODES as readonly string[]).includes(value ?? "");
}

export function regionLabel(code: string): string {
  return isRegionCode(code) ? `${REGION_LABELS[code]} (${code})` : code;
}
