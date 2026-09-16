import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { HttpResponse, http } from "msw";
import { afterEach, describe, expect, it, vi } from "vitest";
import { errorBody, roomType } from "@/lib/api/mocks/fixtures";
import { requestLog } from "@/lib/api/mocks/handlers";
import { server } from "@/lib/api/mocks/server";
import { queryWrapper, setActorCookie, setupMockServer } from "@/lib/api/mocks/test-utils";
import { BannerProvider } from "../Banner";
import { RoomTypePanel } from "./RoomTypePanel";

setupMockServer();
afterEach(() => cleanup());

function renderPanel(roomTypeId: string | null) {
  const onClose = vi.fn();
  const { Wrapper } = queryWrapper();
  render(
    <Wrapper>
      <BannerProvider>
        <RoomTypePanel propertyId="prop_001" roomTypeId={roomTypeId} onClose={onClose} />
      </BannerProvider>
    </Wrapper>,
  );
  return { onClose };
}

const writes = () => requestLog.filter((r) => r.method !== "GET");

describe("H3 옆 패널", () => {
  it("등록. 화면 검사에 걸리면 요청이 안 나가고, 통과하면 CAT-06 본문과 닫기", async () => {
    setActorCookie("host_001");
    const { onClose } = renderPanel(null);
    fireEvent.change(screen.getByLabelText("최대 인원", { exact: false }), { target: { value: "101" } });
    fireEvent.click(screen.getByRole("button", { name: "등록" }));
    expect(screen.getByText("이름을(를) 입력하세요.")).toBeTruthy();
    expect(screen.getByText("최대 인원은(는) 1부터 100까지입니다.")).toBeTruthy();
    expect(writes()).toHaveLength(0);

    fireEvent.change(screen.getByLabelText("이름", { exact: false }), { target: { value: "리버 트윈" } });
    fireEvent.change(screen.getByLabelText("최대 인원", { exact: false }), { target: { value: "3" } });
    fireEvent.click(screen.getByRole("button", { name: "등록" }));
    await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1));
    expect(writes()).toHaveLength(1);
    expect(writes()[0].path).toBe("/api/v1/properties/prop_001/room-types");
    expect(writes()[0].body).toEqual({ name: "리버 트윈", maxOccupancy: 3 });
    expect(screen.getByText("객실 타입을 등록했습니다.")).toBeTruthy();
  });

  it("수정. 열릴 때 CAT-08을 읽고 바뀐 필드와 version만 PATCH", async () => {
    setActorCookie("host_001");
    const { onClose } = renderPanel("rt_001");
    const nameInput = (await screen.findByLabelText("이름", { exact: false })) as HTMLInputElement;
    expect(nameInput.value).toBe(roomType.name);
    expect(requestLog[0].path).toBe("/api/v1/room-types/rt_001");

    fireEvent.change(nameInput, { target: { value: "리버 트윈 넓은 방" } });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1));
    expect(writes()[0].method).toBe("PATCH");
    expect(writes()[0].body).toEqual({ version: roomType.version, name: "리버 트윈 넓은 방" });
  });

  it("VERSION_CONFLICT면 Notice와 새로 읽기, 그 뒤 저장은 오른 version으로 성공한다", async () => {
    setActorCookie("host_001");
    let reads = 0;
    // 덮어쓴 핸들러는 requestLog에 안 남아 본문을 따로 받는다
    const patchBodies: unknown[] = [];
    server.use(
      http.get("*/api/v1/room-types/:roomTypeId", () => {
        reads += 1;
        // 두 번째 읽기부터 남이 고친 판(version 1, 설명 바뀜)
        return HttpResponse.json(reads === 1 ? roomType : { ...roomType, version: 1, description: "남이 고친 설명" });
      }),
      http.patch("*/api/v1/room-types/:roomTypeId", async ({ request }) => {
        const body = (await request.json()) as { version: number };
        patchBodies.push(body);
        if (body.version === 0) return HttpResponse.json(errorBody("VERSION_CONFLICT", "version conflict"), { status: 409 });
        return HttpResponse.json({ ...roomType, ...body, version: body.version + 1 });
      }),
    );
    const { onClose } = renderPanel("rt_001");
    const nameInput = (await screen.findByLabelText("이름", { exact: false })) as HTMLInputElement;
    fireEvent.change(nameInput, { target: { value: "내가 고친 이름" } });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));

    await screen.findByText("다른 곳에서 먼저 수정되어 저장하지 못했습니다. 새로 읽은 뒤 다시 저장하세요.");
    expect(onClose).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "새로 읽기" }));
    await waitFor(() => expect((screen.getByLabelText("설명", { exact: false }) as HTMLTextAreaElement).value).toBe("남이 고친 설명"));
    expect(nameInput.value).toBe("내가 고친 이름");
    expect(screen.queryByText("새로 읽기")).toBeNull();

    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1));
    // 첫 저장은 옛 version 0으로 409, 새로 읽은 뒤 저장은 version 1과 내가 고친 필드만
    expect(patchBodies).toEqual([
      { version: 0, name: "내가 고친 이름" },
      { version: 1, name: "내가 고친 이름" },
    ]);
  });
});
