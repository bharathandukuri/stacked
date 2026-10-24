import { BarChart3 } from "lucide-react"
import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty"

interface AnalyticsEmptyStateProps {
  title?: string
  description?: string
  className?: string
}

export function AnalyticsEmptyState({
  title = "No activity recorded yet",
  description = "Events and metrics will appear here once candidates start assessments or content is published.",
  className,
}: AnalyticsEmptyStateProps) {
  return (
    <Empty className={className}>
      <EmptyHeader>
        <EmptyMedia variant="icon">
          <BarChart3 className="size-6 text-muted-foreground" />
        </EmptyMedia>
        <EmptyTitle>{title}</EmptyTitle>
        <EmptyDescription>{description}</EmptyDescription>
      </EmptyHeader>
    </Empty>
  )
}

