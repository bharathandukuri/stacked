import { Clock } from "lucide-react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"

interface StepBasicInfoProps {
  title: string
  onTitleChange: (val: string) => void
  description: string
  onDescriptionChange: (val: string) => void
  durationMinutes: number
  onDurationChange: (val: number) => void
  scheduledStart: string
  onScheduledStartChange: (val: string) => void
  scheduledEnd: string
  onScheduledEndChange: (val: string) => void
  maxAttempts: number | undefined
  onMaxAttemptsChange: (val: number | undefined) => void
  passingScore: number | undefined
  onPassingScoreChange: (val: number | undefined) => void
}

export function StepBasicInfo({
  title,
  onTitleChange,
  description,
  onDescriptionChange,
  durationMinutes,
  onDurationChange,
  scheduledStart,
  onScheduledStartChange,
  scheduledEnd,
  onScheduledEndChange,
  maxAttempts,
  onMaxAttemptsChange,
  passingScore,
  onPassingScoreChange,
}: StepBasicInfoProps) {
  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Assessment Details</CardTitle>
          <CardDescription>
            Provide a distinct title and instructions for candidates taking this assessment.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <label className="text-sm font-semibold text-foreground">
              Assessment Title <span className="text-destructive">*</span>
            </label>
            <Input
              placeholder="e.g. Senior Backend Java Engineer Assessment"
              value={title}
              onChange={(e) => onTitleChange(e.target.value)}
            />
          </div>

          <div className="space-y-2">
            <label className="text-sm font-semibold text-foreground">
              Instructions / Description
            </label>
            <Textarea
              placeholder="Enter guidelines, allowed programming languages, reference hints, and conduct rules..."
              rows={4}
              value={description}
              onChange={(e) => onDescriptionChange(e.target.value)}
            />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            <Clock className="size-5 text-primary" />
            Timing & Benchmark Constraints
          </CardTitle>
          <CardDescription>
            Configure overall countdown duration, scheduled availability window, and passing criteria.
          </CardDescription>
        </CardHeader>
        <CardContent className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="space-y-2">
            <label className="text-sm font-semibold text-foreground">
              Duration (Minutes) <span className="text-destructive">*</span>
            </label>
            <Input
              type="number"
              min={1}
              value={durationMinutes}
              onChange={(e) => onDurationChange(Math.max(1, parseInt(e.target.value) || 1))}
            />
          </div>

          <div className="space-y-2">
            <label className="text-sm font-semibold text-foreground">
              Passing Score (Optional)
            </label>
            <Input
              type="number"
              min={0}
              placeholder="e.g. 70 (Leave blank for no minimum)"
              value={passingScore ?? ""}
              onChange={(e) =>
                onPassingScoreChange(
                  e.target.value === "" ? undefined : parseFloat(e.target.value) || 0
                )
              }
            />
          </div>

          <div className="space-y-2">
            <label className="text-sm font-semibold text-foreground">
              Scheduled Start (Optional)
            </label>
            <Input
              type="datetime-local"
              value={scheduledStart}
              onChange={(e) => onScheduledStartChange(e.target.value)}
            />
          </div>

          <div className="space-y-2">
            <label className="text-sm font-semibold text-foreground">
              Scheduled End (Optional)
            </label>
            <Input
              type="datetime-local"
              value={scheduledEnd}
              onChange={(e) => onScheduledEndChange(e.target.value)}
            />
          </div>

          <div className="space-y-2 sm:col-span-2">
            <label className="text-sm font-semibold text-foreground">
              Max Attempts Allowed per Candidate
            </label>
            <Input
              type="number"
              min={1}
              placeholder="Default is 1 attempt"
              value={maxAttempts ?? ""}
              onChange={(e) =>
                onMaxAttemptsChange(
                  e.target.value === "" ? undefined : parseInt(e.target.value) || 1
                )
              }
            />
          </div>
        </CardContent>
      </Card>
    </div>
  )
}

