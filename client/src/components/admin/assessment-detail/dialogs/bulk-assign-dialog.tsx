import { Users, Loader2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"

interface BulkAssignDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  rawEmails: string
  onRawEmailsChange: (val: string) => void
  parsedEmails: string[]
  onSubmit: () => void
  isPending: boolean
}

export function BulkAssignDialog({
  open,
  onOpenChange,
  rawEmails,
  onRawEmailsChange,
  parsedEmails,
  onSubmit,
  isPending,
}: BulkAssignDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[550px]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Users className="h-5 w-5 text-primary" />
            <span>Assign Candidates to Assessment</span>
          </DialogTitle>
          <DialogDescription>
            Paste or type candidate email addresses below. Separate them using commas, newlines, or spaces.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-2">
          <Textarea
            placeholder="alice@company.com, bob@domain.org&#10;carol@university.edu"
            className="min-h-[140px] font-mono text-sm resize-y"
            value={rawEmails}
            onChange={(e) => onRawEmailsChange(e.target.value)}
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
              onRawEmailsChange("")
              onOpenChange(false)
            }}
          >
            Cancel
          </Button>
          <Button
            onClick={onSubmit}
            disabled={parsedEmails.length === 0 || isPending}
            className="gap-2"
          >
            {isPending && <Loader2 className="h-4 w-4 animate-spin" />}
            <span>
              Assign {parsedEmails.length > 0 ? `(${parsedEmails.length})` : ""}
            </span>
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

