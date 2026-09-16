"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Button } from "@/components/Button";
import { DataTable, type Column } from "@/components/DataTable";
import { EmptyState } from "@/components/EmptyState";
import { Pagination } from "@/components/Pagination";
import { QueryErrorNotice } from "@/components/QueryErrorNotice";
import { SegmentedFilter } from "@/components/SegmentedFilter";
import { StatusBadge, promotionBadge } from "@/components/StatusBadge";
import { usePromotions } from "@/lib/api/hooks";
import type { Promotion } from "@/lib/api/types";
import { regionLabel } from "@/lib/regions";

const PAGE_SIZE = 20;

// 필터 셋. URL의 enabled는 true, false, 없음
type EnabledFilter = "all" | "true" | "false";
const FILTERS: { value: EnabledFilter; label: string }[] = [
  { value: "all", label: "전체" },
  { value: "true", label: "사용 중" },
  { value: "false", label: "꺼짐" },
];

function filterOf(value: string | null): EnabledFilter {
  return value === "true" || value === "false" ? value : "all";
}

// O1 프로모션 목록. PROMO-04에 enabled와 page. 켜기와 끄기 토글은 없다(O2 머리에서). 401과 403은 Notice
export default function PromotionsPage() {
  const router = useRouter();
  const params = useSearchParams();
  const page = Math.max(0, Number(params.get("page") ?? "0") || 0);
  const filter = filterOf(params.get("enabled"));
  const list = usePromotions({ page, size: PAGE_SIZE, ...(filter === "all" ? {} : { enabled: filter === "true" }) });

  const go = (nextFilter: EnabledFilter, nextPage: number) => {
    const q = new URLSearchParams();
    if (nextFilter !== "all") q.set("enabled", nextFilter);
    if (nextPage > 0) q.set("page", String(nextPage));
    const s = q.toString();
    router.push(s ? `/operator/promotions?${s}` : "/operator/promotions");
  };

  const columns: Column<Promotion>[] = [
    { key: "name", header: "이름", render: (p) => <span className="font-semibold">{p.name}</span> },
    { key: "discountRate", header: "할인율", width: "80px", align: "right", render: (p) => <span className="tabular-nums">{p.discountRate}%</span> },
    { key: "campaign", header: "캠페인 기간", width: "230px", render: (p) => <span className="tabular-nums text-ink-2">{p.campaignStartDate} 부터 {p.campaignEndDate} 전까지</span> },
    { key: "stay", header: "숙박 기간", width: "230px", render: (p) => (p.stayStartDate && p.stayEndDate ? <span className="tabular-nums text-ink-2">{p.stayStartDate} 부터 {p.stayEndDate} 전까지</span> : <span className="text-ink-3">제한 없음</span>) },
    { key: "minNights", header: "최소 박수", width: "90px", align: "right", render: (p) => <span className="tabular-nums">{p.minNights}박</span> },
    { key: "regions", header: "지역", render: (p) => (p.regionCodes.length === 0 ? <span className="text-ink-3">전체</span> : <span className="text-ink-2">{p.regionCodes.map(regionLabel).join(", ")}</span>) },
    {
      key: "enabled",
      header: "상태",
      width: "90px",
      render: (p) => {
        const badge = promotionBadge(p.enabled);
        return <StatusBadge tone={badge.tone}>{badge.label}</StatusBadge>;
      },
    },
    {
      key: "actions",
      header: "",
      width: "70px",
      align: "right",
      render: (p) => (
        <Link href={`/operator/promotions/${p.id}/edit`} className="text-13 text-devbar hover:underline">
          수정
        </Link>
      ),
    },
  ];

  return (
    <div className="flex flex-col gap-5">
      <header className="flex items-center justify-between">
        <h1 className="text-22 font-semibold">프로모션</h1>
        <Button onClick={() => router.push("/operator/promotions/new")}>프로모션 등록</Button>
      </header>

      <SegmentedFilter options={FILTERS} value={filter} onChange={(v) => go(v, 0)} ariaLabel="사용 여부" />

      {list.isError ? (
        <QueryErrorNotice error={list.error} onRetry={() => void list.refetch()} retrying={list.isFetching} />
      ) : (
        <>
          <DataTable
            columns={columns}
            rows={list.data?.items ?? []}
            rowKey={(p) => p.id}
            loading={list.isPending}
            empty={<EmptyState title="프로모션이 없습니다" description="캠페인 기간과 할인율을 정해 프로모션을 등록합니다." action={<Button onClick={() => router.push("/operator/promotions/new")}>프로모션 등록</Button>} />}
          />
          {list.data && list.data.totalPages > 1 ? <Pagination page={list.data.page} totalPages={list.data.totalPages} onChange={(next) => go(filter, next)} disabled={list.isFetching} /> : null}
        </>
      )}
    </div>
  );
}
