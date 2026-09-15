import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { property } from "@/lib/api/mocks/fixtures";
import { PropertyForm, validateProperty, type PropertySubmit } from "./PropertyForm";

// vitest는 globals가 없어 RTL의 자동 정리가 안 걸린다. 테스트마다 비운다
afterEach(() => cleanup());

function renderForm(overrides: Partial<React.ComponentProps<typeof PropertyForm>> = {}) {
  const onSubmit = vi.fn<(out: PropertySubmit) => void>();
  const onCancel = vi.fn();
  const utils = render(<PropertyForm baseline={null} saving={false} serverErrors={{}} notice={null} conflict={false} onSubmit={onSubmit} onCancel={onCancel} {...overrides} />);
  return { ...utils, onSubmit, onCancel };
}

function type(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label, { exact: false }), { target: { value } });
}

describe("W08 H2 몫. 화면 검사가 서버보다 먼저", () => {
  it("빈 폼을 내면 필수 셋의 문구가 필드 아래에 뜨고 onSubmit이 불리지 않는다", () => {
    const { onSubmit } = renderForm();
    fireEvent.click(screen.getByRole("button", { name: "등록" }));
    expect(screen.getByText("이름을(를) 입력하세요.")).toBeTruthy();
    expect(screen.getByText("지역을 고르세요.")).toBeTruthy();
    expect(screen.getByText("주소을(를) 입력하세요.")).toBeTruthy();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it("글자 수 한도. 이름 100, 주소 300, 설명 2000", () => {
    expect(validateProperty({ name: "a".repeat(101), regionCode: "SEOUL", address: "b".repeat(301), description: "c".repeat(2001) })).toEqual({
      name: "이름은(는) 100자 이하입니다.",
      regionCode: undefined,
      address: "주소은(는) 300자 이하입니다.",
      description: "설명은(는) 2000자 이하입니다.",
    });
    expect(validateProperty({ name: "a".repeat(100), regionCode: "JEJU", address: "b".repeat(300), description: "" })).toEqual({ name: undefined, regionCode: undefined, address: undefined, description: undefined });
  });

  it("고치면 그 필드의 문구가 사라진다", () => {
    renderForm();
    fireEvent.click(screen.getByRole("button", { name: "등록" }));
    type("이름", "한강 뷰");
    expect(screen.queryByText("이름을(를) 입력하세요.")).toBeNull();
    expect(screen.getByText("지역을 고르세요.")).toBeTruthy();
  });
});

describe("등록 본문", () => {
  it("빈 설명은 본문에서 빠지고 앞뒤 공백은 잘린다", () => {
    const { onSubmit } = renderForm();
    type("이름", "  한강 뷰 스테이 ");
    type("지역", "SEOUL");
    type("주소", "서울 마포구 1 ");
    fireEvent.click(screen.getByRole("button", { name: "등록" }));
    expect(onSubmit).toHaveBeenCalledWith({ kind: "create", body: { name: "한강 뷰 스테이", regionCode: "SEOUL", address: "서울 마포구 1" } });
  });

  it("설명이 있으면 같이 간다", () => {
    const { onSubmit } = renderForm();
    type("이름", "이름");
    type("지역", "BUSAN");
    type("주소", "주소");
    type("설명", "강이 보인다");
    fireEvent.click(screen.getByRole("button", { name: "등록" }));
    expect(onSubmit.mock.calls[0][0]).toEqual({ kind: "create", body: { name: "이름", regionCode: "BUSAN", address: "주소", description: "강이 보인다" } });
  });
});

describe("수정 본문. version 숨김 보관, 바뀐 필드만", () => {
  it("이름만 바꾸면 version과 name만", () => {
    const { onSubmit } = renderForm({ baseline: { ...property, version: 4 } });
    expect((screen.getByLabelText("이름", { exact: false }) as HTMLInputElement).value).toBe(property.name);
    type("이름", "바뀐 이름");
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    expect(onSubmit).toHaveBeenCalledWith({ kind: "update", body: { version: 4, name: "바뀐 이름" } });
  });

  it("바뀐 것이 없으면 보내지 않고 문구를 낸다", () => {
    const { onSubmit } = renderForm({ baseline: property });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    expect(screen.getByText("바뀐 내용이 없습니다.")).toBeTruthy();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it("새로 읽기로 기준이 바뀌어도 입력값은 남고 version은 새 값", () => {
    const onSubmit = vi.fn<(out: PropertySubmit) => void>();
    const { rerender } = render(<PropertyForm baseline={property} saving={false} serverErrors={{}} notice={null} conflict={false} onSubmit={onSubmit} onCancel={() => {}} />);
    type("이름", "내가 고친 이름");
    rerender(<PropertyForm baseline={{ ...property, version: 1, address: "남이 고친 주소" }} saving={false} serverErrors={{}} notice={null} conflict={false} onSubmit={onSubmit} onCancel={() => {}} />);
    expect((screen.getByLabelText("이름", { exact: false }) as HTMLInputElement).value).toBe("내가 고친 이름");
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    // 내가 안 건드린 주소는 남이 고친 값을 따라가고 본문에는 안 간다. 남의 수정을 옛 값으로 되돌리지 않는다
    expect((screen.getByLabelText("주소", { exact: false }) as HTMLInputElement).value).toBe("남이 고친 주소");
    expect(onSubmit).toHaveBeenCalledWith({ kind: "update", body: { version: 1, name: "내가 고친 이름" } });
  });
});

describe("서버 오류 자리", () => {
  it("details의 field가 필드 아래에 뜬다", () => {
    renderForm({ baseline: property, serverErrors: { address: "주소는 1자 이상 300자 이하" } });
    expect(screen.getByText("주소는 1자 이상 300자 이하")).toBeTruthy();
  });

  it("VERSION_CONFLICT면 새로 읽기 Notice가 뜨고 저장이 막히며 새로 읽기가 onReload를 부른다", () => {
    const onReload = vi.fn();
    renderForm({ baseline: property, conflict: true, onReload });
    expect(screen.getByText("다른 곳에서 먼저 수정되어 저장하지 못했습니다. 새로 읽은 뒤 다시 저장하세요.")).toBeTruthy();
    expect((screen.getByRole("button", { name: "저장" }) as HTMLButtonElement).disabled).toBe(true);
    fireEvent.click(screen.getByRole("button", { name: "새로 읽기" }));
    expect(onReload).toHaveBeenCalledTimes(1);
  });
});
