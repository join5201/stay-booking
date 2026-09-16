"use client";

import { useBanner } from "../Banner";
import { QueryErrorNotice } from "../QueryErrorNotice";
import { SidePanel } from "../SidePanel";
import { Skeleton } from "../Skeleton";
import { useWriteError } from "../useWriteError";
import { useCreateRoomType, useRoomType, useUpdateRoomType } from "@/lib/api/hooks";
import type { RegisterRoomTypeBody, RoomType, UpdateRoomTypeBody } from "@/lib/api/types";
import { RoomTypeForm, type RoomTypeSubmit } from "./RoomTypeForm";

export interface RoomTypePanelProps {
  propertyId: string;
  // null이면 등록(?new=1). 있으면 수정(?edit=)이고 열릴 때 CAT-08을 새로 읽는다
  roomTypeId: string | null;
  onClose: () => void;
}

// H3 옆 패널. 등록은 CAT-06, 수정은 CAT-07. 성공하면 닫고 훅의 무효화가 CAT-09를 다시 읽는다
export function RoomTypePanel({ propertyId, roomTypeId, onClose }: RoomTypePanelProps) {
  const editing = roomTypeId !== null;
  return (
    <SidePanel open title={editing ? "객실 타입 수정" : "객실 타입 등록"} onClose={onClose}>
      {editing ? <EditBody roomTypeId={roomTypeId} onClose={onClose} /> : <CreateBody propertyId={propertyId} onClose={onClose} />}
    </SidePanel>
  );
}

function CreateBody({ propertyId, onClose }: { propertyId: string; onClose: () => void }) {
  const banner = useBanner();
  const create = useCreateRoomType(propertyId);
  const writeError = useWriteError();

  const save = (body: RegisterRoomTypeBody) => {
    writeError.reset();
    create.mutate(body, {
      onSuccess: () => {
        banner.notify("객실 타입을 등록했습니다.");
        onClose();
      },
      onError: (error) => writeError.present(error, () => save(body)),
    });
  };

  return (
    <RoomTypeForm
      baseline={null}
      saving={create.isPending}
      serverErrors={writeError.fields}
      notice={writeError.notice ?? writeError.notFound}
      conflict={false}
      onSubmit={(out: RoomTypeSubmit) => {
        if (out.kind === "create") save(out.body);
      }}
      onCancel={onClose}
    />
  );
}

function EditBody({ roomTypeId, onClose }: { roomTypeId: string; onClose: () => void }) {
  const banner = useBanner();
  const roomType = useRoomType(roomTypeId, { fresh: true });
  const update = useUpdateRoomType(roomTypeId);
  const writeError = useWriteError();

  const save = (body: UpdateRoomTypeBody) => {
    writeError.reset();
    update.mutate(body, {
      onSuccess: () => {
        banner.notify("객실 타입을 저장했습니다.");
        onClose();
      },
      onError: (error) => writeError.present(error, () => save(body)),
    });
  };

  const reload = () => {
    writeError.reset();
    void roomType.refetch();
  };

  if (roomType.isPending) return <Skeleton lines={5} />;
  if (roomType.isError) return <QueryErrorNotice error={roomType.error} onRetry={() => void roomType.refetch()} retrying={roomType.isFetching} />;
  return (
    <RoomTypeForm
      baseline={roomType.data satisfies RoomType}
      saving={update.isPending}
      serverErrors={writeError.fields}
      notice={writeError.notice ?? writeError.notFound}
      conflict={writeError.conflict}
      onReload={reload}
      reloading={roomType.isFetching}
      onSubmit={(out: RoomTypeSubmit) => {
        if (out.kind === "update") save(out.body);
      }}
      onCancel={onClose}
    />
  );
}
