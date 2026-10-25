import { type ReactNode } from "react"
import { type LucideIcon, Inbox } from "lucide-react"
import {
  Empty,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
  EmptyDescription,
} from "@/components/ui/empty"

interface TableEmptyStateProps {
  icon?: LucideIcon
  title?: string
  description?: string
  action?: ReactNode
  colSpan?: number
  isTableRow?: boolean
  className?: string
}

export function TableEmptyState({
  icon: Icon = Inbox,
  title = "No data found",
  description = "No items match your filter criteria.",
  action,
  colSpan = 5,
  isTableRow = true,
  className,
}: TableEmptyStateProps) {
  const content = (
    <Empty className={className}>
      <EmptyHeader>
        <EmptyMedia variant="icon">
          <Icon />
        </EmptyMedia>
        <EmptyTitle className="text-sm font-semibold">{title}</EmptyTitle>
        <EmptyDescription className="text-xs">{description}</EmptyDescription>
      </EmptyHeader>
      {action && <div className="mt-2">{action}</div>}
    </Empty>
  )

  if (isTableRow) {
    return (
      <tr>
        <td colSpan={colSpan} className="p-8 text-center">
          {content}
        </td>
      </tr>
    )
  }

  return content
}
