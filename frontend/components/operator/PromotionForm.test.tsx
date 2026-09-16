import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { promotion } from "@/lib/api/mocks/fixtures";
import type { Promotion } from "@/lib/api/types";
import { PromotionForm, mergePromotionValues, promotionBodyOf, promotionValuesOf, validatePromotion, type PromotionSubmit } from "./PromotionForm";

afterEach(() => cleanup());

function renderForm(baseline: Promotion | null, extra: Partial<Parameters<typeof PromotionForm>[0]> = {}) {
  const onSubmit = vi.fn<(out: PromotionSubmit) => void>();
  const onReload = vi.fn();
  const view = render(<PromotionForm baseline={baseline} saving={false} serverErrors={{}} notice={null} conflict={false} onSubmit={onSubmit} onCancel={() => {}} onReload={onReload} {...extra} />);
  return { onSubmit, onReload, view };
}

const input = (label: string) => screen.getByLabelText(label, { exact: false }) as HTMLInputElement;
const submitButton = () => screen.getByRole("button", { name: /^(등록|저장)$/ });

const valid = { name: "가을 10%", discountRate: 10, campaignStartDate: "2026-10-01", campaignEndDate: "2026-11-01", stayStartDate: "", stayEndDate: "", minNights: 1, regionCodes: [] };

