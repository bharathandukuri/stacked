import { Send, KeyRound } from "lucide-react"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Switch } from "@/components/ui/switch"

interface StepCandidateInvitationsProps {
  candidateEmailsRaw: string
  onCandidateEmailsRawChange: (val: string) => void
  parsedCandidateEmails: string[]
  enableInvitationCode: boolean
  onEnableInvitationCodeChange: (val: boolean) => void
  invitationCodeExpiresAt: string
  onInvitationCodeExpiresAtChange: (val: string) => void
  invitationCodeMaxUses: number | undefined
  onInvitationCodeMaxUsesChange: (val: number | undefined) => void
}

export function StepCandidateInvitations({
  candidateEmailsRaw,
  onCandidateEmailsRawChange,
  parsedCandidateEmails,
  enableInvitationCode,
  onEnableInvitationCodeChange,
  invitationCodeExpiresAt,
  onInvitationCodeExpiresAtChange,
  invitationCodeMaxUses,
  onInvitationCodeMaxUsesChange,
}: StepCandidateInvitationsProps) {
  return (
    <div className="space-y-6">
      {/* Method A: Email Invitations */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            <Send className="size-5 text-primary" />
            Direct Candidate Assignments
          </CardTitle>
          <CardDescription>
            Candidates with these emails will automatically find this assessment on their dashboard upon login.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <div className="flex justify-between text-xs font-semibold">
              <span>Candidate Email Addresses</span>
              <span className="text-primary font-bold">
                {parsedCandidateEmails.length} Recognized Email(s)
              </span>
            </div>
            <Textarea
              placeholder="alice@example.com, bob@company.org&#10;carol@university.edu"
              rows={5}
              value={candidateEmailsRaw}
              onChange={(e) => onCandidateEmailsRawChange(e.target.value)}
            />
            <p className="text-xs text-muted-foreground">
              Accepts comma-separated, space-separated, or newline-separated email addresses.
            </p>
          </div>

          {parsedCandidateEmails.length > 0 && (
            <div className="p-3 bg-muted/40 rounded-lg border">
              <span className="text-xs font-semibold text-foreground block mb-2">
                Recognized Candidate List ({parsedCandidateEmails.length}):
              </span>
              <div className="flex flex-wrap gap-1.5 max-h-36 overflow-y-auto">
                {parsedCandidateEmails.map((email) => (
                  <Badge key={email} variant="secondary" className="text-xs font-mono">
                    {email}
                  </Badge>
                ))}
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Method B: Shareable Invitation Code */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="text-lg flex items-center gap-2">
                <KeyRound className="size-5 text-primary" />
                Shareable Join Passcode
              </CardTitle>
              <CardDescription>
                Generate a secure passcode allowing candidates to self-enroll and start the test.
              </CardDescription>
            </div>
            <Switch
              checked={enableInvitationCode}
              onCheckedChange={onEnableInvitationCodeChange}
            />
          </div>
        </CardHeader>

        {enableInvitationCode && (
          <CardContent className="space-y-4 pt-2">
            <div className="p-3 rounded-lg border border-primary/20 bg-primary/5 text-xs text-muted-foreground">
              A cryptographically secure unique passcode (e.g.{" "}
              <span className="font-mono font-semibold text-primary">STK-XXXX-YYYY</span>) will be automatically generated and bound to this assessment.
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-2">
                <label className="text-sm font-semibold text-foreground">
                  Passcode Expiration (Optional)
                </label>
                <Input
                  type="datetime-local"
                  value={invitationCodeExpiresAt}
                  onChange={(e) => onInvitationCodeExpiresAtChange(e.target.value)}
                />
                <p className="text-xs text-muted-foreground">
                  After this time, no new candidate can join with this code.
                </p>
              </div>

              <div className="space-y-2">
                <label className="text-sm font-semibold text-foreground">
                  Max Uses Limit (Optional)
                </label>
                <Input
                  type="number"
                  min={1}
                  placeholder="Unlimited if empty"
                  value={invitationCodeMaxUses ?? ""}
                  onChange={(e) =>
                    onInvitationCodeMaxUsesChange(
                      e.target.value ? Number(e.target.value) : undefined
                    )
                  }
                />
                <p className="text-xs text-muted-foreground">
                  Maximum number of candidates allowed to join with this code.
                </p>
              </div>
            </div>
          </CardContent>
        )}
      </Card>
    </div>
  )
}

