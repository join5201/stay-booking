import { expect, test } from "@playwright/test";
import { GUEST, bookingOf, inventoryOf, reserveThroughScreens, seedStay, setActor } from "./support";

// E05. 확정 예약을 G7에서 취소하면 환불 줄이 생기고 soldCount가 내려간다(계약 8-1 E05)
test("E05 확정 뒤 취소와 환불과 재고 복원", async ({ page, context, request }) => {
  const stay = await seedStay(request, "E05");
  await setActor(context, GUEST, "APPROVE");
  const bookingId = await reserveThroughScreens(page, stay);

  await page.getByRole("button", { name: "결제하기" }).click();
  await expect(page.getByText("예약이 확정됐습니다")).toBeVisible();
  const sold = await inventoryOf(request, stay.roomTypeId, stay.checkIn);
  expect(sold, "확정 직후").toMatchObject({ heldCount: 0, soldCount: 1, availableCount: stay.total - 1 });

  // G5에서 G7로
  await page.getByRole("button", { name: "예약 상세로" }).click();
  await expect(page).toHaveURL(new RegExp(`/bookings/${bookingId}$`));
  await expect(page.getByRole("heading", { name: "예약 상세" })).toBeVisible();
  await expect(page.getByText("예약이 확정됐습니다")).toBeVisible();
  const cancelButton = page.getByRole("button", { name: "예약 취소" });
  await expect(cancelButton).toBeEnabled();

  // 취소 시트(BOOK-04)
  await cancelButton.click();
  const sheet = page.getByRole("dialog", { name: "예약을 취소할까요?" });
  await expect(sheet).toBeVisible();
  await sheet.getByRole("textbox").fill("E05 일정 변경");
  await expect(sheet.getByText("9/300. 비워도 됩니다.")).toBeVisible();
  const canceled = page.waitForResponse((r) => r.request().method() === "POST" && r.url().endsWith(`/api/v1/bookings/${bookingId}/cancellations`));
  await sheet.getByRole("button", { name: "예약 취소" }).click();
  const cancelRes = await canceled;
  expect(cancelRes.status(), "BOOK-04").toBe(200);
  expect(cancelRes.request().headers()["idempotency-key"], "취소에도 키").toBeTruthy();
  expect(cancelRes.request().postDataJSON()).toEqual({ reason: "E05 일정 변경" });

  await expect(sheet).toBeHidden();
  await expect(page.getByRole("status")).toContainText("예약을 취소했습니다.");
  await expect(page.getByText("예약이 취소됐습니다")).toBeVisible();
  await expect(page.getByText("사유: E05 일정 변경")).toBeVisible();
  await expect(page.getByText("환불").first()).toBeVisible();
  await expect(page.getByRole("button", { name: "예약 취소" })).toHaveCount(0);

  const booking = await bookingOf(request, bookingId);
  expect(booking.status).toBe("CANCELED");
  expect(booking.payment.refund?.amount).toBe(stay.amountPerNight * stay.nights);
  for (const date of stay.nightDates) {
    const inv = await inventoryOf(request, stay.roomTypeId, date);
    expect(inv, `재고 복원 ${date}`).toMatchObject({ heldCount: 0, soldCount: 0, availableCount: stay.total });
  }
});
