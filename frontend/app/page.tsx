"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { EmptyState } from "@/components/EmptyState";
import { PropertyCard } from "@/components/guest/PropertyCard";
import { Pagination } from "@/components/Pagination";
import { QueryErrorNotice } from "@/components/QueryErrorNotice";
import { SearchForm, type SearchValues } from "@/components/SearchForm";
import { Skeleton } from "@/components/Skeleton";
import { useSearch } from "@/lib/api/hooks";
import { fieldErrorsOf, placementOf } from "@/lib/errors";
import { nightsOf, searchComplete, searchParamsOf, searchQueryOf, searchValuesOf, validateSearch } from "@/lib/stay";

const PAGE_SIZE = 20;

// G1 검색. 조건 넷이 URL에 있고 화면 검사를 통과할 때만 SEARCH-01(계약 2절 G1 행).
// 검색 버튼은 URL만 바꾸고 요청은 URL에서 나온다. 그래서 뒤로 가기와 새로 고침이 같은 결과를 낸다
export default function SearchPage() {
  const router = useRouter();
  const params = useSearchParams();
  const values = searchValuesOf(params);
  const page = Math.max(0, Number(params.get("page") ?? "0") || 0);
  const complete = searchComplete(values);
  const query = searchQueryOf(values);
  const search = useSearch(complete ? searchParamsOf(values) : null, { page, size: PAGE_SIZE });

  const go = (next: SearchValues, nextPage: number) => {
    router.push(`/${searchQueryOf(next, { page: nextPage > 0 ? nextPage : undefined })}`);
  };

  // 서버 400은 필드 아래(details의 field). field가 없으면 화면 안 Notice에 코드 문구
  const fieldErrors = search.isError && placementOf(search.error) === "field" ? fieldErrorsOf(search.error) : {};
  const underFields = Object.keys(fieldErrors).length > 0;

  return (
    <div className="flex flex-col gap-5">
      <header>
        <h1 className="text-22 font-semibold">숙소 검색</h1>
      </header>

      <SearchForm key={query} initial={values} validate={validateSearch} onSubmit={(v) => go(v, 0)} submitting={complete && search.isFetching} serverErrors={fieldErrors} />

      {!complete ? (
        <p className="text-14 text-ink-3">지역과 체크인과 체크아웃과 인원을 넣고 검색하세요.</p>
      ) : search.isError ? (
        underFields ? null : <QueryErrorNotice error={search.error} onRetry={() => void search.refetch()} retrying={search.isFetching} />
      ) : search.isPending ? (
        <Skeleton lines={6} />
      ) : search.data.items.length === 0 ? (
        <EmptyState title="조건에 맞는 숙소가 없습니다" description="날짜나 인원이나 지역을 바꿔 다시 검색하세요." />
      ) : (
        <>
          <p className="text-13 text-ink-3">
            {values.checkIn} 부터 {values.checkOut} 전까지 {nightsOf(values)}박, {values.guestCount}명. 숙소 {search.data.totalElements}곳
          </p>
          <div className="flex flex-col gap-4">
            {search.data.items.map((r) => (
              <PropertyCard key={r.property.id} result={r} query={query} />
            ))}
          </div>
          {search.data.totalPages > 1 ? <Pagination page={search.data.page} totalPages={search.data.totalPages} onChange={(next) => go(values, next)} disabled={search.isFetching} /> : null}
        </>
      )}
    </div>
  );
}
