import { Activity, ShieldCheck, Eye, Radio, Loader2 } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import type { AssessmentAuditEvent } from "@/types/assessment"

interface AttemptAuditDialogProps {
  attemptId: string | null
  onClose: () => void
  auditTrailData: AssessmentAuditEvent[] | undefined
  loadingAudit: boolean
}

export function AttemptAuditDialog({
  attemptId,
  onClose,
  auditTrailData,
  loadingAudit,
}: AttemptAuditDialogProps) {
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
        return (
          <Badge variant="outline" className="text-[10px]">
            {source}
          </Badge>
        )
    }
  }

  return (
    <Dialog open={Boolean(attemptId)} onOpenChange={(open) => !open && onClose()}>
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
                      <TableCell>{getAuditSourceBadge(evt.source)}</TableCell>
                      <TableCell className="font-mono text-[11px] text-muted-foreground max-w-xs truncate">
                        {evt.metadata ? JSON.stringify(evt.metadata) : "—"}
                      </TableCell>
                      <TableCell className="text-right font-mono text-muted-foreground">
                        {new Date(evt.timestamp).toLocaleTimeString([], {
                          hour: "2-digit",
                          minute: "2-digit",
                          second: "2-digit",
                        })}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          </div>
        )}

        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            Close
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

