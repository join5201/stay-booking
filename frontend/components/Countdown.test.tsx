import { act, cleanup, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { Countdown } from "./Countdown";

// W06. 남은 시간은 expiresAt과 serverNow 차이로 세고 0에서 onZero를 1회만 부른다. 브라우저 시계를 바꿔도 값이 같다
const SERVER_NOW = "2026-09-15T10:00:00Z";
const EXPIRES_AT = "2026-09-15T10:10:00Z";

describe("Countdown (W06)", () => {
  beforeEach(() => {
    // 브라우저 시계를 서버보다 25년 앞으로 틀어 둔다. 절대 시각을 쓰면 바로 0이 된다
    vi.useFakeTimers({ now: new Date("2001-01-01T00:00:00Z") });
  });
  afterEach(() => {
    cleanup();
    vi.useRealTimers();
  });

  it("브라우저 시계와 무관하게 expiresAt과 serverNow 차이에서 시작한다", () => {
    render(<Countdown expiresAt={EXPIRES_AT} serverNow={SERVER_NOW} />);
    expect(Date.now()).toBeLessThan(Date.parse(SERVER_NOW));
    expect(screen.getByText("10:00")).toBeTruthy();
  });

  it("브라우저 시계를 다른 값으로 바꿔도 같은 값이다", () => {
    vi.setSystemTime(new Date("2099-12-31T23:59:59Z"));
    render(<Countdown expiresAt={EXPIRES_AT} serverNow={SERVER_NOW} />);
    expect(screen.getByText("10:00")).toBeTruthy();
  });

  it("1초마다 줄고 0에서 onZero를 한 번만 부른다", () => {
    const onZero = vi.fn();
    render(<Countdown expiresAt={EXPIRES_AT} serverNow={SERVER_NOW} onZero={onZero} />);

    act(() => vi.advanceTimersByTime(1000));
    expect(screen.getByText("09:59")).toBeTruthy();
    expect(onZero).not.toHaveBeenCalled();

    act(() => vi.advanceTimersByTime(599_000));
    expect(screen.getByText("00:00")).toBeTruthy();
    expect(onZero).toHaveBeenCalledTimes(1);

    act(() => vi.advanceTimersByTime(30_000));
    expect(screen.getByText("00:00")).toBeTruthy();
    expect(onZero).toHaveBeenCalledTimes(1);
  });

  it("새 응답(serverNow)이 오면 그 값으로 다시 센다", () => {
    const onZero = vi.fn();
    const { rerender } = render(<Countdown expiresAt={EXPIRES_AT} serverNow={SERVER_NOW} onZero={onZero} />);
    act(() => vi.advanceTimersByTime(600_000));
    expect(onZero).toHaveBeenCalledTimes(1);

    // 서버가 아직 2분 남았다고 하면 2분부터
    rerender(<Countdown expiresAt={EXPIRES_AT} serverNow="2026-09-15T10:08:00Z" onZero={onZero} />);
    expect(screen.getByText("02:00")).toBeTruthy();
    act(() => vi.advanceTimersByTime(120_000));
    expect(onZero).toHaveBeenCalledTimes(2);
  });
});
