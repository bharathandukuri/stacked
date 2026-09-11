import { useEffect } from "react"
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import {
  GraduationCap,
  Users,
  CheckCircle2,
  Clock,
  ShieldCheck,
  Server,
  Database,
  Lock,
  Loader2,
} from "lucide-react"

import { AdminSidebar } from "@/components/admin/sidebar"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import {
  useAdmin,
  useIsAuthenticated,
  useIsInitialized,
} from "@/stores/auth-store"
import { initializeAuthSession } from "@/hooks/use-auth"

export const Route = createFileRoute("/admin/")({
  component: AdminDashboardPage,
})

export function AdminDashboardPage() {
  const navigate = useNavigate()
  const isAuthenticated = useIsAuthenticated()
  const isInitialized = useIsInitialized()
  const admin = useAdmin()

  // Session verification and initialization gate
  useEffect(() => {
    let isMounted = true

    async function verifySession() {
      if (!isInitialized) {
        const hasActiveSession = await initializeAuthSession()
        if (isMounted && !hasActiveSession) {
          await navigate({ to: "/admin/login" })
        }
      } else if (!isAuthenticated) {
        await navigate({ to: "/admin/login" })
      }
    }

    verifySession()

    return () => {
      isMounted = false
    }
  }, [isInitialized, isAuthenticated, navigate])

  // Display clean loading state while verifying the session
  if (!isInitialized) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-3 bg-background">
        <Loader2 className="size-8 animate-spin text-primary" />
        <span className="text-sm font-medium text-muted-foreground">
          Verifying administrator session...
        </span>
      </div>
    )
  }

  // If unauthenticated, redirect is in flight
  if (!isAuthenticated) {
    return null
  }

  return (
    <div className="flex min-h-screen bg-background text-foreground">
      {/* Admin Sidebar */}
      <AdminSidebar currentPath="/admin" />

      {/* Main Content Area */}
      <main className="flex-1 overflow-y-auto">
        {/* Top Header Bar */}
        <header className="sticky top-0 z-10 flex h-16 items-center justify-between border-b border-border bg-background/80 px-8 backdrop-blur-md">
          <div>
            <h1 className="text-lg font-semibold tracking-tight text-foreground">
              Dashboard Overview
            </h1>
            <p className="text-xs text-muted-foreground">
              Platform administration & active session status
            </p>
          </div>

          <div className="flex items-center gap-3">
            <Badge
              variant="outline"
              className="gap-1 border-emerald-500/40 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
            >
              <CheckCircle2 className="size-3" /> Live Production Mode
            </Badge>
          </div>
        </header>

        {/* Dashboard Body */}
        <div className="space-y-6 p-8">
          {/* Welcome Banner */}
          <div className="rounded-xl border border-border bg-linear-to-r from-primary/10 via-background to-background p-6 shadow-sm">
            <div className="flex flex-col gap-1">
              <span className="text-xs font-semibold tracking-wider text-primary uppercase">
                Administrator Portal
              </span>
              <h2 className="text-2xl font-bold tracking-tight text-foreground">
                Welcome back, {admin?.name || "Administrator"}!
              </h2>
              <p className="text-sm text-muted-foreground">
                Logged in as{" "}
                <span className="font-medium text-foreground">
                  {admin?.email}
                </span>{" "}
                with <span className="font-semibold text-primary">ADMIN</span>{" "}
                privileges.
              </p>
            </div>
          </div>

          {/* Metric Cards Grid */}
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <Card className="border-border">
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">
                  Active Assessments
                </CardTitle>
                <GraduationCap className="size-4 text-primary" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">12</div>
                <p className="text-xs text-muted-foreground">
                  +2 scheduled for this week
                </p>
              </CardContent>
            </Card>

            <Card className="border-border">
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">
                  Total Candidates
                </CardTitle>
                <Users className="size-4 text-blue-500" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">1,482</div>
                <p className="text-xs text-muted-foreground">
                  Across 8 test batches
                </p>
              </CardContent>
            </Card>

            <Card className="border-border">
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">
                  Avg. Completion Time
                </CardTitle>
                <Clock className="size-4 text-amber-500" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">42m 18s</div>
                <p className="text-xs text-muted-foreground">
                  94.2% completion rate
                </p>
              </CardContent>
            </Card>

            <Card className="border-border">
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">
                  Active Sessions
                </CardTitle>
                <ShieldCheck className="size-4 text-emerald-500" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">1 Active</div>
                <p className="text-xs text-muted-foreground">
                  Protected with RTR & Redis
                </p>
              </CardContent>
            </Card>
          </div>

          {/* Security & Infrastructure Status Panel */}
          <div className="grid gap-6 md:grid-cols-2">
            <Card className="border-border">
              <CardHeader>
                <div className="flex items-center gap-2">
                  <Lock className="size-4 text-primary" />
                  <CardTitle className="text-base">
                    Security & Session Hardening
                  </CardTitle>
                </div>
                <CardDescription>
                  Enterprise authentication protections enabled for admin
                  accounts
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="flex items-center justify-between rounded-lg border border-border/70 p-3 text-sm">
                  <div>
                    <span className="font-medium">
                      Refresh Token Rotation (RTR)
                    </span>
                    <p className="text-xs text-muted-foreground">
                      Each token refresh issues a new rotated token and
                      invalidates the previous
                    </p>
                  </div>
                  <Badge
                    variant="outline"
                    className="border-emerald-500/30 text-emerald-500"
                  >
                    Active
                  </Badge>
                </div>

                <div className="flex items-center justify-between rounded-lg border border-border/70 p-3 text-sm">
                  <div>
                    <span className="font-medium">HTTP-Only Secure Cookie</span>
                    <p className="text-xs text-muted-foreground">
                      Tokens isolated from client-side script access
                    </p>
                  </div>
                  <Badge
                    variant="outline"
                    className="border-emerald-500/30 text-emerald-500"
                  >
                    Configured
                  </Badge>
                </div>

                <div className="flex items-center justify-between rounded-lg border border-border/70 p-3 text-sm">
                  <div>
                    <span className="font-medium">
                      Timing Attack Mitigation
                    </span>
                    <p className="text-xs text-muted-foreground">
                      Constant-time dummy hash verification on invalid email
                      attempts
                    </p>
                  </div>
                  <Badge
                    variant="outline"
                    className="border-emerald-500/30 text-emerald-500"
                  >
                    Active
                  </Badge>
                </div>
              </CardContent>
            </Card>

            <Card className="border-border">
              <CardHeader>
                <div className="flex items-center gap-2">
                  <Server className="size-4 text-primary" />
                  <CardTitle className="text-base">
                    Backend Infrastructure
                  </CardTitle>
                </div>
                <CardDescription>
                  State of connected persistent stores and Spring Security layer
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="flex items-center justify-between rounded-lg border border-border/70 p-3 text-sm">
                  <div className="flex items-center gap-3">
                    <Database className="size-4 text-emerald-500" />
                    <div>
                      <span className="font-medium">MongoDB Database</span>
                      <p className="text-xs text-muted-foreground">
                        Document storage for admin users & audit metadata
                      </p>
                    </div>
                  </div>
                  <Badge
                    variant="outline"
                    className="border-emerald-500/30 text-emerald-500"
                  >
                    Connected
                  </Badge>
                </div>

                <div className="flex items-center justify-between rounded-lg border border-border/70 p-3 text-sm">
                  <div className="flex items-center gap-3">
                    <Server className="size-4 text-red-500" />
                    <div>
                      <span className="font-medium">Redis Session Cache</span>
                      <p className="text-xs text-muted-foreground">
                        Lettuce pooled connection for session storage & token
                        revocation
                      </p>
                    </div>
                  </div>
                  <Badge
                    variant="outline"
                    className="border-emerald-500/30 text-emerald-500"
                  >
                    Connected
                  </Badge>
                </div>

                <div className="flex items-center justify-between rounded-lg border border-border/70 p-3 text-sm">
                  <div className="flex items-center gap-3">
                    <ShieldCheck className="size-4 text-primary" />
                    <div>
                      <span className="font-medium">
                        Spring Security 7 + Java 25
                      </span>
                      <p className="text-xs text-muted-foreground">
                        Custom UserDetailsService & DaoAuthenticationProvider
                      </p>
                    </div>
                  </div>
                  <Badge
                    variant="outline"
                    className="border-emerald-500/30 text-emerald-500"
                  >
                    Enforced
                  </Badge>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </main>
    </div>
  )
}
