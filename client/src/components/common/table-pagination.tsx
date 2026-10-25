import { Button } from "@/components/ui/button"
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select"

interface TablePaginationProps {
  page: number
  pageSize: number
  totalElements: number
  totalPages: number
  isFetching?: boolean
  onPageChange: (newPage: number) => void
  onPageSizeChange?: (newPageSize: number) => void
  pageSizeOptions?: number[]
}

export function TablePagination({
  page,
  pageSize,
  totalElements,
  totalPages,
  isFetching = false,
  onPageChange,
  onPageSizeChange,
  pageSizeOptions = [10, 20, 50],
}: TablePaginationProps) {
  const fromRecord = totalElements === 0 ? 0 : page * pageSize + 1
  const toRecord = Math.min((page + 1) * pageSize, totalElements)

  return (
    <div className="flex flex-col items-center justify-between gap-3 border-t border-border px-6 py-4 sm:flex-row">
      <div className="flex items-center gap-2 text-xs text-muted-foreground">
        {onPageSizeChange && (
          <>
            <span>Rows per page:</span>
            <NativeSelect
              value={pageSize}
              onChange={(e) => onPageSizeChange(Number(e.target.value))}
              className="h-8 w-18 text-xs"
            >
              {pageSizeOptions.map((opt) => (
                <NativeSelectOption key={opt} value={String(opt)}>
                  {opt}
                </NativeSelectOption>
              ))}
            </NativeSelect>
          </>
        )}
        <span className="ml-2">
          Showing {fromRecord}–{toRecord} of {totalElements}
        </span>
      </div>

      <div className="flex items-center gap-2">
        <Button
          variant="outline"
          size="sm"
          disabled={page === 0 || isFetching}
          onClick={() => onPageChange(Math.max(0, page - 1))}
          className="h-8 text-xs"
        >
          Previous
        </Button>
        <span className="px-2 text-xs font-medium text-muted-foreground">
          Page {page + 1} of {Math.max(1, totalPages)}
        </span>
        <Button
          variant="outline"
          size="sm"
          disabled={page >= totalPages - 1 || isFetching}
          onClick={() => onPageChange(page + 1)}
          className="h-8 text-xs"
        >
          Next
        </Button>
      </div>
    </div>
  )
}

