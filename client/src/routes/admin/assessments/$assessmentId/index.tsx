import { useState, useMemo } from "react"
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router"
import {
  ArrowLeft,
  Clock,
  Users,
  AlertCircle,
  FileQuestion,
  Lock,
  Plus,
  Radio,
  RefreshCw,
  Search,
  Code2,
  CheckSquare,
  ShieldCheck,
  ShieldAlert,
  Loader2,
  Layers,
  Award,
  KeyRound,
  Copy,
  Check,
  Share2,
  Ban,
  BarChart3,
  Eye,
  Activity,
  TrendingUp,
  Target,
  CheckCircle2,
} from "lucide-react"

import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { AdminSidebar } from "@/components/admin/sidebar"
import { MonacoEditor } from "@/components/monaco-editor"
import { MarkdownPreview } from "@/components/markdown-preview/markdown-preview"
import {
  useAssessment,
  useAssessmentAttempts,
  useCandidateAssignments,
  useBulkAssignCandidates,
  useCompleteAssessment,
  useGenerateInvitationCode,
  useRevokeInvitationCode,
  useAssessmentMetrics,
  useAttemptDetailForAdmin,
  useAttemptAuditTrail,
} from "@/hooks/use-assessments"
import { resolveMonacoLanguage } from "@/lib/monaco-languages"
import { toast } from "@/components/ui/toast"
import { cn } from "@/lib/utils"
import type {
  AssessmentStatus,
  CandidateParticipationStatus,
  AttemptStatus,
  AssessmentAuditEvent,
} from "@/types/assessment"

export const Route = createFileRoute("/admin/assessments/$assessmentId/")({
  component: AssessmentDetailPage,
})

const EMAIL_REGEX = /[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}/g

