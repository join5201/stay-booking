import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { HttpResponse, http } from "msw";
import { afterEach, describe, expect, it, vi } from "vitest";
import { errorBody, promotion } from "@/lib/api/mocks/fixtures";
import { requestLog } from "@/lib/api/mocks/handlers";
import { server } from "@/lib/api/mocks/server";
import { queryWrapper, setActorCookie, setupMockServer } from "@/lib/api/mocks/test-utils";
import { BannerProvider } from "../Banner";
import { PromotionEditor } from "./PromotionEditor";

setupMockServer();
afterEach(() => cleanup());

function renderEditor() {
  setActorCookie("operator_001");
  const onDone = vi.fn();
  const { Wrapper } = queryWrapper();
  render(
    <Wrapper>
      <BannerProvider>
        <PromotionEditor promotionId="promo_001" onDone={onDone} />
      </BannerProvider>
    </Wrapper>,
  );
  return { onDone };
}

const writes = () => requestLog.filter((r) => r.method !== "GET");
const nameInput = () => screen.getByLabelText("이름", { exact: false }) as HTMLInputElement;

describe("O2 수정", () => {
  it("진입 시 PROMO-03을 읽고 저장은 PROMO-02에 version과 바뀐 필드만", async () => {
    const { onDone } = renderEditor();
    await waitFor(() => expect(nameInput().value).toBe(promotion.name));
    expect(requestLog[0].path).toBe("/api/v1/promotions/promo_001");
    expect(screen.getByText("사용 중")).toBeTruthy();

    fireEvent.change(nameInput(), { target: { value: "겨울 10%" } });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    await waitFor(() => expect(onDone).toHaveBeenCalledTimes(1));
    expect(writes()[0].method).toBe("PATCH");
    expect(writes()[0].path).toBe("/api/v1/promotions/promo_001");
    expect(writes()[0].body).toEqual({ version: promotion.version, name: "겨울 10%" });
    expect(screen.getByText("프로모션을 저장했습니다.")).toBeTruthy();
  });

  it("머리의 사용 끄기는 확인 시트 뒤 enabled와 version만 보내고, 폼의 내 입력값은 남는다", async () => {
    renderEditor();
    await waitFor(() => expect(nameInput().value).toBe(promotion.name));
    fireEvent.change(nameInput(), { target: { value: "내가 고친 이름" } });

    fireEvent.click(screen.getByRole("button", { name: "사용 끄기" }));
    const dialog = await screen.findByRole("dialog");
    expect(dialog.textContent).toContain("프로모션 사용 끄기");
    expect(writes()).toHaveLength(0);
    fireEvent.click(within(dialog).getByRole("button", { name: "사용 끄기" }));

    await screen.findByText("프로모션을 껐습니다.");
    expect(writes()).toHaveLength(1);
    expect(writes()[0].body).toEqual({ version: promotion.version, enabled: false });
    // 응답이 캐시에 들어가 머리가 꺼짐으로 바뀌고 버튼은 사용 켜기
    await waitFor(() => expect(screen.getByText("꺼짐")).toBeTruthy());
    expect(screen.getByRole("button", { name: "사용 켜기" })).toBeTruthy();
    expect(nameInput().value).toBe("내가 고친 이름");

    // 그 뒤 저장은 오른 version으로
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    await waitFor(() => expect(writes()).toHaveLength(2));
    expect(writes()[1].body).toEqual({ version: promotion.version + 1, name: "내가 고친 이름" });
  });

  it("VERSION_CONFLICT면 Notice와 새로 읽기 뒤 오른 version으로 저장한다", async () => {
    let reads = 0;
    const patchBodies: unknown[] = [];
    server.use(
      http.get("*/api/v1/promotions/:promotionId", () => {
        reads += 1;
        return HttpResponse.json(reads === 1 ? promotion : { ...promotion, version: 1, minNights: 2 });
      }),
      http.patch("*/api/v1/promotions/:promotionId", async ({ request }) => {
        const body = (await request.json()) as { version: number };
        patchBodies.push(body);
        if (body.version === 0) return HttpResponse.json(errorBody("VERSION_CONFLICT", "conflict"), { status: 409 });
        return HttpResponse.json({ ...promotion, ...body, version: body.version + 1 });
      }),
    );
    const { onDone } = renderEditor();
    await waitFor(() => expect(nameInput().value).toBe(promotion.name));
    fireEvent.change(nameInput(), { target: { value: "내가 고친 이름" } });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    await screen.findByText("다른 곳에서 먼저 수정되어 저장하지 못했습니다. 새로 읽은 뒤 다시 저장하세요.");
    expect(onDone).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole("button", { name: "새로 읽기" }));
    await waitFor(() => expect((screen.getByLabelText("최소 박수", { exact: false }) as HTMLInputElement).value).toBe("2"));
    expect(nameInput().value).toBe("내가 고친 이름");
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    await waitFor(() => expect(onDone).toHaveBeenCalledTimes(1));
    expect(patchBodies).toEqual([
      { version: 0, name: "내가 고친 이름" },
      { version: 1, name: "내가 고친 이름" },
    ]);
  });

  it("없는 id는 전면 Notice", async () => {
    server.use(http.get("*/api/v1/promotions/:promotionId", () => HttpResponse.json(errorBody("RESOURCE_NOT_FOUND", "none"), { status: 404 })));
    renderEditor();
    await screen.findByText("찾을 수 없습니다");
    expect(screen.getByRole("button", { name: "프로모션 목록으로" })).toBeTruthy();
  });
});
