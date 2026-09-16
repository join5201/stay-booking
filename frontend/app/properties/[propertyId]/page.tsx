"use client";

import Link from "next/link";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import { Button } from "@/components/Button";
import { EmptyState } from "@/components/EmptyState";
import { StayConditions } from "@/components/guest/StayConditions";
import { Pagination } from "@/components/Pagination";
import { QueryErrorNotice } from "@/components/QueryErrorNotice";
import { Skeleton } from "@/components/Skeleton";
import { useProperty, useRoomTypes } from "@/lib/api/hooks";
import { regionLabel } from "@/lib/regions";
import { searchQueryOf, searchValuesOf } from "@/lib/stay";

const PAGE_SIZE = 20;

// G2 숙소 상세. CAT-03과 CAT-09 진입 시. 검색 쿼리를 유지해 객실 줄이 G3에 넘긴다. 404는 전면 Notice(계약 2절 G2 행)
export default function PropertyPage() {
  const { propertyId } = useParams<{ propertyId: string }>();
  const router = useRouter();
  const params = useSearchParams();
  const values = searchValuesOf(params);
  const query = searchQueryOf(values);
  const page = Math.max(0, Number(params.get("rtPage") ?? "0") || 0);
  const property = useProperty(propertyId);
  const roomTypes = useRoomTypes(propertyId, { page, size: PAGE_SIZE });

  if (property.isPending) return <Skeleton lines={6} />;
  if (property.isError) {
    return <QueryErrorNotice error={property.error} onRetry={() => void property.refetch()} retrying={property.isFetching} action={<Button variant="outline" onClick={() => router.push(`/${query}`)}>검색으로</Button>} />;
  }
  const p = property.data;

  return (
    <div className="flex flex-col gap-5">
      <nav aria-label="경로" className="text-13 text-ink-3">
        <Link href={`/${query}`} className="hover:underline">
          검색
        </Link>
        <span className="mx-2">/</span>
        <span>{p.name}</span>
      </nav>

      <header className="flex flex-col gap-2">
        <h1 className="text-22 font-semibold">{p.name}</h1>
        <p className="text-14 text-ink-2">
          {regionLabel(p.regionCode)}, {p.address}
        </p>
        {p.description ? <p className="text-14 text-ink-2">{p.description}</p> : null}
        <StayConditions values={values} />
      </header>

      <section className="flex flex-col gap-3">
        <h2 className="text-16 font-semibold">객실 타입</h2>
        {roomTypes.isError ? (
          <QueryErrorNotice error={roomTypes.error} onRetry={() => void roomTypes.refetch()} retrying={roomTypes.isFetching} />
        ) : roomTypes.isPending ? (
          <Skeleton lines={4} />
        ) : roomTypes.data.items.length === 0 ? (
          <EmptyState title="객실 타입이 없습니다" />
        ) : (
          <>
            <ul className="divide-y divide-line-soft rounded-card border border-line bg-surface">
              {roomTypes.data.items.map((r) => (
                <li key={r.id} className="flex items-center justify-between gap-4 px-5 py-3 text-14">
                  <div className="flex flex-col gap-0.5">
                    <Link href={`/room-types/${r.id}${query}`} className="font-semibold text-devbar hover:underline">
                      {r.name}
                    </Link>
                    <span className="text-13 text-ink-3">최대 {r.maxOccupancy}명{r.description ? `. ${r.description}` : ""}</span>
                  </div>
                  <Link href={`/room-types/${r.id}${query}`} className="text-13 text-devbar hover:underline">
                    가용 수와 예상 금액
                  </Link>
                </li>
              ))}
            </ul>
            {roomTypes.data.totalPages > 1 ? (
              <Pagination page={roomTypes.data.page} totalPages={roomTypes.data.totalPages} onChange={(next) => router.push(`/properties/${propertyId}${searchQueryOf(values, { rtPage: next > 0 ? next : undefined })}`)} disabled={roomTypes.isFetching} />
            ) : null}
          </>
        )}
      </section>
    </div>
  );
}