function AssessmentDetailPage() {
  const { assessmentId } = Route.useParams()
  const navigate = useNavigate()

  const [activeTab, setActiveTab] = useState("metrics")
  const [candidatePage] = useState(0)
  const [candidateSearch, setCandidateSearch] = useState("")
  const [attemptPage] = useState(0)

  // Bulk Assign Dialog State
  const [bulkAssignOpen, setBulkAssignOpen] = useState(false)
  const [candidateEmailsRaw, setCandidateEmailsRaw] = useState("")

  // Detailed Modals
  const [inspectAttemptId, setInspectAttemptId] = useState<string | null>(null)
  const [auditAttemptId, setAuditAttemptId] = useState<string | null>(null)

  // Queries
  const { data: assessment, isLoading, isError, error, refetch: refetchAssessment } = useAssessment(assessmentId)
  const { data: metricsData, isLoading: loadingMetrics, refetch: refetchMetrics } = useAssessmentMetrics(assessmentId)
  const { data: candidateData, isLoading: loadingCandidates, refetch: refetchCandidates } = useCandidateAssignments(
    assessmentId,
    candidatePage,
    50
  )
  const { data: attemptsData, isLoading: loadingAttempts, refetch: refetchAttempts } = useAssessmentAttempts(
    assessmentId,
    attemptPage,
    50
  )

  // Modals Data
  const { data: attemptDetail, isLoading: loadingDetail } = useAttemptDetailForAdmin(inspectAttemptId || "")
  const { data: auditTrailData, isLoading: loadingAudit } = useAttemptAuditTrail(auditAttemptId || "")

  // Mutations
  const bulkAssignMutation = useBulkAssignCandidates(assessmentId)
  const completeMutation = useCompleteAssessment()
  const generateCodeMutation = useGenerateInvitationCode(assessmentId)
  const revokeCodeMutation = useRevokeInvitationCode(assessmentId)

  const [copiedCode, setCopiedCode] = useState(false)
  const [copiedLink, setCopiedLink] = useState(false)
  const [completeConfirmOpen, setCompleteConfirmOpen] = useState(false)

  const handleCopyCode = (code: string) => {
    navigator.clipboard.writeText(code)
    setCopiedCode(true)
    setTimeout(() => setCopiedCode(false), 2000)
    toast.add({
      title: "Code Copied",
      description: `Invitation code ${code} copied to clipboard!`,
      type: "success",
    })
  }

  const handleCopyJoinLink = (code: string) => {
    const url = `${window.location.origin}/assessments/join?code=${encodeURIComponent(code)}`
    navigator.clipboard.writeText(url)
    setCopiedLink(true)
    setTimeout(() => setCopiedLink(false), 2000)
    toast.add({
      title: "Join Link Copied",
      description: "Candidate invitation link copied to clipboard!",
      type: "success",
    })
  }

  // Parsed candidate emails
  const parsedEmails = useMemo(() => {
    if (!candidateEmailsRaw) return []
    const matches = candidateEmailsRaw.match(EMAIL_REGEX) || []
    return Array.from(new Set(matches.map((e) => e.toLowerCase().trim())))
  }, [candidateEmailsRaw])

  const handleBulkAssignSubmit = async () => {
    if (parsedEmails.length === 0 || !assessment || assessment.status === "COMPLETED") return
    await bulkAssignMutation.mutateAsync({
      emails: parsedEmails,
    })
    setCandidateEmailsRaw("")
    setBulkAssignOpen(false)
    refetchCandidates()
    refetchAssessment()
    refetchMetrics()
  }

  const handleCompleteAssessment = async () => {
    await completeMutation.mutateAsync(assessmentId)
    setCompleteConfirmOpen(false)
    refetchAssessment()
    refetchMetrics()
  }

  if (isLoading) {
    return (
      <div className="flex h-screen bg-background overflow-hidden">
        <AdminSidebar currentPath="/admin/assessments" />
        <main className="flex-1 flex flex-col items-center justify-center gap-3">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
          <p className="text-sm text-muted-foreground">Loading assessment details...</p>
        </main>
      </div>
    )
  }

  if (isError || !assessment) {
    return (
      <div className="flex h-screen bg-background overflow-hidden">
        <AdminSidebar currentPath="/admin/assessments" />
        <main className="flex-1 flex flex-col items-center justify-center gap-4 p-8">
          <AlertCircle className="h-10 w-10 text-destructive" />
          <h2 className="text-xl font-bold">Assessment Not Found</h2>
          <p className="text-sm text-muted-foreground">
            {error instanceof Error ? error.message : "The requested assessment could not be loaded."}
          </p>
          <Button variant="outline" onClick={() => navigate({ to: "/admin/assessments" })}>
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back to Assessments
          </Button>
        </main>
      </div>
    )
  }

  const getStatusBadge = (status: AssessmentStatus) => {
    switch (status) {
      case "ACTIVE":
        return <Badge className="bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30">Active</Badge>
      case "COMPLETED":
        return <Badge className="bg-purple-500/15 text-purple-600 dark:text-purple-400 border-purple-500/30">Completed</Badge>
      case "NOT_STARTED":
      default:
        return <Badge variant="secondary">Not Started</Badge>
    }
  }

  const getParticipationBadge = (status: CandidateParticipationStatus) => {
    switch (status) {
      case "INVITED":
        return <Badge variant="secondary">Invited</Badge>
      case "IN_PROGRESS":
        return <Badge className="bg-amber-500/15 text-amber-600 dark:text-amber-400 border-amber-500/30 animate-pulse">In Progress</Badge>
      case "SUBMITTED":
        return <Badge className="bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30">Submitted</Badge>
      case "EXPIRED":
        return <Badge variant="destructive">Expired</Badge>
      case "CANCELLED":
        return <Badge variant="outline" className="text-muted-foreground">Cancelled</Badge>
      default:
        return <Badge variant="outline">{status}</Badge>
    }
  }

  const getAttemptBadge = (status: AttemptStatus) => {
    switch (status) {
      case "IN_PROGRESS":
        return <Badge className="bg-amber-500/15 text-amber-600 dark:text-amber-400 border-amber-500/30 animate-pulse">In Progress</Badge>
      case "SUBMITTED":
        return <Badge className="bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30">Submitted</Badge>
      case "EXPIRED":
        return <Badge variant="destructive">Expired</Badge>
      case "CANCELLED":
        return <Badge variant="outline">Cancelled</Badge>
      default:
        return <Badge variant="outline">{status}</Badge>
    }
  }

  const getAuditSourceBadge = (source: string) => {
    switch (source) {
      case "SERVER_VERIFIED":
        return (
          <Badge className="bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30 text-[10px] gap-1">
            <ShieldCheck className="size-2.5" />
            <span>Server Verified</span>
          </Badge>
        )
      case "CLIENT_TELEMETRY":
        return (
          <Badge className="bg-amber-500/15 text-amber-600 dark:text-amber-400 border-amber-500/30 text-[10px] gap-1">
            <Eye className="size-2.5" />
            <span>Client Telemetry</span>
          </Badge>
        )
      case "SYSTEM_PRESENCE":
        return (
          <Badge className="bg-sky-500/15 text-sky-600 dark:text-sky-400 border-sky-500/30 text-[10px] gap-1">
            <Radio className="size-2.5" />
            <span>System Presence</span>
          </Badge>
        )
      default:
        return <Badge variant="outline" className="text-[10px]">{source}</Badge>
    }
  }

  return (
    <div className="flex h-screen bg-background overflow-hidden">
      <AdminSidebar currentPath="/admin/assessments" />

      <main className="flex-1 flex flex-col min-w-0 overflow-y-auto">
        {/* Top Header */}
        <header className="border-b bg-card/50 backdrop-blur-sm sticky top-0 z-10 px-8 py-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-center gap-3 min-w-0">
              <Link to="/admin/assessments">
                <Button variant="ghost" size="icon" className="h-8 w-8">
                  <ArrowLeft className="h-4 w-4" />
                </Button>
              </Link>
              <div className="min-w-0">
                <div className="flex items-center gap-2">
                  <h1 className="text-xl font-bold tracking-tight truncate">{assessment.title}</h1>
                  {getStatusBadge(assessment.status)}
                </div>
                <p className="text-xs text-muted-foreground truncate font-mono mt-0.5">
                  ID: {assessment.id} • Slug: {assessment.slug}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-2 shrink-0">
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  refetchAssessment()
                  refetchMetrics()
                  refetchAttempts()
                  refetchCandidates()
                }}
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

              {assessment.status !== "COMPLETED" && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setCompleteConfirmOpen(true)}
                  className="gap-1.5 text-xs h-8 text-purple-600 border-purple-500/30 hover:bg-purple-500/10 dark:text-purple-400"
                >
                  <CheckCircle2 className="size-3.5" />
                  <span>Complete Assessment</span>
                </Button>
              )}
            </div>
          </div>
        </header>

        <div className="p-8 space-y-6">
          {/* Key Metric Summary Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <Card>
              <CardContent className="pt-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-muted-foreground">Duration & Schedule</p>
                    <p className="text-2xl font-bold mt-1">{assessment.durationMinutes} min</p>
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
                    <p className="text-sm font-medium text-muted-foreground">Candidates Assigned</p>
                    <p className="text-2xl font-bold mt-1">{assessment.assignedCandidateCount}</p>
                    <p className="text-xs text-muted-foreground mt-1">
                      {assessment.maxAttempts ? `Max ${assessment.maxAttempts} attempts` : "Unlimited attempts"}
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
                    <p className="text-sm font-medium text-muted-foreground">Total Questions</p>
                    <p className="text-2xl font-bold mt-1">{assessment.items?.length ?? 0}</p>
                    <p className="text-xs text-muted-foreground mt-1">
                      {assessment.items?.filter((i) => i.questionType === "CODING").length ?? 0} Code,{" "}
                      {assessment.items?.filter((i) => i.questionType === "MCQ").length ?? 0} MCQ
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
                    <p className="text-sm font-medium text-muted-foreground">Maximum Points</p>
                    <p className="text-2xl font-bold mt-1">{assessment.totalScore} pts</p>
                    <p className="text-xs text-muted-foreground mt-1">
                      {assessment.passingScore ? `Pass mark: ${assessment.passingScore} pts` : "No minimum passing score"}
                    </p>
                  </div>
                  <Award className="h-8 w-8 text-muted-foreground/40" />
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Invitation Code & Candidate Access Management Card */}
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
                  {assessment.invitationCode && assessment.invitationCodeEnabled ? (
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => revokeCodeMutation.mutate()}
                      disabled={revokeCodeMutation.isPending}
                      className="text-xs text-destructive hover:bg-destructive/10 border-destructive/30 gap-1.5"
                    >
                      {revokeCodeMutation.isPending ? (
                        <Loader2 className="size-3.5 animate-spin" />
                      ) : (
                        <Ban className="size-3.5" />
                      )}
                      <span>Revoke Code</span>
                    </Button>
                  ) : null}

                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => generateCodeMutation.mutate()}
                    disabled={generateCodeMutation.isPending}
                    className="text-xs gap-1.5"
                  >
                    {generateCodeMutation.isPending ? (
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
                      onClick={() => handleCopyCode(assessment.invitationCode!)}
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
                      onClick={() => handleCopyJoinLink(assessment.invitationCode!)}
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
                    onClick={() => generateCodeMutation.mutate()}
                    disabled={generateCodeMutation.isPending}
                    className="gap-1.5 mt-1"
                  >
                    {generateCodeMutation.isPending ? (
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

          {/* Main Tabs Navigation */}
          <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
            <TabsList className="bg-muted p-1">
              <TabsTrigger value="metrics" className="gap-2">
                <BarChart3 className="h-4 w-4" />
                Metrics & Analytics
              </TabsTrigger>
              <TabsTrigger value="attempts" className="gap-2">
                <FileQuestion className="h-4 w-4" />
                Monitoring & Attempts ({attemptsData?.totalElements ?? 0})
              </TabsTrigger>
              <TabsTrigger value="candidates" className="gap-2">
                <Users className="h-4 w-4" />
                Assigned Candidates ({assessment.assignedCandidateCount})
              </TabsTrigger>
              <TabsTrigger value="questions" className="gap-2">
                <Layers className="h-4 w-4" />
                Questions ({assessment.items?.length ?? 0})
              </TabsTrigger>
            </TabsList>

            {/* TAB 1: METRICS & ANALYTICS */}
            <TabsContent value="metrics" className="space-y-6">
              {loadingMetrics ? (
                <div className="flex items-center justify-center py-16 gap-2 text-muted-foreground">
                  <Loader2 className="h-5 w-5 animate-spin text-primary" />
                  <span>Aggregating assessment analytics & metrics...</span>
                </div>
              ) : metricsData ? (
                <div className="space-y-6">
                  {/* Performance KPIs */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                    <Card className="bg-card/60">
                      <CardContent className="pt-6">
                        <div className="flex items-center justify-between">
                          <div className="space-y-1">
                            <span className="text-xs text-muted-foreground font-medium">Completion Rate</span>
                            <div className="text-2xl font-bold font-mono">
                              {metricsData.completionRate.toFixed(1)}%
                            </div>
                            <span className="text-[11px] text-muted-foreground">
                              {metricsData.submittedCount} submitted / {metricsData.totalCandidatesAssigned} assigned
                            </span>
                          </div>
                          <Target className="size-8 text-primary/40" />
                        </div>
                      </CardContent>
                    </Card>

                    <Card className="bg-card/60">
                      <CardContent className="pt-6">
                        <div className="flex items-center justify-between">
                          <div className="space-y-1">
                            <span className="text-xs text-muted-foreground font-medium">Pass Rate</span>
                            <div className="text-2xl font-bold font-mono text-emerald-600 dark:text-emerald-400">
                              {metricsData.passRate.toFixed(1)}%
                            </div>
                            <span className="text-[11px] text-muted-foreground">
                              Pass threshold configured
                            </span>
                          </div>
                          <Award className="size-8 text-emerald-500/40" />
                        </div>
                      </CardContent>
                    </Card>

                    <Card className="bg-card/60">
                      <CardContent className="pt-6">
                        <div className="flex items-center justify-between">
                          <div className="space-y-1">
                            <span className="text-xs text-muted-foreground font-medium">Score Distribution</span>
                            <div className="text-2xl font-bold font-mono">
                              {metricsData.averageScore.toFixed(1)}{" "}
                              <span className="text-xs font-normal text-muted-foreground">
                                / {metricsData.maxPossibleScore}
                              </span>
                            </div>
                            <span className="text-[11px] text-muted-foreground">
                              Median: {metricsData.medianScore.toFixed(1)} pts
                            </span>
                          </div>
                          <TrendingUp className="size-8 text-sky-500/40" />
                        </div>
                      </CardContent>
                    </Card>

                    <Card className="bg-card/60">
                      <CardContent className="pt-6">
                        <div className="flex items-center justify-between">
                          <div className="space-y-1">
                            <span className="text-xs text-muted-foreground font-medium">Coding Accuracy</span>
                            <div className="text-2xl font-bold font-mono text-purple-600 dark:text-purple-400">
                              {metricsData.codingSuccessRate.toFixed(1)}%
                            </div>
                            <span className="text-[11px] text-muted-foreground">
                              {metricsData.totalCodeSubmissions} code submissions
                            </span>
                          </div>
                          <Code2 className="size-8 text-purple-500/40" />
                        </div>
                      </CardContent>
                    </Card>
                  </div>

                  {/* Secondary Metrics Strip */}
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                    <Card>
                      <CardHeader className="pb-2">
                        <CardDescription className="text-xs">Average Duration</CardDescription>
                        <CardTitle className="text-xl font-bold font-mono">
                          {metricsData.averageTimeSpentMinutes.toFixed(1)} minutes
                        </CardTitle>
                      </CardHeader>
                    </Card>

                    <Card>
                      <CardHeader className="pb-2">
                        <CardDescription className="text-xs">Candidate Pipeline</CardDescription>
                        <CardTitle className="text-sm font-semibold flex items-center gap-2">
                          <span className="text-amber-500">{metricsData.inProgressCount} in progress</span>
                          <span>•</span>
                          <span className="text-muted-foreground">{metricsData.invitedCount} invited</span>
                        </CardTitle>
                      </CardHeader>
                    </Card>

                    <Card>
                      <CardHeader className="pb-2">
                        <CardDescription className="text-xs">Total Integrity Flags</CardDescription>
                        <CardTitle className="text-xl font-bold flex items-center gap-2">
                          <span className={metricsData.totalIntegrityFlags > 0 ? "text-amber-500 font-mono" : "text-muted-foreground font-mono"}>
                            {metricsData.totalIntegrityFlags}
                          </span>
                          <span className="text-xs font-normal text-muted-foreground">
                            focus blur / reconnect events
                          </span>
                        </CardTitle>
                      </CardHeader>
                    </Card>
                  </div>

                  {/* Question-Wise Performance Breakdown Table */}
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-base flex items-center gap-2">
                        <Layers className="size-4 text-primary" />
                        <span>Question-by-Question Difficulty & Success Rates</span>
                      </CardTitle>
                      <CardDescription>
                        Detailed aggregate outcomes per question across all completed candidate attempts.
                      </CardDescription>
                    </CardHeader>
                    <CardContent>
                      <div className="rounded-md border overflow-hidden">
                        <Table>
                          <TableHeader>
                            <TableRow>
                              <TableHead className="w-12">#</TableHead>
                              <TableHead>Question Title</TableHead>
                              <TableHead>Type</TableHead>
                              <TableHead>Max Points</TableHead>
                              <TableHead>Answered</TableHead>
                              <TableHead>Correct</TableHead>
                              <TableHead>Average Score</TableHead>
                              <TableHead className="w-40 text-right">Success Rate</TableHead>
                            </TableRow>
                          </TableHeader>
                          <TableBody>
                            {metricsData.questionMetrics?.map((qm) => (
                              <TableRow key={qm.questionId}>
                                <TableCell className="font-mono text-xs">{qm.index + 1}</TableCell>
                                <TableCell className="font-medium text-xs">{qm.title}</TableCell>
                                <TableCell>
                                  <Badge variant="outline" className="text-[10px]">
                                    {qm.questionType}
                                  </Badge>
                                </TableCell>
                                <TableCell className="font-mono text-xs">{qm.maxPoints} pts</TableCell>
                                <TableCell className="text-xs">{qm.answeredCount}</TableCell>
                                <TableCell className="text-xs font-semibold text-emerald-600 dark:text-emerald-400">
                                  {qm.correctCount}
                                </TableCell>
                                <TableCell className="font-mono text-xs">
                                  {qm.averageScore.toFixed(1)} pts
                                </TableCell>
                                <TableCell className="text-right">
                                  <div className="flex items-center justify-end gap-2">
                                    <div className="w-20 bg-muted rounded-full h-2 overflow-hidden">
                                      <div
                                        className="h-full bg-primary rounded-full transition-all"
                                        style={{ width: `${Math.min(100, qm.successRate)}%` }}
                                      />
                                    </div>
                                    <span className="font-mono text-xs font-bold w-12 text-right">
                                      {qm.successRate.toFixed(1)}%
                                    </span>
                                  </div>
                                </TableCell>
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
                      </div>
                    </CardContent>
                  </Card>
                </div>
              ) : (
                <div className="text-center py-12 text-muted-foreground">
                  <BarChart3 className="size-8 mx-auto mb-2 opacity-50" />
                  <p className="text-sm">No metrics available yet.</p>
                </div>
              )}
            </TabsContent>

            {/* TAB 2: MONITORING & ATTEMPTS */}
            <TabsContent value="attempts" className="space-y-4">
              <div className="rounded-md border bg-card overflow-hidden">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Candidate</TableHead>
                      <TableHead>Attempt #</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Started At</TableHead>
                      <TableHead>Deadline / Completed</TableHead>
                      <TableHead>Score</TableHead>
                      <TableHead>Integrity Flags</TableHead>
                      <TableHead className="text-right">Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {loadingAttempts ? (
                      <TableRow>
                        <TableCell colSpan={8} className="h-32 text-center">
                          <div className="flex items-center justify-center gap-2 text-muted-foreground">
                            <Loader2 className="h-4 w-4 animate-spin text-primary" />
                            Loading candidate monitoring & attempts...
                          </div>
                        </TableCell>
                      </TableRow>
                    ) : attemptsData?.items?.length === 0 ? (
                      <TableRow>
                        <TableCell colSpan={8} className="h-32 text-center text-muted-foreground">
                          No candidate attempts have been initiated yet.
                        </TableCell>
                      </TableRow>
                    ) : (
                      attemptsData?.items?.map((att) => (
                        <TableRow key={att.id}>
                          <TableCell className="font-mono text-sm font-medium">
                            {att.candidateEmail}
                          </TableCell>
                          <TableCell>
                            <Badge variant="outline">#{att.attemptNumber}</Badge>
                          </TableCell>
                          <TableCell>{getAttemptBadge(att.status)}</TableCell>
                          <TableCell className="text-xs text-muted-foreground">
                            {new Date(att.startedAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                          </TableCell>
                          <TableCell className="text-xs text-muted-foreground">
                            {new Date(att.effectiveDeadline).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                          </TableCell>
                          <TableCell>
                            {att.score !== undefined && att.score !== null ? (
                              <div className="flex items-center gap-1.5">
                                <span className="font-semibold text-sm">
                                  {att.score}/{att.maxPossibleScore ?? assessment.totalScore}
                                </span>
                                {att.passed !== undefined && (
                                  <Badge
                                    className={cn(
                                      "text-[10px] px-1.5 py-0",
                                      att.passed
                                        ? "bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30"
                                        : "bg-destructive/15 text-destructive border-destructive/30"
                                    )}
                                  >
                                    {att.passed ? "PASSED" : "FAILED"}
                                  </Badge>
                                )}
                              </div>
                            ) : (
                              "—"
                            )}
                          </TableCell>
                          <TableCell>
                            {att.integrityFlagsCount > 0 ? (
                              <Badge variant="destructive" className="gap-1 text-xs">
                                <ShieldAlert className="h-3 w-3" />
                                {att.integrityFlagsCount} flag(s)
                              </Badge>
                            ) : (
                              <Badge variant="secondary" className="gap-1 text-xs text-muted-foreground">
                                <ShieldCheck className="h-3 w-3 text-emerald-500" />
                                Clean
                              </Badge>
                            )}
                          </TableCell>
                          <TableCell className="text-right">
                            <div className="flex items-center justify-end gap-1.5">
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={() => setInspectAttemptId(att.id)}
                                className="h-7 text-xs gap-1"
                                title="Inspect Candidate Submission"
                              >
                                <Eye className="size-3.5" />
                                <span>Inspect</span>
                              </Button>
                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => setAuditAttemptId(att.id)}
                                className="h-7 text-xs gap-1 text-muted-foreground hover:text-foreground"
                                title="View Comprehensive Audit Trail"
                              >
                                <Activity className="size-3.5" />
                                <span>Audit</span>
                              </Button>
                            </div>
                          </TableCell>
                        </TableRow>
                      ))
                    )}
                  </TableBody>
                </Table>
              </div>
            </TabsContent>

            {/* TAB 3: ASSIGNED CANDIDATES */}
            <TabsContent value="candidates" className="space-y-4">
              <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <div className="relative w-full sm:w-72">
                  <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                  <Input
                    type="search"
                    placeholder="Search candidate email..."
                    value={candidateSearch}
                    onChange={(e) => setCandidateSearch(e.target.value)}
                    className="pl-8 h-9"
                  />
                </div>
                {assessment.status === "COMPLETED" ? (
                  <Badge variant="outline" className="text-muted-foreground border-border gap-1.5 text-xs py-1.5 px-3">
                    <Lock className="size-3.5 text-muted-foreground" />
                    <span>Candidate Roster Closed (Assessment Ended)</span>
                  </Badge>
                ) : (
                  <Button size="sm" onClick={() => setBulkAssignOpen(true)} className="gap-1.5">
                    <Plus className="h-4 w-4" />
                    Assign Candidates
                  </Button>
                )}
              </div>

              <div className="rounded-md border bg-card overflow-hidden">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Candidate Email</TableHead>
                      <TableHead>Participation Status</TableHead>
                      <TableHead>Attempts Used</TableHead>
                      <TableHead>Invited At</TableHead>
                      <TableHead>Started At</TableHead>
                      <TableHead>Latest Score</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {loadingCandidates ? (
                      <TableRow>
                        <TableCell colSpan={6} className="h-32 text-center">
                          <div className="flex items-center justify-center gap-2 text-muted-foreground">
                            <Loader2 className="h-4 w-4 animate-spin text-primary" />
                            Loading candidate assignments...
                          </div>
                        </TableCell>
                      </TableRow>
                    ) : candidateData?.items?.length === 0 ? (
                      <TableRow>
                        <TableCell colSpan={6} className="h-32 text-center text-muted-foreground">
                          No candidates assigned yet.
                        </TableCell>
                      </TableRow>
                    ) : (
                      candidateData?.items?.map((c) => (
                        <TableRow key={c.id}>
                          <TableCell className="font-medium text-xs font-mono">{c.candidateEmail}</TableCell>
                          <TableCell>{getParticipationBadge(c.status)}</TableCell>
                          <TableCell>
                            <Badge variant="outline">{c.attemptsCount}</Badge>
                          </TableCell>
                          <TableCell className="text-xs text-muted-foreground">
                            {c.invitedAt ? new Date(c.invitedAt).toLocaleString() : "—"}
                          </TableCell>
                          <TableCell className="text-xs text-muted-foreground">
                            {c.startedAt ? new Date(c.startedAt).toLocaleString() : "Not started"}
                          </TableCell>
                          <TableCell className="font-semibold text-sm">
                            {c.latestScore !== undefined && c.latestScore !== null
                              ? `${c.latestScore} / ${assessment.totalScore}`
                              : "—"}
                          </TableCell>
                        </TableRow>
                      ))
                    )}
                  </TableBody>
                </Table>
              </div>
            </TabsContent>

            {/* TAB 4: QUESTIONS BANK & WEIGHTING */}
            <TabsContent value="questions" className="space-y-4">
              <div className="space-y-4">
                {assessment.items?.map((item, index) => (
                  <Card key={item.questionId} className="border bg-card">
                    <CardHeader className="pb-3">
                      <div className="flex items-start justify-between gap-4">
                        <div className="flex items-center gap-3">
                          <span className="flex h-7 w-7 items-center justify-center rounded-full bg-primary/10 text-xs font-bold text-primary">
                            {index + 1}
                          </span>
                          <div>
                            <CardTitle className="text-base flex items-center gap-2">
                              {item.title}
                              <Badge variant="outline" className="text-xs">
                                {item.points} pts
                              </Badge>
                            </CardTitle>
                            <CardDescription className="flex items-center gap-2 mt-1">
                              {item.questionType === "MCQ" ? (
                                <span className="inline-flex items-center gap-1 text-xs text-sky-600 dark:text-sky-400">
                                  <CheckSquare className="h-3 w-3" />
                                  MCQ ({item.mcqType?.replace("_", " ")})
                                </span>
                              ) : (
                                <span className="inline-flex items-center gap-1 text-xs text-purple-600 dark:text-purple-400">
                                  <Code2 className="h-3 w-3" />
                                  Coding Problem
                                </span>
                              )}
                            </CardDescription>
                          </div>
                        </div>
                      </div>
                    </CardHeader>
                    <CardContent className="space-y-3 pt-0">
                      {item.questionType === "MCQ" && (
                        <div className="space-y-2">
                          {item.mcqContent && (
                            <div className="text-sm text-foreground/90 prose dark:prose-invert max-w-none">
                              <MarkdownPreview content={item.mcqContent} />
                            </div>
                          )}
                          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-2">
                            {item.options?.map((opt) => {
                              const isCorrect = item.correctOptionIds?.includes(opt.id)
                              return (
                                <div
                                  key={opt.id}
                                  className={`flex items-center justify-between p-2.5 rounded-md border text-xs ${
                                    isCorrect
                                      ? "border-emerald-500/50 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300 font-medium"
                                      : "border-border/60 bg-muted/40 text-foreground"
                                  }`}
                                >
                                  <div className="prose dark:prose-invert max-w-none text-xs">
                                    <MarkdownPreview content={opt.content} />
                                  </div>
                                  {isCorrect && (
                                    <Badge className="bg-emerald-600 text-white text-[10px] px-1.5 py-0 h-4 ml-2 shrink-0">
                                      Correct
                                    </Badge>
                                  )}
                                </div>
                              )
                            })}
                          </div>
                        </div>
                      )}

                      {item.questionType === "CODING" && (
                        <div className="space-y-2 text-xs">
                          {item.codingStatement && (
                            <div className="text-sm text-muted-foreground line-clamp-3 prose dark:prose-invert max-w-none">
                              <MarkdownPreview content={item.codingStatement} />
                            </div>
                          )}
                          <div className="flex flex-wrap gap-2 pt-1">
                            <span className="text-muted-foreground font-medium">Supported Languages:</span>
                            {item.supportedLanguageIds?.map((lang) => (
                              <Badge key={lang} variant="secondary" className="text-[11px] font-mono">
                                {lang}
                              </Badge>
                            ))}
                          </div>
                          {item.sampleTestCases && item.sampleTestCases.length > 0 && (
                            <div className="pt-2 text-muted-foreground">
                              {item.sampleTestCases.length} sample test case(s) included.
                            </div>
                          )}
                        </div>
                      )}
                    </CardContent>
                  </Card>
                ))}
              </div>
            </TabsContent>
          </Tabs>
        </div>
      </main>

      {/* INSPECT CANDIDATE ATTEMPT MODAL */}
      <Dialog open={Boolean(inspectAttemptId)} onOpenChange={(open) => !open && setInspectAttemptId(null)}>
        <DialogContent className="sm:max-w-4xl max-h-[85vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Eye className="size-5 text-primary" />
              <span>Candidate Attempt Inspection</span>
            </DialogTitle>
            <DialogDescription>
              Review full candidate responses, code submissions, and execution test outcomes.
            </DialogDescription>
          </DialogHeader>

          {loadingDetail ? (
            <div className="flex items-center justify-center py-16 gap-2 text-muted-foreground">
              <Loader2 className="size-5 animate-spin text-primary" />
              <span>Loading attempt details...</span>
            </div>
          ) : attemptDetail ? (
            <div className="space-y-6 py-2">
              {/* Attempt Overview Header */}
              <div className="p-4 rounded-xl border bg-muted/20 grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
                <div>
                  <span className="text-muted-foreground">Attempt Number</span>
                  <p className="font-semibold text-foreground mt-0.5">Attempt #{attemptDetail.attemptNumber}</p>
                </div>
                <div>
                  <span className="text-muted-foreground">Status</span>
                  <p className="mt-0.5">{getAttemptBadge(attemptDetail.status)}</p>
                </div>
                <div>
                  <span className="text-muted-foreground">Score Awarded</span>
                  <p className="font-bold text-foreground text-sm mt-0.5">
                    {attemptDetail.score} / {attemptDetail.maxPossibleScore} pts ({attemptDetail.percentage.toFixed(1)}%)
                  </p>
                </div>
                <div>
                  <span className="text-muted-foreground">Completed</span>
                  <p className="font-mono text-muted-foreground mt-0.5">
                    {attemptDetail.answeredCount} / {attemptDetail.totalQuestions} answered
                  </p>
                </div>
              </div>

              {/* Question Submissions List */}
              <div className="space-y-4">
                <h3 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
                  Questions & Candidate Answers ({attemptDetail.questionResults?.length ?? 0})
                </h3>

                {attemptDetail.questionResults?.map((q) => {
                  return (
                    <div key={q.questionId} className="rounded-xl border p-4 bg-card/40 space-y-3 text-xs">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-foreground">Q#{q.questionIndex + 1}</span>
                          <span className="font-semibold text-foreground">{q.title}</span>
                          <Badge variant="outline" className="text-[10px]">{q.questionType}</Badge>
                        </div>
                        <span className="font-mono font-semibold">
                          {q.pointsAwarded} / {q.maxPoints} pts
                        </span>
                      </div>

                      {q.questionType === "MCQ" && (
                        <div className="space-y-2 pt-1">
                          {q.mcqStatement && (
                            <div className="prose dark:prose-invert max-w-none text-xs">
                              <MarkdownPreview content={q.mcqStatement} />
                            </div>
                          )}
                          <div className="grid grid-cols-1 gap-1.5 pt-1">
                            {q.options?.map((opt) => {
                              const isSelected = q.selectedOptionIds?.includes(opt.id)
                              return (
                                <div
                                  key={opt.id}
                                  className={cn(
                                    "p-2.5 rounded-lg border text-xs flex items-center justify-between",
                                    isSelected
                                      ? "border-primary bg-primary/10 text-foreground font-medium"
                                      : "border-border/60 bg-muted/20 text-muted-foreground"
                                  )}
                                >
                                  <span>{opt.content}</span>
                                  {isSelected && (
                                    <Badge variant="outline" className="text-[10px]">Candidate Choice</Badge>
                                  )}
                                </div>
                              )
                            })}
                          </div>
                        </div>
                      )}

                      {q.questionType === "CODING" && (
                        <div className="space-y-2 pt-1">
                          {q.codingStatement && (
                            <div className="prose dark:prose-invert max-w-none text-xs text-muted-foreground">
                              <MarkdownPreview content={q.codingStatement} />
                            </div>
                          )}
                          <div className="flex items-center justify-between pt-1">
                            <span className="text-muted-foreground font-medium">
                              Language: <strong className="text-foreground">{q.languageId || "none"}</strong>
                            </span>
                            {q.allTestcasesPassed !== undefined && (
                              <Badge
                                className={cn(
                                  "text-[10px]",
                                  q.allTestcasesPassed
                                    ? "bg-emerald-500/15 text-emerald-600 dark:text-emerald-400"
                                    : "bg-destructive/15 text-destructive"
                                )}
                              >
                                {q.allTestcasesPassed ? "ALL TEST CASES PASSED" : "FAILED TEST CASES"}
                              </Badge>
                            )}
                          </div>

                          <div className="h-44 rounded-lg overflow-hidden border">
                            <MonacoEditor
                              language={resolveMonacoLanguage(q.languageId)}
                              value={q.code || "// No code submitted"}
                              height="100%"
                              options={{
                                readOnly: true,
                                minimap: { enabled: false },
                                fontSize: 11,
                                lineNumbers: "on",
                                automaticLayout: true,
                              }}
                            />
                          </div>
                        </div>
                      )}
                    </div>
                  )
                })}
              </div>
            </div>
          ) : null}

          <DialogFooter>
            <Button variant="outline" onClick={() => setInspectAttemptId(null)}>
              Close
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* FULL AUDIT TRAIL TIMELINE MODAL */}
      <Dialog open={Boolean(auditAttemptId)} onOpenChange={(open) => !open && setAuditAttemptId(null)}>
        <DialogContent className="sm:max-w-3xl max-h-[85vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Activity className="size-5 text-primary" />
              <span>Candidate Assessment Audit Trail</span>
            </DialogTitle>
            <DialogDescription>
              Chronological ledger of verified server operations, client telemetry, and connection presence.
            </DialogDescription>
          </DialogHeader>

          {loadingAudit ? (
            <div className="flex items-center justify-center py-16 gap-2 text-muted-foreground">
              <Loader2 className="size-5 animate-spin text-primary" />
              <span>Loading audit events...</span>
            </div>
          ) : !auditTrailData || auditTrailData.length === 0 ? (
            <div className="text-center py-12 text-muted-foreground text-xs">
              No audit events recorded for this candidate attempt.
            </div>
          ) : (
            <div className="space-y-3 py-2">
              <div className="rounded-md border overflow-hidden">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Event</TableHead>
                      <TableHead>Source</TableHead>
                      <TableHead>Details / Safe Metadata</TableHead>
                      <TableHead className="text-right">Timestamp</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {auditTrailData.map((evt: AssessmentAuditEvent) => (
                      <TableRow key={evt.id} className="text-xs">
                        <TableCell className="font-semibold text-foreground">
                          {evt.eventType.replace(/_/g, " ")}
                        </TableCell>
                        <TableCell>
                          {getAuditSourceBadge(evt.source)}
                        </TableCell>
                        <TableCell className="font-mono text-[11px] text-muted-foreground max-w-xs truncate">
                          {evt.metadata ? JSON.stringify(evt.metadata) : "—"}
                        </TableCell>
                        <TableCell className="text-right font-mono text-muted-foreground">
                          {new Date(evt.timestamp).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" })}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            </div>
          )}

          <DialogFooter>
            <Button variant="outline" onClick={() => setAuditAttemptId(null)}>
              Close
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Bulk Assign Candidates Dialog */}
      <Dialog open={bulkAssignOpen} onOpenChange={setBulkAssignOpen}>
        <DialogContent className="sm:max-w-[550px]">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Users className="h-5 w-5 text-primary" />
              Assign Candidates to Assessment
            </DialogTitle>
            <DialogDescription>
              Paste or type candidate email addresses below. Separate them using commas, newlines, or spaces.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-2">
            <Textarea
              placeholder="alice@company.com, bob@domain.org&#10;carol@university.edu"
              className="min-h-[140px] font-mono text-sm resize-y"
              value={candidateEmailsRaw}
              onChange={(e) => setCandidateEmailsRaw(e.target.value)}
            />

            <div className="flex items-center justify-between text-xs text-muted-foreground">
              <span>
                {parsedEmails.length > 0 ? (
                  <span className="text-primary font-medium">
                    ✓ {parsedEmails.length} valid unique candidate email(s) detected
                  </span>
                ) : (
                  "No valid emails detected yet"
                )}
              </span>
              <span>Duplicates will be automatically skipped</span>
            </div>
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => {
                setCandidateEmailsRaw("")
                setBulkAssignOpen(false)
              }}
            >
              Cancel
            </Button>
            <Button
              onClick={handleBulkAssignSubmit}
              disabled={parsedEmails.length === 0 || bulkAssignMutation.isPending}
              className="gap-2"
            >
              {bulkAssignMutation.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
              Assign {parsedEmails.length > 0 ? `(${parsedEmails.length})` : ""}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Complete Assessment Confirmation Dialog */}
      <AlertDialog open={completeConfirmOpen} onOpenChange={setCompleteConfirmOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Complete Assessment?</AlertDialogTitle>
            <AlertDialogDescription>
              This will officially close this assessment immediately. New candidate attempts and registrations will be closed, and in-progress candidates will have their sessions finalized.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleCompleteAssessment}
              className="bg-purple-600 hover:bg-purple-700 text-white"
            >
              {completeMutation.isPending ? <Loader2 className="size-4 animate-spin mr-1" /> : null}
              Yes, Complete Assessment
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
