import { expect, test } from "@playwright/test";
import { GUEST, HOST, bookingOf, confirmFromG3, inventoryOf, seoulDate, setActor } from "./support";

// E01. 호스트가 화면으로 숙소와 객실과 재고와 요금을 만들고, 게스트가 검색해서 예약하고 결제 승인까지(계약 8-1 E01)
test("E01 호스트 등록부터 게스트 결제 승인까지", async ({ page, context, request }) => {
  const tag = Date.now().toString(36);
  const propertyName = `E2E E01 스테이 ${tag}`;
  const roomTypeName = `E2E E01 더블 ${tag}`;
  const checkIn = seoulDate(12);
  const lastNight = seoulDate(13);
  const checkOut = seoulDate(14);

  await setActor(context, HOST);

  // H2 숙소 등록(CAT-01)
  await page.goto("/host/properties/new");
  await expect(page.getByRole("heading", { name: "숙소 등록" })).toBeVisible();
  await page.locator("#property-name").fill(propertyName);
  await page.locator("#property-region").selectOption("BUSAN");
  await page.locator("#property-address").fill("부산 해운대구 E2E로 1");
  await page.locator("#property-description").fill("E01 화면 등록");
  const propertyCreated = page.waitForResponse((r) => r.request().method() === "POST" && r.url().endsWith("/api/v1/properties"));
  await page.getByRole("button", { name: "등록" }).click();
  const propertyRes = await propertyCreated;
  expect(propertyRes.status(), "CAT-01").toBe(201);
  const property = (await propertyRes.json()) as { id: string };
  await expect(page).toHaveURL(/\/host\/properties$/);
  await expect(page.getByText(propertyName)).toBeVisible();

  // H3 객실 타입 등록(CAT-06). 옆 패널
  await page.goto(`/host/properties/${property.id}/room-types?new=1`);
  const roomTypePanel = page.getByRole("dialog", { name: "객실 타입 등록" });
  await expect(roomTypePanel).toBeVisible();
  await roomTypePanel.locator("#room-type-name").fill(roomTypeName);
  await roomTypePanel.locator("#room-type-max-occupancy").fill("2");
  await roomTypePanel.locator("#room-type-description").fill("E01");
  const roomTypeCreated = page.waitForResponse((r) => r.request().method() === "POST" && r.url().endsWith(`/api/v1/properties/${property.id}/room-types`));
  await roomTypePanel.getByRole("button", { name: "등록" }).click();
  const roomTypeRes = await roomTypeCreated;
  expect(roomTypeRes.status(), "CAT-06").toBe(201);
  const roomType = (await roomTypeRes.json()) as { id: string };
  await expect(roomTypePanel).toBeHidden();
  await expect(page.getByText(roomTypeName)).toBeVisible();

  // H4 재고 일괄 등록(INV-02). 화면의 기간은 양끝 포함
  await page.goto(`/host/room-types/${roomType.id}/inventories`);
  await page.getByRole("button", { name: "일괄 등록" }).click();
  const bulkPanel = page.getByRole("dialog", { name: "재고 일괄 등록" });
  await expect(bulkPanel).toBeVisible();
  await bulkPanel.locator("#bulk-from").fill(checkIn);
  await bulkPanel.locator("#bulk-to").fill(lastNight);
  await bulkPanel.locator("#bulk-total-count").fill("3");
  const bulkCreated = page.waitForResponse((r) => r.request().method() === "POST" && r.url().endsWith(`/api/v1/room-types/${roomType.id}/inventories/bulk`));
  await bulkPanel.getByRole("button", { name: "일괄 등록" }).click();
  expect((await bulkCreated).status(), "INV-02").toBe(201);
  await expect(bulkPanel).toBeHidden();

  // H5 요금 등록(RATE-01). 밤마다 한 번
  await page.goto(`/host/room-types/${roomType.id}/rates`);
  for (const date of [checkIn, lastNight]) {
    await page.getByRole("button", { name: date, exact: true }).click();
    const ratePanel = page.getByRole("dialog", { name: `${date} 요금 등록` });
    await expect(ratePanel).toBeVisible();
    await ratePanel.locator("#rate-amount").fill("100000");
    const rateCreated = page.waitForResponse((r) => r.request().method() === "POST" && r.url().endsWith(`/api/v1/room-types/${roomType.id}/rates`));
    await ratePanel.getByRole("button", { name: "등록" }).click();
    expect((await rateCreated).status(), `RATE-01 ${date}`).toBe(201);
    await expect(ratePanel).toBeHidden();
  }
  for (const date of [checkIn, lastNight]) {
    const inv = await inventoryOf(request, roomType.id, date);
    expect(inv, `재고 ${date}`).toMatchObject({ totalCount: 3, heldCount: 0, soldCount: 0, availableCount: 3 });
  }

  // G1 검색(SEARCH-01). 카드의 객실 줄이 G3
  await setActor(context, GUEST, "APPROVE");
  await page.goto("/");
  await page.locator("#search-region").selectOption("BUSAN");
  await page.locator("#search-checkin").fill(checkIn);
  await page.locator("#search-checkout").fill(checkOut);
  await page.locator("#search-guests").fill("2");
  await page.getByRole("button", { name: "검색" }).click();
  await expect(page).toHaveURL(/regionCode=BUSAN/);
  await expect(page.getByRole("link", { name: propertyName })).toBeVisible();
  await page.getByRole("link", { name: roomTypeName }).click();

  // G3부터 G5. 결제 승인(PAY-01 APPROVE)
  await expect(page).toHaveURL(new RegExp(`/room-types/${roomType.id}\\?`));
  await expect(page.getByRole("heading", { name: roomTypeName })).toBeVisible();
  const bookingId = await confirmFromG3(page);
  await expect(page.getByText("남은 시간")).toBeVisible();
  await page.getByRole("button", { name: "결제하기" }).click();
  await expect(page.getByText("예약이 확정됐습니다")).toBeVisible();
  await expect(page.getByText("1/3")).toBeVisible();

  // 서버와 재고의 근거
  const booking = await bookingOf(request, bookingId);
  expect(booking.status).toBe("CONFIRMED");
  expect(booking.payment.attemptCount).toBe(1);
  for (const date of [checkIn, lastNight]) {
    const inv = await inventoryOf(request, roomType.id, date);
    expect(inv, `재고 ${date}`).toMatchObject({ totalCount: 3, heldCount: 0, soldCount: 1, availableCount: 2 });
  }
});
