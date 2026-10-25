import { useState, useMemo } from "react"
import { createFileRoute } from "@tanstack/react-router"
import {
  Users,
  Layers,
  BarChart3,
  Radio,
} from "lucide-react"

import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs"
import { AdminPageLayout } from "@/components/admin/admin-page-layout"
import { PageLoadingState } from "@/components/common/page-loading-state"
import { PageErrorState } from "@/components/common/page-error-state"
import { ConfirmActionDialog } from "@/components/common/confirm-action-dialog"

import { AssessmentDetailHeader } from "@/components/admin/assessment-detail/assessment-detail-header"
import { AssessmentMetricsTab } from "@/components/admin/assessment-detail/tabs/assessment-metrics-tab"
import { AssessmentAttemptsTab } from "@/components/admin/assessment-detail/tabs/assessment-attempts-tab"
import { AssessmentCandidatesTab } from "@/components/admin/assessment-detail/tabs/assessment-candidates-tab"
import { AssessmentQuestionsTab } from "@/components/admin/assessment-detail/tabs/assessment-questions-tab"
import { AttemptInspectDialog } from "@/components/admin/assessment-detail/dialogs/attempt-inspect-dialog"
import { AttemptAuditDialog } from "@/components/admin/assessment-detail/dialogs/attempt-audit-dialog"
import { BulkAssignDialog } from "@/components/admin/assessment-detail/dialogs/bulk-assign-dialog"

import {
  useAssessment,
  useAssessmentAttempts,
  useCandidateAssignments,
  useBulkAssignCandidates,
  useActivateAssessment,
  useCompleteAssessment,
  useGenerateInvitationCode,
  useRevokeInvitationCode,
  useAssessmentMetrics,
  useAttemptDetailForAdmin,
  useAttemptAuditTrail,
} from "@/hooks/use-assessments"
import { toast } from "@/components/ui/toast"

export const Route = createFileRoute("/admin/assessments/$assessmentId/")({
  component: AssessmentDetailPage,
})

const EMAIL_REGEX = /[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}/g

