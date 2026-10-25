import {
  Clock,
  Users,
  AlertCircle,
  FileQuestion,
  Lock,
  Radio,
  RefreshCw,
  Award,
  KeyRound,
  Copy,
  Check,
  Share2,
  Ban,
  CheckCircle2,
  Play,
  Loader2,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import type {
  AssessmentDetailResponse,
  AssessmentItemResponse,
  AssessmentStatus,
} from "@/types/assessment"

interface AssessmentDetailHeaderProps {
  assessment: AssessmentDetailResponse
  onRefreshAll: () => void
  onOpenActivateConfirm: () => void
  onOpenCompleteConfirm: () => void
  onRevokeInvitationCode: () => void
  isRevokingCode: boolean
  onGenerateInvitationCode: () => void
  isGeneratingCode: boolean
  onCopyCode: (code: string) => void
  copiedCode: boolean
  onCopyJoinLink: (code: string) => void
  copiedLink: boolean
}

export function AssessmentDetailHeader({
  assessment,
  onRefreshAll,
  onOpenActivateConfirm,
  onOpenCompleteConfirm,
  onRevokeInvitationCode,
  isRevokingCode,
  onGenerateInvitationCode,
  isGeneratingCode,
  onCopyCode,
  copiedCode,
  onCopyJoinLink,
  copiedLink,
}: AssessmentDetailHeaderProps) {
  const getStatusBadge = (status: AssessmentStatus) => {
    switch (status) {
      case "ACTIVE":
        return (
          <Badge className="bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30 gap-1 text-xs">
            <Radio className="size-3 animate-pulse" />
            Live / Active
          </Badge>
        )
      case "NOT_STARTED":
        return (
          <Badge variant="secondary" className="gap-1 text-xs">
            <Clock className="size-3" />
            Scheduled / Draft
          </Badge>
        )
      case "COMPLETED":
        return (
          <Badge variant="outline" className="gap-1 text-xs">
            <CheckCircle2 className="size-3 text-muted-foreground" />
            Concluded
          </Badge>
        )
      default:
        return (
          <Badge variant="outline" className="text-xs">
            {status}
          </Badge>
        )
    }
  }

  return (
    <div className="space-y-6">
      {/* Top Action Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b">
        <div className="space-y-1">
          <div className="flex flex-wrap items-center gap-2.5">
            <h1 className="text-2xl font-bold tracking-tight text-foreground">
              {assessment.title}
            </h1>
            {getStatusBadge(assessment.status)}
            {assessment.manuallyCompleted && (
              <Badge variant="destructive" className="text-xs">
                Manually Completed
              </Badge>
            )}
          </div>
          <p className="text-xs text-muted-foreground truncate font-mono">
            ID: {assessment.id} • Slug: {assessment.slug}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2 shrink-0">
          <Button
            variant="outline"
            size="sm"
            onClick={onRefreshAll}
            className="gap-1.5 text-xs h-8"
          >
            <RefreshCw className="h-3.5 w-3.5" />
            <span>Refresh</span>
          </Button>

          <div
            className="inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded-md text-xs font-medium bg-muted/60 text-muted-foreground border border-border"
            title="Assessment configuration is permanently locked and immutable. If changes are needed, create a new assessment."
          >
            <Lock className="h-3 w-3 text-muted-foreground" />
            <span>Immutable</span>
          </div>

          {assessment.status === "NOT_STARTED" && (
            <Button
              size="sm"
              onClick={onOpenActivateConfirm}
              className="gap-1.5 text-xs h-8 bg-emerald-600 hover:bg-emerald-700 text-white"
            >
              <Play className="size-3.5 fill-current" />
              <span>Activate Assessment</span>
            </Button>
          )}

          {assessment.status !== "COMPLETED" && (
            <Button
              variant="outline"
              size="sm"
              onClick={onOpenCompleteConfirm}
              className="gap-1.5 text-xs h-8 text-purple-600 border-purple-500/30 hover:bg-purple-500/10 dark:text-purple-400"
            >
              <CheckCircle2 className="size-3.5" />
              <span>Complete Assessment</span>
            </Button>
          )}
        </div>
      </div>

      {assessment.manualInterventionMessage && (
        <div className="flex items-center gap-3 p-4 rounded-lg bg-amber-500/10 border border-amber-500/30 text-amber-900 dark:text-amber-200 text-sm">
          <AlertCircle className="size-5 text-amber-600 shrink-0" />
          <div>
            <p className="font-semibold text-xs uppercase tracking-wide text-amber-700 dark:text-amber-400">
              Manual Intervention Notice
            </p>
            <p className="mt-0.5 text-sm">{assessment.manualInterventionMessage}</p>
          </div>
        </div>
      )}

      {/* Key Metric Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-muted-foreground">
                  Duration & Schedule
                </p>
                <p className="text-2xl font-bold mt-1">
                  {assessment.durationMinutes} min
                </p>
                <p className="text-xs text-muted-foreground mt-1">
                  {assessment.scheduledStart
                    ? new Date(assessment.scheduledStart).toLocaleDateString()
                    : "Always open"}
                  {assessment.scheduledEnd
                    ? ` - ${new Date(assessment.scheduledEnd).toLocaleDateString()}`
                    : ""}
                </p>
              </div>
              <Clock className="h-8 w-8 text-muted-foreground/40" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-muted-foreground">
                  Candidates Assigned
                </p>
                <p className="text-2xl font-bold mt-1">
                  {assessment.assignedCandidateCount}
                </p>
                <p className="text-xs text-muted-foreground mt-1">
                  {assessment.maxAttempts
                    ? `Max ${assessment.maxAttempts} attempts`
                    : "Unlimited attempts"}
                </p>
              </div>
              <Users className="h-8 w-8 text-muted-foreground/40" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-muted-foreground">
                  Total Questions
                </p>
                <p className="text-2xl font-bold mt-1">
                  {assessment.items?.length ?? 0}
                </p>
                <p className="text-xs text-muted-foreground mt-1">
                  {assessment.items?.filter((i: AssessmentItemResponse) => i.questionType === "CODING").length ?? 0}{" "}
                  Code,{" "}
                  {assessment.items?.filter((i: AssessmentItemResponse) => i.questionType === "MCQ").length ?? 0}{" "}
                  MCQ
                </p>
              </div>
              <FileQuestion className="h-8 w-8 text-muted-foreground/40" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-muted-foreground">
                  Maximum Points
                </p>
                <p className="text-2xl font-bold mt-1">
                  {assessment.totalScore} pts
                </p>
                <p className="text-xs text-muted-foreground mt-1">
                  {assessment.passingScore
                    ? `Pass mark: ${assessment.passingScore} pts`
                    : "No minimum passing score"}
                </p>
              </div>
              <Award className="h-8 w-8 text-muted-foreground/40" />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Invitation Code & Access Card */}
      <Card className="border-border/80">
        <CardHeader className="pb-3">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
            <div className="space-y-1">
              <CardTitle className="text-base flex items-center gap-2">
                <KeyRound className="size-4 text-primary" />
                <span>Candidate Invitation Code & Access</span>
              </CardTitle>
              <CardDescription>
                Share this code or link with candidates to allow instant self-registration and testing.
              </CardDescription>
            </div>

            <div className="flex items-center gap-2">
              {assessment.invitationCode && assessment.invitationCodeEnabled && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={onRevokeInvitationCode}
                  disabled={isRevokingCode}
                  className="text-xs text-destructive hover:bg-destructive/10 border-destructive/30 gap-1.5"
                >
                  {isRevokingCode ? (
                    <Loader2 className="size-3.5 animate-spin" />
                  ) : (
                    <Ban className="size-3.5" />
                  )}
                  <span>Revoke Code</span>
                </Button>
              )}

              <Button
                variant="outline"
                size="sm"
                onClick={onGenerateInvitationCode}
                disabled={isGeneratingCode}
                className="text-xs gap-1.5"
              >
                {isGeneratingCode ? (
                  <Loader2 className="size-3.5 animate-spin" />
                ) : (
                  <RefreshCw className="size-3.5" />
                )}
                <span>
                  {assessment.invitationCode ? "Regenerate Code" : "Generate Code"}
                </span>
              </Button>
            </div>
          </div>
        </CardHeader>

        <CardContent>
          {assessment.invitationCode ? (
            <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4 p-4 rounded-xl border bg-muted/20">
              <div className="space-y-1.5">
                <div className="flex items-center gap-2">
                  <span className="font-mono text-xl font-bold tracking-widest text-primary">
                    {assessment.invitationCode}
                  </span>
                  {assessment.invitationCodeEnabled ? (
                    <Badge className="bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30 text-[11px]">
                      Active
                    </Badge>
                  ) : (
                    <Badge variant="destructive" className="text-[11px]">
                      Revoked
                    </Badge>
                  )}
                </div>

                <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-muted-foreground">
                  <span>
                    Uses:{" "}
                    <strong className="text-foreground">
                      {assessment.invitationCodeUsedCount ?? 0}
                    </strong>
                    {assessment.invitationCodeMaxUses
                      ? ` / ${assessment.invitationCodeMaxUses}`
                      : " (Unlimited)"}
                  </span>
                  <span>•</span>
                  <span>
                    Expires:{" "}
                    <strong className="text-foreground">
                      {assessment.invitationCodeExpiresAt
                        ? new Date(assessment.invitationCodeExpiresAt).toLocaleString()
                        : "Never"}
                    </strong>
                  </span>
                </div>
              </div>

              <div className="flex items-center gap-2 shrink-0">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => onCopyCode(assessment.invitationCode!)}
                  className="text-xs gap-1.5"
                >
                  {copiedCode ? (
                    <Check className="size-3.5 text-emerald-500" />
                  ) : (
                    <Copy className="size-3.5" />
                  )}
                  <span>Copy Code</span>
                </Button>

                <Button
                  size="sm"
                  onClick={() => onCopyJoinLink(assessment.invitationCode!)}
                  className="text-xs gap-1.5 font-medium"
                >
                  {copiedLink ? (
                    <Check className="size-3.5 text-white" />
                  ) : (
                    <Share2 className="size-3.5" />
                  )}
                  <span>Copy Join Link</span>
                </Button>
              </div>
            </div>
          ) : (
            <div className="p-4 rounded-xl border border-dashed text-center space-y-2">
              <p className="text-sm font-medium text-foreground">
                No Invitation Code Active
              </p>
              <p className="text-xs text-muted-foreground max-w-md mx-auto">
                Generate an invitation code to enable candidate self-service entry without having to manually input emails in advance.
              </p>
              <Button
                size="sm"
                variant="outline"
                onClick={onGenerateInvitationCode}
                disabled={isGeneratingCode}
                className="gap-1.5 mt-1"
              >
                {isGeneratingCode ? (
                  <Loader2 className="size-3.5 animate-spin" />
                ) : (
                  <KeyRound className="size-3.5" />
                )}
                <span>Generate Invitation Code</span>
              </Button>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
