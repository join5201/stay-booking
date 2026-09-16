import { expect, test } from "@playwright/test";
import { BACKEND, GUEST, MOCK_SYSTEM, api, bookingOf, inventoryOf, newKey, reserveThroughScreens, seedStay, setActor } from "./support";

// E03. 보류(DEFER)면 처리 중 카드. 시험이 INTERNAL-01로 승인 이벤트를 넣고 새로 고침하면 확정(계약 8-1 E03)
test("E03 결제 보류 뒤 모의 PG 승인 이벤트와 새로 고침", async ({ page, context, request }) => {
  const stay = await seedStay(request, "E03");
  await setActor(context, GUEST, "DEFER");
  const bookingId = await reserveThroughScreens(page, stay);

  await page.getByRole("button", { name: "결제하기" }).click();
  await expect(page.getByText("결제를 처리 중입니다")).toBeVisible();
  await expect(page.getByText("1/3")).toBeVisible();
  // 처리 중에는 다시 결제하기가 잠긴다(G5 processing)
  await expect(page.getByRole("button", { name: "다시 결제하기" })).toBeDisabled();

  const pending = await bookingOf(request, bookingId);
  expect(pending.status).toBe("HELD");
  expect(pending.payment.attempts).toHaveLength(1);
  const attempt = pending.payment.attempts[0];
  expect(attempt.status).toBe("REQUESTED");

  // INTERNAL-01. 행위자는 mock_001(MOCK_SYSTEM). 화면 밖 경로라 백엔드로 직접
  const event = await api<{ result: string }>(request, "/internal/mock-payments/events", {
    base: BACKEND,
    method: "POST",
    actor: MOCK_SYSTEM,
    body: { eventId: newKey(), paymentAttemptId: attempt.id, pgTransactionId: attempt.pgTransactionId, outcome: "APPROVED", amount: attempt.amount, currency: attempt.currency },
  });
  expect(event.status, "INTERNAL-01").toBe(200);

  await page.getByRole("button", { name: "새로 고침" }).click();
  await expect(page.getByText("예약이 확정됐습니다")).toBeVisible();
  await expect(page.getByText("결제를 처리 중입니다")).toHaveCount(0);

  const confirmed = await bookingOf(request, bookingId);
  expect(confirmed.status).toBe("CONFIRMED");
  expect(confirmed.payment.attempts[0].status).toBe("APPROVED");
  for (const date of stay.nightDates) {
    const inv = await inventoryOf(request, stay.roomTypeId, date);
    expect(inv, `재고 ${date}`).toMatchObject({ heldCount: 0, soldCount: 1, availableCount: stay.total - 1 });
  }
});
