import { expect, test } from "@playwright/test";
import { GUEST, bookingOf, inventoryOf, reserveThroughScreens, seedStay, setActor } from "./support";

// E02. 거절 세 번이면 예약이 만료되고 재고가 돌아온다(계약 8-1 E02)
test("E02 결제 거절 세 번 뒤 만료와 재고 반환", async ({ page, context, request }) => {
  const stay = await seedStay(request, "E02");
  await setActor(context, GUEST, "DECLINE");
  const bookingId = await reserveThroughScreens(page, stay);

  const held = await inventoryOf(request, stay.roomTypeId, stay.checkIn);
  expect(held, "선점 직후").toMatchObject({ heldCount: 1, soldCount: 0, availableCount: stay.total - 1 });

  await page.getByRole("button", { name: "결제하기" }).click();
  await expect(page.getByText("1/3")).toBeVisible();
  await page.getByRole("button", { name: "다시 결제하기" }).click();
  await expect(page.getByText("2/3")).toBeVisible();
  await page.getByRole("button", { name: "다시 결제하기" }).click();

  await expect(page.getByText("예약이 만료됐습니다", { exact: true })).toBeVisible();
  await expect(page.getByText("결제 시도 3회가 모두 실패해 예약이 만료됐습니다.")).toBeVisible();
  await expect(page.getByText("3/3")).toBeVisible();
  await expect(page.getByRole("button", { name: "다시 결제하기" })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "같은 조건으로 새 예약" })).toBeVisible();

  const booking = await bookingOf(request, bookingId);
  expect(booking.status).toBe("EXPIRED");
  expect(booking.expirationReason).toBe("PAYMENT_FAILED");
  expect(booking.payment.attemptCount).toBe(3);
  expect(booking.payment.attempts.map((a) => a.status)).toEqual(["FAILED", "FAILED", "FAILED"]);

  for (const date of stay.nightDates) {
    const inv = await inventoryOf(request, stay.roomTypeId, date);
    expect(inv, `재고 반환 ${date}`).toMatchObject({ heldCount: 0, soldCount: 0, availableCount: stay.total });
  }
});
