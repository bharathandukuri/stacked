import { RefreshCw } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { cn } from "@/lib/utils"

export type DateRangeDays = 7 | 14 | 30 | 90

interface AnalyticsHeaderProps {
  days: DateRangeDays
  onDaysChange: (days: DateRangeDays) => void
  onRefresh: () => void
  isRefreshing: boolean
  lastUpdated?: Date
}

export function AnalyticsHeader({
  days,
  onDaysChange,
  onRefresh,
  isRefreshing,
  lastUpdated,
}: AnalyticsHeaderProps) {
  const options: { label: string; value: DateRangeDays }[] = [
    { label: "7 Days", value: 7 },
    { label: "14 Days", value: 14 },
    { label: "30 Days", value: 30 },
    { label: "90 Days", value: 90 },
  ]

  return (
    <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between border-b border-border/80 bg-background/95 pb-5 backdrop-blur-sm">
      <div>
        <div className="flex items-center gap-2">
          <h1 className="text-xl font-bold tracking-tight text-foreground sm:text-2xl">
            Analytics & Platform Telemetry
          </h1>
          <Badge
            variant="outline"
            className="hidden items-center gap-1.5 border-emerald-500/30 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 sm:inline-flex text-[11px]"
          >
            <span className="size-1.5 rounded-full bg-emerald-500 animate-pulse" />
            Live Event Stream
          </Badge>
        </div>
        <p className="text-xs text-muted-foreground mt-1">
          Candidate participation funnels, real-time code judging metrics, and operational performance
          {lastUpdated && (
            <span className="ml-1 opacity-70">
              • Updated at {lastUpdated.toLocaleTimeString()}
            </span>
          )}
        </p>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        {/* Date Range Selector Pills */}
        <div className="inline-flex rounded-lg border border-border bg-muted/40 p-1 shadow-xs">
          {options.map((opt) => (
            <button
              key={opt.value}
              type="button"
              onClick={() => onDaysChange(opt.value)}
              className={cn(
                "rounded-md px-2.5 py-1 text-xs font-medium transition-all cursor-pointer",
                days === opt.value
                  ? "bg-background text-foreground shadow-xs font-semibold"
                  : "text-muted-foreground hover:text-foreground"
              )}
            >
              {opt.label}
            </button>
          ))}
        </div>

        {/* Manual Refresh Button */}
        <Button
          variant="outline"
          size="sm"
          onClick={onRefresh}
          disabled={isRefreshing}
          className="h-8 gap-1.5 text-xs border-border"
        >
          <RefreshCw className={cn("size-3.5", isRefreshing && "animate-spin text-primary")} />
          <span>Refresh</span>
        </Button>
      </div>
    </div>
  )
}
