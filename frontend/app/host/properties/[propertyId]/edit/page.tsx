"use client";

import { useParams, useRouter } from "next/navigation";
import { useBanner } from "@/components/Banner";
import { Button } from "@/components/Button";
import { PropertyForm, type PropertySubmit } from "@/components/host/PropertyForm";
import { Notice } from "@/components/Notice";
import { QueryErrorNotice } from "@/components/QueryErrorNotice";
import { Skeleton } from "@/components/Skeleton";
import { useWriteError } from "@/components/useWriteError";
import { useProperty, useUpdateProperty } from "@/lib/api/hooks";
import type { UpdatePropertyBody } from "@/lib/api/types";

// H2 숙소 수정. 진입 시 CAT-03(fresh), 저장은 CAT-02(version 숨김, 바뀐 필드만). 409면 새로 읽기, 404는 전면
export default function EditPropertyPage() {
  const { propertyId } = useParams<{ propertyId: string }>();
  const router = useRouter();
  const banner = useBanner();
  const property = useProperty(propertyId, { fresh: true });
  const update = useUpdateProperty(propertyId);
  const writeError = useWriteError();

  const save = (body: UpdatePropertyBody) => {
    writeError.reset();
    update.mutate(body, {
      onSuccess: () => {
        banner.notify("숙소를 저장했습니다.");
        router.push("/host/properties");
      },
      onError: (error) => writeError.present(error, () => save(body)),
    });
  };

  const onSubmit = (out: PropertySubmit) => {
    if (out.kind === "update") save(out.body);
  };

  // 새로 읽기. 입력값은 폼이 들고 있고 기준(version 포함)만 바뀐다
  const reload = () => {
    writeError.reset();
    void property.refetch();
  };

  let body;
  if (property.isPending) body = <Skeleton lines={6} />;
  else if (property.isError)
    body = <QueryErrorNotice error={property.error} onRetry={() => void property.refetch()} retrying={property.isFetching} action={<Button variant="outline" onClick={() => router.push("/host/properties")}>내 숙소로</Button>} />;
  else if (writeError.notFound)
    body = (
      <Notice fullPage title="찾을 수 없습니다" action={<Button variant="outline" onClick={() => router.push("/host/properties")}>내 숙소로</Button>}>
        {writeError.notFound}
      </Notice>
    );
  else
    body = (
      <PropertyForm
        baseline={property.data}
        saving={update.isPending}
        serverErrors={writeError.fields}
        notice={writeError.notice}
        conflict={writeError.conflict}
        onReload={reload}
        reloading={property.isFetching}
        onSubmit={onSubmit}
        onCancel={() => router.push("/host/properties")}
      />
    );

  return (
    <div className="flex flex-col gap-5">
      <h1 className="text-22 font-semibold">숙소 수정</h1>
      {body}
    </div>
  );
}
