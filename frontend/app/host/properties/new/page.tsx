"use client";

import { useRouter } from "next/navigation";
import { useBanner } from "@/components/Banner";
import { PropertyForm, type PropertySubmit } from "@/components/host/PropertyForm";
import { useWriteError } from "@/components/useWriteError";
import { useCreateProperty } from "@/lib/api/hooks";
import type { RegisterPropertyBody } from "@/lib/api/types";

// H2 숙소 등록. CAT-01. 저장 뒤 H1(계약 2절 H2 행)
export default function NewPropertyPage() {
  const router = useRouter();
  const banner = useBanner();
  const create = useCreateProperty();
  const writeError = useWriteError();

  const save = (body: RegisterPropertyBody) => {
    writeError.reset();
    create.mutate(body, {
      onSuccess: () => {
        banner.notify("숙소를 등록했습니다.");
        router.push("/host/properties");
      },
      onError: (error) => writeError.present(error, () => save(body)),
    });
  };

  const onSubmit = (out: PropertySubmit) => {
    if (out.kind === "create") save(out.body);
  };

  return (
    <div className="flex flex-col gap-5">
      <h1 className="text-22 font-semibold">숙소 등록</h1>
      <PropertyForm
        baseline={null}
        saving={create.isPending}
        serverErrors={writeError.fields}
        notice={writeError.notice ?? writeError.notFound}
        conflict={false}
        onSubmit={onSubmit}
        onCancel={() => router.push("/host/properties")}
      />
    </div>
  );
}
