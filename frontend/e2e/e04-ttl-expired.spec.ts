import { expect, test } from "@playwright/test";
import { GUEST, bookingOf, holdBooking, inventoryOf, seedStay, setActor, trackBookingReads } from "./support";

// E04. hold-ttl 20초 백엔드. 남은 시간이 0에 닿으면 화면이 BOOK-03을 한 번 다시 읽고, 서버가 EXPIRED라 해야 만료 카드를 그린다(계약 8-1 E04, P01).
// 화면이 스스로 만료로 바꾸지 않았음은 요청 기록으로 본다: 만료 카드 직전의 마지막 응답이 EXPIRED다.
// 서버의 만료 처리는 스캔 주기가 있어 0 도달 직후의 응답이 아직 HELD일 수 있다. 그때는 1초 뒤 한 번 더 읽는다(Countdown ZERO_RETRY_MS. 이 시험이 잡은 되풀이 폭주의 수정)
test("E04 남은 시간 0 도달 뒤 서버 재조회로만 만료 카드", async ({ page, context, request }) => {
  const stay = await seedStay(request, "E04");
  const booking = await holdBooking(request, stay);
  expect(booking.status).toBe("HELD");

  await setActor(context, GUEST, "APPROVE");
  const reads = trackBookingReads(page, booking.id);
  const enteredAt = Date.now();
  await page.goto(`/bookings/${booking.id}/pay`);
  await expect(page.getByText("남은 시간")).toBeVisible();
  await expect(page.getByRole("button", { name: "결제하기" })).toBeEnabled();
  await expect.poll(() => reads.length, { message: "진입 시 BOOK-03 한 번" }).toBe(1);
  expect(reads[0].bookingStatus).toBe("HELD");

  // hold-ttl 20초 안에 만료 카드. 화면이 부른 마지막 읽기가 EXPIRED를 받은 뒤에만 그려진다
  await expect(page.getByText("예약이 만료됐습니다", { exact: true })).toBeVisible({ timeout: 40_000 });
  const shownAt = Date.now();
  await expect(page.getByText("남은 시간 안에 결제하지 않아 예약이 만료됐습니다.")).toBeVisible();
  await expect(page.getByRole("button", { name: "결제하기" })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "같은 조건으로 새 예약" })).toBeVisible();

  // 요청 기록. 0 도달 전에는 읽지 않았고, 0 도달 뒤 읽은 응답이 아직 HELD면 1초 뒤에 한 번 더(Countdown ZERO_RETRY_MS). 되풀이 폭주가 아니다
  const timeline = reads.map((r) => ({ status: r.bookingStatus, sinceEntryMs: r.at - reads[0].at }));
  test.info().annotations.push({ type: "BOOK-03 reads", description: JSON.stringify(timeline) });
  expect(reads.length, "진입 1회와 0 도달 뒤 한두 번").toBeGreaterThanOrEqual(2);
  expect(reads.length, "되풀이 폭주 없음").toBeLessThanOrEqual(4);
  expect(reads[0].bookingStatus).toBe("HELD");
  expect(reads.at(-1)?.bookingStatus).toBe("EXPIRED");
  expect(reads.slice(1, -1).every((r) => r.bookingStatus === "HELD"), "EXPIRED는 마지막 응답뿐").toBe(true);
  expect(reads[1].at - reads[0].at, "두 번째 읽기는 남은 시간이 다 지난 뒤").toBeGreaterThanOrEqual(15_000);
  for (let i = 2; i < reads.length; i += 1) expect(reads[i].at - reads[i - 1].at, `${i}번째 읽기 간격`).toBeGreaterThanOrEqual(900);
  expect(reads[reads.length - 1].at, "만료 카드는 EXPIRED 응답 뒤").toBeLessThanOrEqual(shownAt);
  expect(shownAt - enteredAt, "만료 카드까지 40초 안").toBeLessThanOrEqual(40_000);

  const expired = await bookingOf(request, booking.id);
  expect(expired.status).toBe("EXPIRED");
  expect(expired.expirationReason).toBe("TTL_EXPIRED");
  expect(expired.payment.attemptCount).toBe(0);
  for (const date of stay.nightDates) {
    const inv = await inventoryOf(request, stay.roomTypeId, date);
    expect(inv, `재고 반환 ${date}`).toMatchObject({ heldCount: 0, soldCount: 0, availableCount: stay.total });
  }
});
