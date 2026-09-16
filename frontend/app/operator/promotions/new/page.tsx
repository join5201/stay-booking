"use client";

import { useRouter } from "next/navigation";
import { useBanner } from "@/components/Banner";
import { PromotionForm, type PromotionSubmit } from "@/components/operator/PromotionForm";
import { useWriteError } from "@/components/useWriteError";
import { useCreatePromotion } from "@/lib/api/hooks";
import type { CreatePromotionBody } from "@/lib/api/types";

// O2 등록. 저장은 PROMO-01(enabled는 생략이라 기본 true). 성공하면 O1로
export default function NewPromotionPage() {
  const router = useRouter();
  const banner = useBanner();
  const create = useCreatePromotion();
  const writeError = useWriteError();

  const save = (body: CreatePromotionBody) => {
    writeError.reset();
    create.mutate(body, {
      onSuccess: () => {
        banner.notify("프로모션을 등록했습니다.");
        router.push("/operator/promotions");
      },
      onError: (error) => writeError.present(error, () => save(body)),
    });
  };

  return (
    <div className="flex flex-col gap-5">
      <h1 className="text-22 font-semibold">프로모션 등록</h1>
      <PromotionForm
        baseline={null}
        saving={create.isPending}
        serverErrors={writeError.fields}
        notice={writeError.notice ?? writeError.notFound}
        conflict={false}
        onSubmit={(out: PromotionSubmit) => {
          if (out.kind === "create") save(out.body);
        }}
        onCancel={() => router.push("/operator/promotions")}
      />
    </div>
  );
}
