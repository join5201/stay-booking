import { expect, test } from "@playwright/test";
import { HOST, api, seedStay, setActor } from "./support";

interface PropertyView {
  id: string;
  name: string;
  address: string;
  version: number;
}

// E06. H2 수정 화면을 연 채 다른 곳이 먼저 고치면 저장이 409 VERSION_CONFLICT. 새로 읽기 뒤 저장하면 성공(계약 8-1 E06, W10)
test("E06 버전 충돌 뒤 새로 읽기와 재저장", async ({ page, context, request }) => {
  const stay = await seedStay(request, "E06");
  const first = await api<PropertyView>(request, `/api/v1/properties/${stay.propertyId}`, { actor: HOST });
  expect(first.status, "CAT-03").toBe(200);
  const version0 = first.data.version;

  await setActor(context, HOST);
  await page.goto(`/host/properties/${stay.propertyId}/edit`);
  await expect(page.getByRole("heading", { name: "숙소 수정" })).toBeVisible();
  await expect(page.locator("#property-name")).toHaveValue(stay.propertyName);

  // 화면 밖에서 먼저 고친다(CAT-02). version이 하나 오른다
  const elsewhere = await api<PropertyView>(request, `/api/v1/properties/${stay.propertyId}`, { method: "PATCH", actor: HOST, body: { version: version0, address: "부산 수영구 먼저 고친 길 9" } });
  expect(elsewhere.status, "CAT-02 먼저 고침").toBe(200);
  expect(elsewhere.data.version).toBe(version0 + 1);

  // 화면은 옛 version으로 저장 시도. 409
  const newName = `${stay.propertyName} 고침`;
  await page.locator("#property-name").fill(newName);
  const conflicted = page.waitForResponse((r) => r.request().method() === "PATCH" && r.url().endsWith(`/api/v1/properties/${stay.propertyId}`));
  await page.getByRole("button", { name: "저장" }).click();
  const conflictRes = await conflicted;
  expect(conflictRes.status(), "옛 version 저장").toBe(409);
  expect(((await conflictRes.json()) as { code: string }).code).toBe("VERSION_CONFLICT");
  const notice = page.getByRole("alert").filter({ hasText: "저장하지 못했습니다" });
  await expect(notice).toBeVisible();
  await expect(page.getByRole("button", { name: "저장" })).toBeDisabled();

  // 새로 읽기(CAT-03) 뒤 저장. 200이고 version이 둘 오른다
  const reread = page.waitForResponse((r) => r.request().method() === "GET" && r.url().endsWith(`/api/v1/properties/${stay.propertyId}`));
  await notice.getByRole("button", { name: "새로 읽기" }).click();
  expect((await reread).status(), "새로 읽기").toBe(200);
  await expect(notice).toHaveCount(0);
  await expect(page.getByRole("button", { name: "저장" })).toBeEnabled();
  await expect(page.locator("#property-name")).toHaveValue(newName);
  const saved = page.waitForResponse((r) => r.request().method() === "PATCH" && r.url().endsWith(`/api/v1/properties/${stay.propertyId}`));
  await page.getByRole("button", { name: "저장" }).click();
  const savedRes = await saved;
  expect(savedRes.status(), "새 version 저장").toBe(200);
  expect(savedRes.request().postDataJSON()).toMatchObject({ version: version0 + 1, name: newName });
  await expect(page).toHaveURL(/\/host\/properties$/);
  await expect(page.getByRole("status")).toContainText("숙소를 저장했습니다.");
  await expect(page.getByText(newName)).toBeVisible();

  const after = await api<PropertyView>(request, `/api/v1/properties/${stay.propertyId}`, { actor: HOST });
  expect(after.data.name).toBe(newName);
  expect(after.data.version).toBe(version0 + 2);
});
