import { Search, Plus, Lock } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { TablePagination } from "@/components/common/table-pagination"
import { TableEmptyState } from "@/components/common/table-empty-state"
import { PageLoadingState } from "@/components/common/page-loading-state"
import type {
  CandidateAssignmentReport,
  CandidateParticipationStatus,
  AssessmentStatus,
} from "@/types/assessment"

interface AssessmentCandidatesTabProps {
  candidates: CandidateAssignmentReport[]
  loadingCandidates: boolean
  totalScore: number
  assessmentStatus: AssessmentStatus
  manuallyActivated?: boolean
  search: string
  onSearchChange: (val: string) => void
  page: number
  totalPages: number
  totalElements: number
  pageSize: number
  onPageChange: (newPage: number) => void
  onPageSizeChange: (newSize: number) => void
  onOpenBulkAssign: () => void
}

export function AssessmentCandidatesTab({
  candidates,
  loadingCandidates,
  totalScore,
  assessmentStatus,
  manuallyActivated,
  search,
  onSearchChange,
  page,
  totalPages,
  totalElements,
  pageSize,
  onPageChange,
  onPageSizeChange,
  onOpenBulkAssign,
}: AssessmentCandidatesTabProps) {
  const isRosterFrozen =
    assessmentStatus !== "NOT_STARTED" || Boolean(manuallyActivated)

  const getParticipationBadge = (status: CandidateParticipationStatus) => {
    switch (status) {
      case "INVITED":
        return <Badge variant="secondary">Invited</Badge>
      case "IN_PROGRESS":
        return (
          <Badge className="bg-amber-500/15 text-amber-600 dark:text-amber-400 border-amber-500/30 animate-pulse">
            In Progress
          </Badge>
        )
      case "SUBMITTED":
        return (
          <Badge className="bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30">
            Submitted
          </Badge>
        )
      case "EXPIRED":
        return <Badge variant="destructive">Expired</Badge>
      case "CANCELLED":
        return (
          <Badge variant="outline" className="text-muted-foreground">
            Cancelled
          </Badge>
        )
      default:
        return <Badge variant="outline">{status}</Badge>
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="relative w-full sm:w-72">
          <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            type="search"
            placeholder="Search candidate email..."
            value={search}
            onChange={(e) => onSearchChange(e.target.value)}
            className="pl-8 h-9"
          />
        </div>

        {isRosterFrozen ? (
          <Badge
            variant="outline"
            className="text-muted-foreground border-border gap-1.5 text-xs py-1.5 px-3"
          >
            <Lock className="size-3.5 text-muted-foreground" />
            <span>
              Candidate Roster Frozen (
              {assessmentStatus === "COMPLETED"
                ? "Assessment Ended"
                : "Assessment Active"}
              )
            </span>
          </Badge>
        ) : (
          <Button size="sm" onClick={onOpenBulkAssign} className="gap-1.5">
            <Plus className="h-4 w-4" />
            <span>Assign Candidates</span>
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
                  <PageLoadingState message="Loading candidate assignments..." />
                </TableCell>
              </TableRow>
            ) : candidates.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} className="h-32 p-0">
                  <TableEmptyState
                    title="No Candidates Found"
                    description={
                      search
                        ? "No candidates matched your search query."
                        : "No candidates assigned to this assessment yet."
                    }
                  />
                </TableCell>
              </TableRow>
            ) : (
              candidates.map((c) => (
                <TableRow key={c.id}>
                  <TableCell className="font-medium text-xs font-mono">
                    {c.candidateEmail}
                  </TableCell>
                  <TableCell>{getParticipationBadge(c.status)}</TableCell>
                  <TableCell>
                    <Badge variant="outline">{c.attemptsCount}</Badge>
                  </TableCell>
                  <TableCell className="text-xs text-muted-foreground">
                    {c.invitedAt ? new Date(c.invitedAt).toLocaleString() : "—"}
                  </TableCell>
                  <TableCell className="text-xs text-muted-foreground">
                    {c.startedAt
                      ? new Date(c.startedAt).toLocaleString()
                      : "Not started"}
                  </TableCell>
                  <TableCell className="font-semibold text-sm">
                    {c.latestScore !== undefined && c.latestScore !== null
                      ? `${c.latestScore} / ${totalScore}`
                      : "—"}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {totalPages > 1 && (
        <TablePagination
          page={page}
          totalPages={totalPages}
          totalElements={totalElements}
          pageSize={pageSize}
          onPageChange={onPageChange}
          onPageSizeChange={onPageSizeChange}
        />
      )}
    </div>
  )
}
