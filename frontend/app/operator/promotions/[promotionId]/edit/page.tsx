"use client";

import { useParams, useRouter } from "next/navigation";
import { PromotionEditor } from "@/components/operator/PromotionEditor";

// O2 수정. /operator/promotions/[promotionId]/edit
export default function EditPromotionPage() {
  const { promotionId } = useParams<{ promotionId: string }>();
  const router = useRouter();
  return <PromotionEditor promotionId={promotionId} onDone={() => router.push("/operator/promotions")} />;
}
