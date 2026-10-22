import { useState, useEffect } from "react"
import axios from "axios"
import { createFileRoute, useNavigate, useSearch, Link } from "@tanstack/react-router"
import {
  KeyRound,
  ArrowRight,
  Loader2,
  AlertCircle,
  ShieldCheck,
  GraduationCap,
} from "lucide-react"

import { Logo } from "@/components/logo"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { useJoinAssessmentByCode } from "@/hooks/use-assessments"
import { useIsAuthenticated, useIsInitialized, useUser } from "@/stores/auth-store"
import { initializeAuthSession } from "@/hooks/use-auth"
import { toast } from "@/components/ui/toast"

interface JoinSearchParams {
  code?: string
}

export const Route = createFileRoute("/assessments/join")({
  validateSearch: (search: Record<string, unknown>): JoinSearchParams => {
    return {
      code: typeof search.code === "string" ? search.code : undefined,
    }
  },
  component: AssessmentJoinPage,
})

export function AssessmentJoinPage() {
  const navigate = useNavigate()
  const search = useSearch({ from: "/assessments/join" })
  const user = useUser()
  const isAuthenticated = useIsAuthenticated()
  const isInitialized = useIsInitialized()

  const [code, setCode] = useState(search.code?.trim().toUpperCase() ?? "")
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  // Pre-fill code if present in URL
  useEffect(() => {
    if (search.code) {
      setCode(search.code.trim().toUpperCase())
    }
  }, [search.code])

  const joinMutation = useJoinAssessmentByCode()

  // Authentication Guard
  useEffect(() => {
    if (!isInitialized) {
      initializeAuthSession()
    }
  }, [isInitialized])

  const handleJoin = async (e?: React.FormEvent) => {
    if (e) e.preventDefault()
    setErrorMessage(null)

    const cleanCode = code.trim().toUpperCase()
    if (!cleanCode) {
      setErrorMessage("Please enter an invitation code.")
      return
    }

    if (!isAuthenticated) {
      toast.add({
        title: "Sign In Required",
        description: "Please sign in to join this assessment.",
        type: "error",
      })
      navigate({
        to: "/login",
      })
      return
    }

    try {
      const summary = await joinMutation.mutateAsync({ code: cleanCode })
      toast.add({
        title: "Assessment Unlocked",
        description: `Successfully joined ${summary.title}. Redirecting to preparation lobby...`,
        type: "success",
      })
      navigate({
        to: "/assessments/$assessmentId",
        params: { assessmentId: summary.assessmentId },
      })
    } catch (err: unknown) {
      let msg = "Invalid or expired invitation code. Please verify the code with your administrator."
      if (axios.isAxiosError(err)) {
        const data = err.response?.data as { message?: string } | undefined
        if (data?.message) msg = data.message
      } else if (err instanceof Error) {
        msg = err.message
      }
      setErrorMessage(msg)
    }
  }

  return (
    <div className="min-h-screen w-full bg-background flex flex-col justify-between">
      {/* Header */}
      <header className="border-b border-border bg-card/60 backdrop-blur-md px-6 py-4">
        <div className="mx-auto flex max-w-5xl items-center justify-between">
          <Link to="/dashboard" className="flex items-center gap-3">
            <Logo size={28} />
            <span className="text-base font-bold tracking-tight text-foreground">Stacked</span>
          </Link>
          <div className="flex items-center gap-2">
            {isAuthenticated ? (
              <Link to="/dashboard">
                <Button variant="ghost" size="sm" className="text-xs">
                  Dashboard
                </Button>
              </Link>
            ) : (
              <Link to="/login">
                <Button variant="outline" size="sm" className="text-xs">
                  Sign In
                </Button>
              </Link>
            )}
          </div>
        </div>
      </header>

      {/* Main Join Card */}
      <main className="flex-1 flex items-center justify-center p-4">
        <div className="w-full max-w-md space-y-6">
          <Card className="border-border/80 shadow-lg">
            <CardHeader className="text-center pb-4">
              <div className="mx-auto mb-3 flex size-12 items-center justify-center rounded-xl bg-primary/10 text-primary">
                <KeyRound className="size-6" />
              </div>
              <CardTitle className="text-2xl font-bold tracking-tight">Join Assessment</CardTitle>
              <CardDescription>
                Enter the invitation code provided by your organization or test administrator.
              </CardDescription>
            </CardHeader>

            <form onSubmit={handleJoin}>
              <CardContent className="space-y-4">
                {errorMessage && (
                  <div className="flex items-start gap-2.5 rounded-lg border border-destructive/30 bg-destructive/10 p-3 text-xs text-destructive">
                    <AlertCircle className="size-4 shrink-0 mt-0.5" />
                    <span>{errorMessage}</span>
                  </div>
                )}

                <div className="space-y-2">
                  <label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                    Invitation Code
                  </label>
                  <Input
                    type="text"
                    placeholder="e.g. STK-7X9B-4R2D"
                    value={code}
                    onChange={(e) => {
                      setCode(e.target.value.toUpperCase())
                      setErrorMessage(null)
                    }}
                    autoCapitalize="characters"
                    autoCorrect="off"
                    spellCheck={false}
                    className="font-mono text-center text-lg tracking-widest uppercase h-12"
                    maxLength={24}
                    disabled={joinMutation.isPending}
                    autoFocus
                  />
                  <p className="text-[11px] text-muted-foreground text-center">
                    Invitation codes are usually formatted like STK-XXXX-YYYY.
                  </p>
                </div>

                {isAuthenticated && user && (
                  <div className="rounded-lg border bg-muted/40 p-3 text-xs text-muted-foreground">
                    <div className="flex items-center gap-2 text-foreground font-medium mb-1">
                      <ShieldCheck className="size-4 text-emerald-500" />
                      <span>Authenticated Candidate</span>
                    </div>
                    <span>
                      Joining as <strong className="text-foreground">{user.email}</strong>. This assessment will be bound to your account.
                    </span>
                  </div>
                )}
              </CardContent>

              <CardFooter className="flex flex-col gap-3 pt-2">
                <Button
                  type="submit"
                  size="lg"
                  className="w-full gap-2 font-semibold"
                  disabled={joinMutation.isPending || !code.trim()}
                >
                  {joinMutation.isPending ? (
                    <>
                      <Loader2 className="size-4 animate-spin" />
                      <span>Verifying Code...</span>
                    </>
                  ) : (
                    <>
                      <span>Enter Assessment</span>
                      <ArrowRight className="size-4" />
                    </>
                  )}
                </Button>

                <div className="text-center">
                  <Link
                    to="/dashboard"
                    className="text-xs text-muted-foreground hover:text-foreground transition-colors"
                  >
                    Return to Dashboard
                  </Link>
                </div>
              </CardFooter>
            </form>
          </Card>

          {/* Quick FAQ / Security note */}
          <div className="rounded-lg border border-border/50 bg-card/40 p-4 text-xs text-muted-foreground space-y-1.5">
            <div className="font-semibold text-foreground flex items-center gap-1.5">
              <GraduationCap className="size-4 text-primary" />
              <span>Assessment Access Rules</span>
            </div>
            <p>
              • Joining with a valid code registers your participation without consuming attempt time.
            </p>
            <p>
              • Your timer will only begin when you explicitly click "Start Assessment" in the preparation lobby.
            </p>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="border-t border-border/60 py-4 text-center text-xs text-muted-foreground">
        Stacked Assessment Platform &bull; Secure Candidate Environment
      </footer>
    </div>
  )
}