export function AssessmentDetailPage() {
  const { assessmentId } = Route.useParams()

  const [activeTab, setActiveTab] = useState("monitor")
  const [candidatePage, setCandidatePage] = useState(0)
  const [candidatePageSize, setCandidatePageSize] = useState(50)
  const [candidateSearch, setCandidateSearch] = useState("")

  const [attemptPage, setAttemptPage] = useState(0)
  const [attemptPageSize, setAttemptPageSize] = useState(50)

  // Bulk Assign Dialog State
  const [bulkAssignOpen, setBulkAssignOpen] = useState(false)
  const [candidateEmailsRaw, setCandidateEmailsRaw] = useState("")

  // Detailed Modals
  const [inspectAttemptId, setInspectAttemptId] = useState<string | null>(null)
  const [auditAttemptId, setAuditAttemptId] = useState<string | null>(null)

  // Action Confirmation Dialogs
  const [activateConfirmOpen, setActivateConfirmOpen] = useState(false)
  const [completeConfirmOpen, setCompleteConfirmOpen] = useState(false)
  const [copiedCode, setCopiedCode] = useState(false)
  const [copiedLink, setCopiedLink] = useState(false)

  // Queries
  const {
    data: assessment,
    isLoading,
    isError,
    error,
    refetch: refetchAssessment,
  } = useAssessment(assessmentId)

  const {
    data: metricsData,
    isLoading: loadingMetrics,
    refetch: refetchMetrics,
  } = useAssessmentMetrics(assessmentId)

  const {
    data: candidateData,
    isLoading: loadingCandidates,
    refetch: refetchCandidates,
  } = useCandidateAssignments(assessmentId, candidatePage, candidatePageSize)

  const {
    data: attemptsData,
    isLoading: loadingAttempts,
    refetch: refetchAttempts,
  } = useAssessmentAttempts(assessmentId, attemptPage, attemptPageSize)

  // Modals Data
  const { data: attemptDetail, isLoading: loadingDetail } = useAttemptDetailForAdmin(
    inspectAttemptId || ""
  )
  const { data: auditTrailData, isLoading: loadingAudit } = useAttemptAuditTrail(
    auditAttemptId || ""
  )

  // Mutations
  const bulkAssignMutation = useBulkAssignCandidates(assessmentId)
  const activateMutation = useActivateAssessment()
  const completeMutation = useCompleteAssessment()
  const generateCodeMutation = useGenerateInvitationCode(assessmentId)
  const revokeCodeMutation = useRevokeInvitationCode(assessmentId)

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

  const handleActivateAssessment = async () => {
    await activateMutation.mutateAsync(assessmentId)
    setActivateConfirmOpen(false)
    refetchAssessment()
    refetchMetrics()
  }

  const handleCompleteAssessment = async () => {
    await completeMutation.mutateAsync(assessmentId)
    setCompleteConfirmOpen(false)
    refetchAssessment()
    refetchMetrics()
  }

  const handleRefreshAll = () => {
    refetchAssessment()
    refetchMetrics()
    refetchAttempts()
    refetchCandidates()
  }

  if (isLoading) {
    return <PageLoadingState message="Loading assessment details..." />
  }

  if (isError || !assessment) {
    return (
      <PageErrorState
        title="Assessment Not Found"
        message={
          error instanceof Error
            ? error.message
            : "The requested assessment could not be loaded."
        }
        returnTo="/admin/assessments"
        returnLabel="Back to Assessments"
        onRetry={() => refetchAssessment()}
      />
    )
  }

  // Filter candidates locally by search text if provided
  const candidateItems = candidateData?.items ?? []
  const filteredCandidates = candidateSearch.trim()
    ? candidateItems.filter((c) =>
        c.candidateEmail.toLowerCase().includes(candidateSearch.toLowerCase().trim())
      )
    : candidateItems

  return (
    <AdminPageLayout
      currentPath="/admin/assessments"
      title={assessment.title}
      breadcrumbs={[
        { label: "Admin", href: "/admin" },
        { label: "Assessments", href: "/admin/assessments" },
        { label: assessment.title },
      ]}
    >
      <div className="space-y-6">
        {/* Header & KPI Metrics */}
        <AssessmentDetailHeader
          assessment={assessment}
          onRefreshAll={handleRefreshAll}
          onOpenActivateConfirm={() => setActivateConfirmOpen(true)}
          onOpenCompleteConfirm={() => setCompleteConfirmOpen(true)}
          onRevokeInvitationCode={() => revokeCodeMutation.mutate()}
          isRevokingCode={revokeCodeMutation.isPending}
          onGenerateInvitationCode={() => generateCodeMutation.mutate()}
          isGeneratingCode={generateCodeMutation.isPending}
          onCopyCode={handleCopyCode}
          copiedCode={copiedCode}
          onCopyJoinLink={handleCopyJoinLink}
          copiedLink={copiedLink}
        />

        {/* Tabbed Content Navigation */}
        <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
          <TabsList className="bg-muted p-1">
            <TabsTrigger value="monitor" className="gap-2 font-semibold">
              <Radio className="h-4 w-4 text-emerald-500 animate-pulse" />
              <span>Live Monitor & Attempts ({attemptsData?.totalElements ?? 0})</span>
            </TabsTrigger>
            <TabsTrigger value="metrics" className="gap-2">
              <BarChart3 className="h-4 w-4" />
              <span>Performance Analytics</span>
            </TabsTrigger>
            <TabsTrigger value="candidates" className="gap-2">
              <Users className="h-4 w-4" />
              <span>Assigned Candidates ({assessment.assignedCandidateCount})</span>
            </TabsTrigger>
            <TabsTrigger value="questions" className="gap-2">
              <Layers className="h-4 w-4" />
              <span>Questions & Items ({assessment.items?.length ?? 0})</span>
            </TabsTrigger>
          </TabsList>

          {/* TAB 1: LIVE MONITOR & ATTEMPTS */}
          <TabsContent value="monitor" className="space-y-4">
            <AssessmentAttemptsTab
              attempts={attemptsData?.items ?? []}
              loadingAttempts={loadingAttempts}
              totalScore={assessment.totalScore}
              page={attemptPage}
              totalPages={attemptsData?.totalPages ?? 1}
              totalElements={attemptsData?.totalElements ?? 0}
              pageSize={attemptPageSize}
              onPageChange={setAttemptPage}
              onPageSizeChange={setAttemptPageSize}
              onInspectAttempt={setInspectAttemptId}
              onAuditAttempt={setAuditAttemptId}
            />
          </TabsContent>

          {/* TAB 2: METRICS & ANALYTICS */}
          <TabsContent value="metrics" className="space-y-6">
            <AssessmentMetricsTab
              metricsData={metricsData}
              loadingMetrics={loadingMetrics}
            />
          </TabsContent>

          {/* TAB 3: ASSIGNED CANDIDATES */}
          <TabsContent value="candidates" className="space-y-4">
            <AssessmentCandidatesTab
              candidates={filteredCandidates}
              loadingCandidates={loadingCandidates}
              totalScore={assessment.totalScore}
              assessmentStatus={assessment.status}
              manuallyActivated={assessment.manuallyActivated}
              search={candidateSearch}
              onSearchChange={setCandidateSearch}
              page={candidatePage}
              totalPages={candidateData?.totalPages ?? 1}
              totalElements={candidateData?.totalElements ?? 0}
              pageSize={candidatePageSize}
              onPageChange={setCandidatePage}
              onPageSizeChange={setCandidatePageSize}
              onOpenBulkAssign={() => setBulkAssignOpen(true)}
            />
          </TabsContent>

          {/* TAB 4: QUESTIONS BANK & WEIGHTING */}
          <TabsContent value="questions" className="space-y-4">
            <AssessmentQuestionsTab items={assessment.items} />
          </TabsContent>
        </Tabs>
      </div>

      {/* Inspect Modal */}
      <AttemptInspectDialog
        attemptId={inspectAttemptId}
        onClose={() => setInspectAttemptId(null)}
        attemptDetail={attemptDetail}
        loadingDetail={loadingDetail}
      />

      {/* Full Audit Trail Modal */}
      <AttemptAuditDialog
        attemptId={auditAttemptId}
        onClose={() => setAuditAttemptId(null)}
        auditTrailData={auditTrailData}
        loadingAudit={loadingAudit}
      />

      {/* Bulk Assign Candidates Dialog */}
      <BulkAssignDialog
        open={bulkAssignOpen}
        onOpenChange={setBulkAssignOpen}
        rawEmails={candidateEmailsRaw}
        onRawEmailsChange={setCandidateEmailsRaw}
        parsedEmails={parsedEmails}
        onSubmit={handleBulkAssignSubmit}
        isPending={bulkAssignMutation.isPending}
      />

      {/* Activate Assessment Confirmation Dialog */}
      <ConfirmActionDialog
        open={activateConfirmOpen}
        onOpenChange={setActivateConfirmOpen}
        title="Activate Assessment?"
        description="This will manually activate this assessment immediately, overriding any scheduled start time. The candidate roster will be frozen to prevent new candidate enrollments, and candidates will be able to begin taking the assessment."
        confirmLabel="Yes, Activate Assessment"
        onConfirm={handleActivateAssessment}
        isPending={activateMutation.isPending}
      />

      {/* Complete Assessment Confirmation Dialog */}
      <ConfirmActionDialog
        open={completeConfirmOpen}
        onOpenChange={setCompleteConfirmOpen}
        title="Complete Assessment?"
        description="This will officially close this assessment immediately. New candidate attempts and registrations will be closed, and in-progress candidates will have their sessions finalized."
        confirmLabel="Yes, Complete Assessment"
        onConfirm={handleCompleteAssessment}
        isPending={completeMutation.isPending}
        destructive
      />
    </AdminPageLayout>
  )
}
