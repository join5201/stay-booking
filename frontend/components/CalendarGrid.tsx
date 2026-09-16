"use client";

import type { ReactNode } from "react";
import { eachDay, monthKeyOf, weekdayOf } from "@/lib/dates";

export interface CalendarGridProps {
  // 양끝 포함. 최대 366일은 PeriodPicker가 막는다
  from: string;
  to: string;
  // 날짜 칸의 내용. 재고 넷이나 요금. 누락은 부모가 표시한다
  renderCell: (date: string) => ReactNode;
  selectedDate?: string | null;
  onSelect?: (date: string) => void;
}

const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"];

// 달마다 7열 격자. 기간 밖 칸은 비운다. H4와 H5가 renderCell만 바꿔 같이 쓴다
export function CalendarGrid({ from, to, renderCell, selectedDate, onSelect }: CalendarGridProps) {
  const days = eachDay(from, to);
  const months = new Map<string, string[]>();
  for (const d of days) {
    const key = monthKeyOf(d);
    const list = months.get(key);
    if (list) list.push(d);
    else months.set(key, [d]);
  }

  return (
    <div className="flex flex-col gap-6">
      {Array.from(months.entries()).map(([month, list]) => {
        const lead = weekdayOf(list[0]);
        return (
          <section key={month} aria-label={month}>
            <h3 className="mb-2 text-15 font-semibold tabular-nums">{month.replace("-", "년 ")}월</h3>
            <div className="grid grid-cols-7 gap-px overflow-hidden rounded-card border border-line bg-line">
              {WEEKDAYS.map((w) => (
                <div key={w} className="bg-surface-2 py-1.5 text-center text-12 text-ink-3">
                  {w}
                </div>
              ))}
              {Array.from({ length: lead }, (_, i) => (
                <div key={`lead-${i}`} className="min-h-20 bg-surface-missing" />
              ))}
              {list.map((date) => {
                const selected = date === selectedDate;
                const day = Number(date.slice(8, 10));
                const body = (
                  <>
                    <span className="block text-12 text-ink-3 tabular-nums">{day}</span>
                    <div className="mt-1 text-13">{renderCell(date)}</div>
                  </>
                );
                return onSelect ? (
                  <button
                    key={date}
                    type="button"
                    aria-pressed={selected}
                    aria-label={date}
                    onClick={() => onSelect(date)}
                    className={`min-h-20 bg-surface p-1.5 text-left hover:bg-surface-2 ${selected ? "outline outline-2 -outline-offset-2 outline-devbar" : ""}`}
                  >
                    {body}
                  </button>
                ) : (
                  <div key={date} aria-label={date} className="min-h-20 bg-surface p-1.5">
                    {body}
                  </div>
                );
              })}
            </div>
          </section>
        );
      })}
    </div>
  );
}
