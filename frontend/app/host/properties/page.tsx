"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Button } from "@/components/Button";
import { DataTable, type Column } from "@/components/DataTable";
import { DateText } from "@/components/DateText";
import { EmptyState } from "@/components/EmptyState";
import { Pagination } from "@/components/Pagination";
import { QueryErrorNotice } from "@/components/QueryErrorNotice";
import { useMyProperties } from "@/lib/api/hooks";
import type { Property } from "@/lib/api/types";
import { regionLabel } from "@/lib/regions";

const PAGE_SIZE = 20;

// H1 내 숙소. CAT-05 목록과 페이지. 401과 403은 Notice(계약 2절 H1 행)
export default function HostPropertiesPage() {
  const router = useRouter();
  const params = useSearchParams();
  const page = Math.max(0, Number(params.get("page") ?? "0") || 0);
  const list = useMyProperties({ page, size: PAGE_SIZE });

  const columns: Column<Property>[] = [
    { key: "name", header: "이름", render: (p) => <span className="font-semibold">{p.name}</span> },
    { key: "region", header: "지역", width: "140px", render: (p) => regionLabel(p.regionCode) },
    { key: "address", header: "주소", render: (p) => <span className="text-ink-2">{p.address}</span> },
    { key: "updatedAt", header: "수정", width: "170px", render: (p) => <DateText value={p.updatedAt} className="text-ink-3" /> },
    {
      key: "actions",
      header: "",
      width: "150px",
      align: "right",
      render: (p) => (
        <span className="flex justify-end gap-3 text-13">
          <Link href={`/host/properties/${p.id}/edit`} className="text-devbar hover:underline">
            수정
          </Link>
          <Link href={`/host/properties/${p.id}/room-types`} className="text-devbar hover:underline">
            객실 타입
          </Link>
        </span>
      ),
    },
  ];

  return (
    <div className="flex flex-col gap-5">
      <header className="flex items-center justify-between">
        <h1 className="text-22 font-semibold">내 숙소</h1>
        <Button onClick={() => router.push("/host/properties/new")}>숙소 등록</Button>
      </header>

      {list.isError ? (
        <QueryErrorNotice error={list.error} onRetry={() => void list.refetch()} retrying={list.isFetching} />
      ) : (
        <>
          <DataTable
            columns={columns}
            rows={list.data?.items ?? []}
            rowKey={(p) => p.id}
            loading={list.isPending}
            empty={<EmptyState title="등록한 숙소가 없습니다" description="숙소를 등록하면 객실 타입과 재고와 요금을 이어서 만들 수 있습니다." action={<Button onClick={() => router.push("/host/properties/new")}>숙소 등록</Button>} />}
          />
          {list.data && list.data.totalPages > 1 ? (
            <Pagination page={list.data.page} totalPages={list.data.totalPages} onChange={(next) => router.push(`/host/properties?page=${next}`)} disabled={list.isFetching} />
          ) : null}
        </>
      )}
    </div>
  );
}
