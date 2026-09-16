import type { ReactNode } from "react";
import { Skeleton } from "./Skeleton";

export interface Column<Row> {
  key: string;
  header: string;
  render: (row: Row) => ReactNode;
  align?: "left" | "right" | "center";
  width?: string;
}

export interface DataTableProps<Row> {
  columns: readonly Column<Row>[];
  rows: readonly Row[];
  rowKey: (row: Row) => string;
  loading?: boolean;
  // rows가 비었을 때. EmptyState를 넣는다
  empty?: ReactNode;
  onRowClick?: (row: Row) => void;
}

const ALIGN = { left: "text-left", right: "text-right", center: "text-center" };

export function DataTable<Row>({ columns, rows, rowKey, loading, empty, onRowClick }: DataTableProps<Row>) {
  if (loading) return <Skeleton lines={5} />;
  if (rows.length === 0 && empty) return <>{empty}</>;
  return (
    <div className="overflow-hidden rounded-card border border-line bg-surface">
      <table className="w-full border-collapse text-14">
        <thead>
          <tr className="bg-surface-2 text-12 text-ink-3">
            {columns.map((c) => (
              <th key={c.key} scope="col" style={{ width: c.width }} className={`border-b border-line px-3 py-2 font-semibold ${ALIGN[c.align ?? "left"]}`}>
                {c.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr
              key={rowKey(row)}
              onClick={onRowClick ? () => onRowClick(row) : undefined}
              className={`border-b border-line-soft last:border-b-0 ${onRowClick ? "cursor-pointer hover:bg-surface-2" : ""}`}
            >
              {columns.map((c) => (
                <td key={c.key} className={`px-3 py-2.5 ${ALIGN[c.align ?? "left"]}`}>
                  {c.render(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
