"use client";

import Link from "next/link";
import { useParams, usePathname, useRouter, useSearchParams } from "next/navigation";
import { useCallback } from "react";
import { Button } from "@/components/Button";
import { DataTable, type Column } from "@/components/DataTable";
import { EmptyState } from "@/components/EmptyState";
import { RoomTypePanel } from "@/components/host/RoomTypePanel";
import { Pagination } from "@/components/Pagination";
import { QueryErrorNotice } from "@/components/QueryErrorNotice";
import { useProperty, useRoomTypes } from "@/lib/api/hooks";
import type { RoomType } from "@/lib/api/types";

const PAGE_SIZE = 20;

// H3 객실 타입. CAT-09 목록. ?new=1과 ?edit=roomTypeId가 옆 패널을 연다(인계 문서 25행과 112행). 저장 뒤 CAT-09 재조회는 훅의 무효화가 한다
export default function RoomTypesPage() {
  const { propertyId } = useParams<{ propertyId: string }>();
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();
  const page = Math.max(0, Number(params.get("page") ?? "0") || 0);
  const editId = params.get("edit");
  const creating = params.get("new") === "1";

  const property = useProperty(propertyId);
  const list = useRoomTypes(propertyId, { page, size: PAGE_SIZE });

  const closePanel = useCallback(() => {
    router.replace(page > 0 ? `${pathname}?page=${page}` : pathname);
  }, [router, pathname, page]);

  const openNew = () => router.push(`${pathname}?new=1`);
  const openEdit = (id: string) => router.push(`${pathname}?edit=${id}`);

  const columns: Column<RoomType>[] = [
    { key: "name", header: "이름", render: (r) => <span className="font-semibold">{r.name}</span> },
    { key: "maxOccupancy", header: "최대 인원", width: "110px", align: "right", render: (r) => <span className="tabular-nums">{r.maxOccupancy}명</span> },
    { key: "description", header: "설명", render: (r) => <span className="text-ink-2">{r.description ? (r.description.length > 60 ? `${r.description.slice(0, 60)}...` : r.description) : "-"}</span> },
    {
      key: "actions",
      header: "",
      width: "90px",
      align: "right",
      render: (r) => (
        <button type="button" onClick={() => openEdit(r.id)} className="text-13 text-devbar hover:underline">
          수정
        </button>
      ),
    },
  ];

  return (
    <div className="flex flex-col gap-5">
      <header className="flex items-center justify-between">
        <div>
          <p className="text-13 text-ink-3">
            <Link href="/host/properties" className="hover:underline">
              내 숙소
            </Link>
            {" / "}
            {property.data?.name ?? propertyId}
          </p>
          <h1 className="text-22 font-semibold">객실 타입</h1>
        </div>
        <Button onClick={openNew}>객실 타입 등록</Button>
      </header>

      {list.isError ? (
        <QueryErrorNotice error={list.error} onRetry={() => void list.refetch()} retrying={list.isFetching} action={<Button variant="outline" onClick={() => router.push("/host/properties")}>내 숙소로</Button>} />
      ) : (
        <>
          <DataTable
            columns={columns}
            rows={list.data?.items ?? []}
            rowKey={(r) => r.id}
            loading={list.isPending}
            empty={<EmptyState title="객실 타입이 없습니다" description="객실 타입을 만든 뒤 재고와 요금을 날짜별로 등록합니다." action={<Button onClick={openNew}>객실 타입 등록</Button>} />}
          />
          {list.data && list.data.totalPages > 1 ? (
            <Pagination page={list.data.page} totalPages={list.data.totalPages} onChange={(next) => router.push(`${pathname}?page=${next}`)} disabled={list.isFetching} />
          ) : null}
        </>
      )}

      {creating || editId ? <RoomTypePanel propertyId={propertyId} roomTypeId={creating ? null : editId} onClose={closePanel} /> : null}
    </div>
  );
}