describe("W08의 O2 몫. 화면 검사가 서버보다 먼저", () => {
  it("빈 폼 제출은 필수 넷의 문구이고 요청이 없다", () => {
    const { onSubmit } = renderForm(null);
    fireEvent.click(submitButton());
    expect(screen.getByText("이름을(를) 입력하세요.")).toBeTruthy();
    expect(screen.getByText("캠페인 시작을(를) 입력하세요.")).toBeTruthy();
    expect(screen.getByText("캠페인 끝을(를) 입력하세요.")).toBeTruthy();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it("할인율 1부터 99, 최소 박수 1부터 30", () => {
    expect(validatePromotion({ ...valid, discountRate: 0 }).discountRate).toBe("할인율은(는) 1부터 99까지입니다.");
    expect(validatePromotion({ ...valid, discountRate: 100 }).discountRate).toBe("할인율은(는) 1부터 99까지입니다.");
    expect(validatePromotion({ ...valid, discountRate: 99 }).discountRate).toBeUndefined();
    expect(validatePromotion({ ...valid, minNights: 0 }).minNights).toBe("최소 박수은(는) 1부터 30까지입니다.");
    expect(validatePromotion({ ...valid, minNights: 31 }).minNights).toBe("최소 박수은(는) 1부터 30까지입니다.");
    expect(validatePromotion({ ...valid, minNights: 30 }).minNights).toBeUndefined();
  });

  it("캠페인 끝은 시작보다 뒤. 같은 날은 안 된다(끝 날짜 제외)", () => {
    expect(validatePromotion({ ...valid, campaignEndDate: "2026-10-01" }).campaignEndDate).toBe("캠페인 끝은(는) 캠페인 시작보다 뒤여야 합니다. 끝 날짜는 제외됩니다.");
    expect(validatePromotion({ ...valid, campaignEndDate: "2026-09-30" }).campaignEndDate).toBe("캠페인 끝은(는) 캠페인 시작보다 뒤여야 합니다. 끝 날짜는 제외됩니다.");
    expect(validatePromotion({ ...valid, campaignEndDate: "2026-10-02" }).campaignEndDate).toBeUndefined();
  });

  it("숙박 기간은 둘 다 또는 둘 다 아님. 둘 다면 순서", () => {
    expect(validatePromotion({ ...valid, stayStartDate: "2026-10-01" }).stayStartDate).toBe("숙박 기간은 시작과 끝을 둘 다 넣거나 둘 다 비웁니다.");
    expect(validatePromotion({ ...valid, stayEndDate: "2026-10-05" }).stayStartDate).toBe("숙박 기간은 시작과 끝을 둘 다 넣거나 둘 다 비웁니다.");
    expect(validatePromotion({ ...valid, stayStartDate: "2026-10-05", stayEndDate: "2026-10-01" }).stayEndDate).toBe("숙박 끝은(는) 숙박 시작보다 뒤여야 합니다. 끝 날짜는 제외됩니다.");
    expect(validatePromotion({ ...valid, stayStartDate: "2026-10-01", stayEndDate: "2026-10-05" }).stayStartDate).toBeUndefined();
    expect(validatePromotion(valid).stayStartDate).toBeUndefined();
  });

  it("화면에서 숙박 시작만 넣고 제출하면 문구가 뜨고 요청이 없다", () => {
    const { onSubmit } = renderForm(null);
    fireEvent.change(input("이름"), { target: { value: "가을 10%" } });
    fireEvent.change(input("캠페인 시작"), { target: { value: "2026-10-01" } });
    fireEvent.change(input("캠페인 끝"), { target: { value: "2026-11-01" } });
    fireEvent.change(input("숙박 시작"), { target: { value: "2026-10-01" } });
    fireEvent.click(submitButton());
    expect(screen.getByText("숙박 기간은 시작과 끝을 둘 다 넣거나 둘 다 비웁니다.")).toBeTruthy();
    expect(onSubmit).not.toHaveBeenCalled();
  });
});

describe("본문", () => {
  it("등록 본문. 숙박 기간이 없으면 두 필드를 생략하고 enabled도 생략(기본 true). 지역은 빈 배열", () => {
    const { onSubmit } = renderForm(null);
    fireEvent.change(input("이름"), { target: { value: "  가을 10%  " } });
    fireEvent.change(input("캠페인 시작"), { target: { value: "2026-10-01" } });
    fireEvent.change(input("캠페인 끝"), { target: { value: "2026-11-01" } });
    fireEvent.click(submitButton());
    expect(onSubmit).toHaveBeenCalledWith({ kind: "create", body: { name: "가을 10%", discountRate: 10, campaignStartDate: "2026-10-01", campaignEndDate: "2026-11-01", minNights: 1, regionCodes: [] } });
  });

  it("등록 본문에 숙박 기간 둘과 지역 코드. 지역은 선택 상자로 더하고 칩으로 뺀다", () => {
    const { onSubmit } = renderForm(null);
    fireEvent.change(input("이름"), { target: { value: "부산 제주" } });
    fireEvent.change(input("캠페인 시작"), { target: { value: "2026-10-01" } });
    fireEvent.change(input("캠페인 끝"), { target: { value: "2026-11-01" } });
    fireEvent.change(input("숙박 시작"), { target: { value: "2026-10-01" } });
    fireEvent.change(input("숙박 끝"), { target: { value: "2026-12-01" } });
    fireEvent.change(screen.getByLabelText("지역", { exact: false }), { target: { value: "BUSAN" } });
    fireEvent.change(screen.getByLabelText("지역", { exact: false }), { target: { value: "JEJU" } });
    fireEvent.change(screen.getByLabelText("지역", { exact: false }), { target: { value: "SEOUL" } });
    fireEvent.click(screen.getByRole("button", { name: "서울 (SEOUL) 빼기" }));
    fireEvent.click(submitButton());
    expect(onSubmit.mock.calls[0][0]).toMatchObject({ kind: "create", body: { stayStartDate: "2026-10-01", stayEndDate: "2026-12-01", regionCodes: ["BUSAN", "JEJU"] } });
  });

  it("수정 본문은 version과 바뀐 필드만. 안 바뀌면 안내", () => {
    const base = { ...promotion, version: 3 };
    const { onSubmit } = renderForm(base);
    fireEvent.click(submitButton());
    expect(screen.getByText("바뀐 내용이 없습니다.")).toBeTruthy();
    expect(onSubmit).not.toHaveBeenCalled();

    fireEvent.change(input("할인율"), { target: { value: "15" } });
    fireEvent.click(submitButton());
    expect(onSubmit).toHaveBeenCalledWith({ kind: "update", body: { version: 3, discountRate: 15 } });
  });

  it("숙박 기간을 바꾸거나 해제하면 두 날짜를 함께 보낸다. 해제는 둘 다 null", () => {
    const withStay: Promotion = { ...promotion, stayStartDate: "2026-10-01", stayEndDate: "2026-12-01", version: 1 };
    expect(promotionBodyOf({ ...promotionValuesOf(withStay), stayEndDate: "2026-12-15" }, withStay)).toEqual({ kind: "update", body: { version: 1, stayStartDate: "2026-10-01", stayEndDate: "2026-12-15" } });
    expect(promotionBodyOf({ ...promotionValuesOf(withStay), stayStartDate: "", stayEndDate: "" }, withStay)).toEqual({ kind: "update", body: { version: 1, stayStartDate: null, stayEndDate: null } });
    expect(promotionBodyOf({ ...promotionValuesOf(promotion), stayStartDate: "2026-10-01", stayEndDate: "2026-12-01" }, promotion)).toEqual({ kind: "update", body: { version: 0, stayStartDate: "2026-10-01", stayEndDate: "2026-12-01" } });
  });

  it("지역 코드는 순서가 달라도 같은 값이면 안 보낸다", () => {
    const base: Promotion = { ...promotion, regionCodes: ["SEOUL", "BUSAN"] };
    expect(promotionBodyOf({ ...promotionValuesOf(base), regionCodes: ["BUSAN", "SEOUL"] }, base)).toBeNull();
    expect(promotionBodyOf({ ...promotionValuesOf(base), regionCodes: ["BUSAN"] }, base)).toEqual({ kind: "update", body: { version: 0, regionCodes: ["BUSAN"] } });
  });

  it("새로 읽기 합치기. 내가 고친 필드는 남고 안 건드린 필드는 새 기준(지역 코드는 값으로 비교)", () => {
    const old = promotionValuesOf(promotion);
    const fresh = promotionValuesOf({ ...promotion, name: "남이 고친 이름", regionCodes: ["JEJU"], version: 1 });
    const mine = { ...old, discountRate: 20, regionCodes: [] as string[] };
    expect(mergePromotionValues(mine, old, fresh)).toEqual({ ...fresh, discountRate: 20 });
  });

  it("VERSION_CONFLICT면 Notice와 새로 읽기, 저장 비활성. 서버 field 문구는 필드 아래", () => {
    const { onReload } = renderForm(promotion, { conflict: true, serverErrors: { discountRate: "할인율은 1 이상" } });
    expect((submitButton() as HTMLButtonElement).disabled).toBe(true);
    expect(screen.getByText("할인율은 1 이상")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "새로 읽기" }));
    expect(onReload).toHaveBeenCalledTimes(1);
  });
});
