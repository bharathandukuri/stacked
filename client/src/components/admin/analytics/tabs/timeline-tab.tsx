import { useState } from "react"
import {
  Clock,
  Filter,
  ChevronLeft,
  ChevronRight,
  Shield,
  User,
  Cpu,
  GraduationCap,
  PlayCircle,
  CheckCircle2,
  AlertCircle,
  FileCode2,
  Terminal,
} from "lucide-react"
import { format, formatDistanceToNow, parseISO } from "date-fns"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { AnalyticsEmptyState } from "../analytics-empty-state"
import { useActivityTimelineQuery } from "@/hooks/use-analytics"
import type {
  ActivityTimelineItemResponse,
  AnalyticsActorType,
  AnalyticsEventType,
} from "@/types/analytics"
import { cn } from "@/lib/utils"

export function TimelineTab() {
  const [eventTypeFilter, setEventTypeFilter] = useState<string | undefined>(undefined)
  const [page, setPage] = useState(0)
  const pageSize = 15

  const { data: timelineData, isLoading, isPlaceholderData } = useActivityTimelineQuery({
    eventType: eventTypeFilter,
    page,
    size: pageSize,
  })

  const filterOptions: { label: string; value?: string }[] = [
    { label: "All Events", value: undefined },
    { label: "Attempts Started", value: "ATTEMPT_STARTED" },
    { label: "Final Submissions", value: "ATTEMPT_SUBMITTED" },
    { label: "Code Evaluations", value: "CODE_SUBMISSION_EVALUATED" },
    { label: "Assessment Activated", value: "ASSESSMENT_ACTIVATED" },
    { label: "Assessment Completed", value: "ASSESSMENT_COMPLETED" },
    { label: "Candidate Enrolled", value: "CANDIDATE_ENROLLED" },
  ]

  const items = timelineData?.items || []
  const totalElements = timelineData?.totalElements || 0
  const totalPages = timelineData?.totalPages || 0

  return (
    <Card className="border-border/80">
      <CardHeader>
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <CardTitle className="text-base font-semibold flex items-center gap-2">
              <Clock className="size-4 text-primary" />
              Platform Activity & Audit Timeline
            </CardTitle>
            <CardDescription className="text-xs">
              Chronological log of administrative actions, candidate attempts, and execution judging
            </CardDescription>
          </div>

          <div className="flex items-center gap-2">
            <span className="text-xs text-muted-foreground font-mono">
              {totalElements.toLocaleString()} Total Records
            </span>
          </div>
        </div>

        {/* Filter Pills */}
        <div className="flex flex-wrap items-center gap-1.5 pt-2">
          <Filter className="size-3.5 text-muted-foreground mr-1" />
          {filterOptions.map((opt) => (
            <button
              key={opt.label}
              type="button"
              onClick={() => {
                setEventTypeFilter(opt.value)
                setPage(0)
              }}
              className={cn(
                "rounded-md border px-2.5 py-1 text-xs font-medium transition-all cursor-pointer",
                eventTypeFilter === opt.value
                  ? "border-primary bg-primary/10 text-primary font-semibold"
                  : "border-border/70 bg-background text-muted-foreground hover:border-border hover:text-foreground"
              )}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </CardHeader>

      <CardContent>
        {isLoading ? (
          <div className="space-y-3">
            {Array.from({ length: 6 }).map((_, i) => (
              <div
                key={i}
                className="flex items-center justify-between rounded-lg border border-border/60 p-3"
              >
                <div className="flex items-center gap-3">
                  <Skeleton className="size-8 rounded-full" />
                  <div className="space-y-1.5">
                    <Skeleton className="h-4 w-48" />
                    <Skeleton className="h-3 w-32" />
                  </div>
                </div>
                <Skeleton className="h-4 w-24" />
              </div>
            ))}
          </div>
        ) : items.length === 0 ? (
          <AnalyticsEmptyState
            title="No activity events found"
            description="No events match the selected filter. Try choosing 'All Events' or clearing search criteria."
            className="py-12 border border-dashed border-border/70"
          />
        ) : (
          <div className="space-y-2.5">
            {items.map((event) => (
              <TimelineEventRow key={event.id} event={event} />
            ))}

            {/* Pagination Controls */}
            {totalPages > 1 && (
              <div className="flex items-center justify-between border-t border-border/70 pt-4 mt-4">
                <span className="text-xs text-muted-foreground">
                  Page {page + 1} of {totalPages}
                </span>
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={page === 0 || isLoading}
                    className="h-8 gap-1 text-xs"
                  >
                    <ChevronLeft className="size-3.5" />
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setPage((p) => p + 1)}
                    disabled={page >= totalPages - 1 || isLoading || isPlaceholderData}
                    className="h-8 gap-1 text-xs"
                  >
                    Next
                    <ChevronRight className="size-3.5" />
                  </Button>
                </div>
              </div>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  )
}

function TimelineEventRow({ event }: { event: ActivityTimelineItemResponse }) {
  const [expanded, setExpanded] = useState(false)

  let relativeTime = ""
  let absoluteTime = ""
  try {
    const parsed = parseISO(event.timestamp)
    relativeTime = formatDistanceToNow(parsed, { addSuffix: true })
    absoluteTime = format(parsed, "yyyy-MM-dd HH:mm:ss")
  } catch {
    relativeTime = event.timestamp
  }

  const { icon, colorClass, label } = getEventMeta(event.eventType)
  const actorBadge = getActorBadge(event.actorType)

  const hasAttributes = event.attributes && Object.keys(event.attributes).length > 0

  return (
    <div className="rounded-lg border border-border/70 bg-card hover:bg-muted/10 transition-colors p-3 text-xs">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
        <div className="flex items-start gap-3">
          <div className={cn("rounded-md p-2 shrink-0 mt-0.5", colorClass)}>
            {icon}
          </div>
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <span className="font-semibold text-foreground text-sm">{label}</span>
              {actorBadge}
              <Badge variant="outline" className="text-[10px] text-muted-foreground border-border">
                {event.entityType}
              </Badge>
              {event.assessmentId && (
                <span className="text-[10px] text-muted-foreground font-mono">
                  asmt: {event.assessmentId.slice(-6)}
                </span>
              )}
            </div>

            <div className="flex flex-wrap items-center gap-2 text-muted-foreground mt-1 text-[11px]">
              {event.candidateEmail && (
                <span className="text-foreground/90 font-medium">{event.candidateEmail}</span>
              )}
              {event.entityId && (
                <span className="font-mono text-muted-foreground">id: {event.entityId}</span>
              )}
            </div>
          </div>
        </div>

        <div className="flex items-center gap-3 shrink-0 self-end sm:self-center">
          <div className="text-right">
            <span className="text-foreground font-medium">{relativeTime}</span>
            <p className="text-[10px] text-muted-foreground">{absoluteTime}</p>
          </div>
          {hasAttributes && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setExpanded(!expanded)}
              className="h-7 px-2 text-[11px] text-muted-foreground hover:text-foreground"
            >
              {expanded ? "Hide Details" : "Details"}
            </Button>
          )}
        </div>
      </div>

      {/* Expandable Metadata Payload */}
      {expanded && hasAttributes && (
        <div className="mt-3 rounded-md border border-border/60 bg-muted/30 p-2.5 font-mono text-[11px] overflow-x-auto text-foreground/90">
          <div className="grid gap-1">
            {Object.entries(event.attributes!).map(([key, val]) => (
              <div key={key} className="flex gap-2">
                <span className="text-muted-foreground">{key}:</span>
                <span className="font-semibold">{String(val)}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

function getEventMeta(type: AnalyticsEventType) {
  switch (type) {
    case "ATTEMPT_STARTED":
      return {
        label: "Attempt Started",
        icon: <PlayCircle className="size-4 text-blue-500" />,
        colorClass: "bg-blue-500/10",
      }
    case "ATTEMPT_SUBMITTED":
      return {
        label: "Attempt Finalized",
        icon: <CheckCircle2 className="size-4 text-emerald-500" />,
        colorClass: "bg-emerald-500/10",
      }
    case "ATTEMPT_EXPIRED":
      return {
        label: "Attempt Expired",
        icon: <AlertCircle className="size-4 text-amber-500" />,
        colorClass: "bg-amber-500/10",
      }
    case "CODE_SUBMISSION_EVALUATED":
      return {
        label: "Code Evaluated",
        icon: <FileCode2 className="size-4 text-purple-500" />,
        colorClass: "bg-purple-500/10",
      }
    case "SAMPLE_RUN_EXECUTED":
      return {
        label: "Sample Code Executed",
        icon: <Terminal className="size-4 text-indigo-500" />,
        colorClass: "bg-indigo-500/10",
      }
    case "ASSESSMENT_ACTIVATED":
      return {
        label: "Assessment Activated",
        icon: <GraduationCap className="size-4 text-emerald-500" />,
        colorClass: "bg-emerald-500/10",
      }
    case "ASSESSMENT_COMPLETED":
      return {
        label: "Assessment Completed",
        icon: <GraduationCap className="size-4 text-blue-500" />,
        colorClass: "bg-blue-500/10",
      }
    case "CANDIDATE_INVITED":
    case "CANDIDATE_ENROLLED":
      return {
        label: "Candidate Enrolled",
        icon: <User className="size-4 text-indigo-500" />,
        colorClass: "bg-indigo-500/10",
      }
    default:
      return {
        label: type.replace(/_/g, " "),
        icon: <Clock className="size-4 text-muted-foreground" />,
        colorClass: "bg-muted",
      }
  }
}

function getActorBadge(actorType: AnalyticsActorType) {
  switch (actorType) {
    case "ADMIN":
      return (
        <Badge variant="outline" className="text-[10px] border-purple-500/30 text-purple-600 dark:text-purple-400">
          <Shield className="size-2.5 mr-1 inline" /> Admin
        </Badge>
      )
    case "CANDIDATE":
      return (
        <Badge variant="outline" className="text-[10px] border-blue-500/30 text-blue-600 dark:text-blue-400">
          <User className="size-2.5 mr-1 inline" /> Candidate
        </Badge>
      )
    case "SYSTEM":
      return (
        <Badge variant="outline" className="text-[10px] border-muted text-muted-foreground">
          <Cpu className="size-2.5 mr-1 inline" /> System
        </Badge>
      )
    default:
      return null
  }
}

