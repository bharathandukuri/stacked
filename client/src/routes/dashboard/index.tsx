import { useState, useEffect } from "react"
import { createFileRoute, useNavigate, Link } from "@tanstack/react-router"
import {
  Shield,
  LogOut,
  Sparkles,
  Award,
  Terminal,
  Clock,
  Database,
  ArrowRight,
} from "lucide-react"

import { Logo } from "@/components/logo"
import { Button, buttonVariants } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Input } from "@/components/ui/input"
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { useUser, useIsAdmin, useIsAuthenticated, useIsInitialized } from "@/stores/auth-store"
import { useLogout, initializeAuthSession } from "@/hooks/use-auth"
import { toast } from "@/components/ui/toast"

export const Route = createFileRoute("/dashboard/")({
  component: DashboardPage,
})

export function DashboardPage() {
  const user = useUser()
  const isAdmin = useIsAdmin()
  const isAuthenticated = useIsAuthenticated()
  const isInitialized = useIsInitialized()
  const navigate = useNavigate()
  const logoutMutation = useLogout()

  const [activeTab, setActiveTab] = useState<"assessments" | "practice">("assessments")
  const [inviteCode, setInviteCode] = useState("")

  // Authentication Guard
  useEffect(() => {
    if (!isInitialized) {
      initializeAuthSession()
    } else if (!isAuthenticated) {
      navigate({ to: "/login", replace: true })
    }
  }, [isInitialized, isAuthenticated, navigate])

  const handleLogout = async () => {
    try {
      await logoutMutation.mutateAsync()
      toast.add({
        title: "Logged Out",
        description: "You have been signed out successfully.",
        type: "success",
      })
      navigate({ to: "/login", replace: true })
    } catch {
      navigate({ to: "/login", replace: true })
    }
  }

  const handleJoinAssessment = (e: React.FormEvent) => {
    e.preventDefault()
    if (!inviteCode.trim()) {
      toast.add({
        title: "Invite Code Required",
        description: "Please enter a valid assessment code or passcode.",
        type: "error",
      })
      return
    }

    toast.add({
      title: "Assessment Module",
      description: `Joining assessment with code: "${inviteCode}". Assessment test runner will launch as part of the Assessments module.`,
      type: "info",
    })
    setInviteCode("")
  }

  if (!isInitialized || !isAuthenticated || !user) {
    return (
      <div className="flex h-screen w-full items-center justify-center bg-background">
        <div className="flex flex-col items-center gap-3 text-muted-foreground">
          <div className="size-8 animate-spin rounded-full border-2 border-primary border-t-transparent" />
          <p className="text-sm font-medium">Loading your dashboard...</p>
        </div>
      </div>
    )
  }

  const userInitial = user.name ? user.name[0].toUpperCase() : "U"

  return (
    <div className="min-h-screen w-full bg-background font-sans text-foreground flex flex-col">
      {/* Top Navbar */}
      <header className="sticky top-0 z-40 border-b border-border bg-card/80 backdrop-blur-md">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
          {/* Logo & Platform Name */}
          <div className="flex items-center gap-6">
            <Link to="/dashboard" className="flex items-center gap-3 transition-opacity hover:opacity-90">
              <Logo size={32} />
              <div className="flex flex-col">
                <span className="text-base font-bold tracking-tight text-foreground">
                  Stacked
                </span>
                <span className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
                  Candidate Portal
                </span>
              </div>
            </Link>

            {/* Navigation tabs */}
            <nav className="flex items-center gap-1 border-l border-border pl-4 sm:pl-6">
              <Button
                variant={activeTab === "assessments" ? "secondary" : "ghost"}
                size="sm"
                onClick={() => setActiveTab("assessments")}
                className="gap-2 text-xs font-medium"
              >
                <Award className="size-3.5 text-primary" />
                <span>Assessments</span>
              </Button>
              <Button
                variant={activeTab === "practice" ? "secondary" : "ghost"}
                size="sm"
                onClick={() => setActiveTab("practice")}
                className="gap-2 text-xs font-medium"
              >
                <Terminal className="size-3.5 text-emerald-500" />
                <span>Practice Arena</span>
                <Badge variant="outline" className="text-[9px] px-1 py-0 border-muted-foreground/30 text-muted-foreground">
                  Soon
                </Badge>
              </Button>
            </nav>
          </div>

          {/* Right Action Bar */}
          <div className="flex items-center gap-3">
            {/* If user is an ADMIN, prominently display Admin Portal button */}
            {isAdmin && (
              <Link
                to="/admin"
                className={buttonVariants({
                  variant: "outline",
                  size: "sm",
                  className: "gap-1.5 border-primary/40 bg-primary/10 text-primary hover:bg-primary/20 text-xs font-semibold shadow-xs",
                })}
              >
                <Shield className="size-3.5 text-primary" />
                <span>Admin Portal</span>
              </Link>
            )}

            {/* User Profile Dropdown Menu */}
            <DropdownMenu>
              <DropdownMenuTrigger
                render={
                  <Button
                    variant="ghost"
                    className="flex items-center gap-2.5 rounded-full p-1 pl-2 hover:bg-muted"
                  >
                    <span className="hidden sm:inline-block text-xs font-medium text-foreground max-w-[120px] truncate">
                      {user.name}
                    </span>
                    <Avatar className="size-8 border border-border">
                      <AvatarFallback className="bg-primary/10 font-bold text-xs text-primary">
                        {userInitial}
                      </AvatarFallback>
                    </Avatar>
                  </Button>
                }
              />
              <DropdownMenuContent align="end" className="w-56 p-1">
                <DropdownMenuLabel className="font-normal px-2 py-1.5">
                  <div className="flex flex-col space-y-1">
                    <p className="text-sm font-semibold leading-none text-foreground truncate">
                      {user.name}
                    </p>
                    <p className="text-xs leading-none text-muted-foreground truncate">
                      {user.email}
                    </p>
                    <div className="pt-1">
                      <Badge
                        variant={isAdmin ? "default" : "secondary"}
                        className="text-[10px] px-1.5 py-0"
                      >
                        {user.role}
                      </Badge>
                    </div>
                  </div>
                </DropdownMenuLabel>
                <DropdownMenuSeparator />

                {isAdmin && (
                  <>
                    <DropdownMenuItem
                      onClick={() => navigate({ to: "/admin" })}
                      className="gap-2 cursor-pointer text-xs"
                    >
                      <Shield className="size-3.5 text-primary" />
                      <span>Admin Portal</span>
                    </DropdownMenuItem>
                    <DropdownMenuSeparator />
                  </>
                )}

                <DropdownMenuItem
                  onClick={handleLogout}
                  className="gap-2 cursor-pointer text-xs text-destructive focus:text-destructive"
                >
                  <LogOut className="size-3.5" />
                  <span>Sign Out</span>
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
      </header>

      {/* Main Container */}
      <main className="flex-1 mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8 space-y-6">
        {/* Tab 1: Assessments Section */}
        {activeTab === "assessments" && (
          <div className="space-y-6">
            {/* Join with Invite Code Bar */}
            <Card className="border-border/80 shadow-xs">
              <CardContent className="p-6">
                <form
                  onSubmit={handleJoinAssessment}
                  className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4"
                >
                  <div className="space-y-1">
                    <h3 className="text-base font-semibold text-foreground">
                      Have an Assessment Invitation Code?
                    </h3>
                    <p className="text-xs text-muted-foreground">
                      Enter the passcode provided by your recruiter or test administrator to begin your session.
                    </p>
                  </div>
                  <div className="flex items-center gap-2 sm:max-w-md w-full">
                    <Input
                      placeholder="e.g. STACK-9482-ENG"
                      value={inviteCode}
                      onChange={(e) => setInviteCode(e.target.value)}
                      className="h-10 text-xs uppercase font-mono"
                    />
                    <Button type="submit" size="sm" className="h-10 px-4 shrink-0 font-medium">
                      <span>Enter Assessment</span>
                      <ArrowRight className="size-3.5 ml-1.5" />
                    </Button>
                  </div>
                </form>
              </CardContent>
            </Card>

            {/* Assessment Cards Header */}
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-lg font-bold text-foreground">
                  Available Assessments
                </h2>
                <p className="text-xs text-muted-foreground">
                  Standardized technical assessments scheduled for candidate benchmarking.
                </p>
              </div>
              <Badge variant="outline" className="text-xs">
                3 Assessments Ready
              </Badge>
            </div>

            {/* Assessment Cards Grid */}
            <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
              {/* Assessment 1 */}
              <Card className="flex flex-col border-border/80 transition-all hover:border-primary/40 hover:shadow-md">
                <CardHeader className="space-y-3 pb-4">
                  <div className="flex items-center justify-between">
                    <Badge variant="secondary" className="gap-1 text-[10px] bg-primary/10 text-primary border-primary/20">
                      <Sparkles className="size-3" />
                      <span>Full Stack</span>
                    </Badge>
                    <div className="flex items-center gap-1 text-xs text-muted-foreground">
                      <Clock className="size-3.5" />
                      <span>90 Mins</span>
                    </div>
                  </div>
                  <div>
                    <CardTitle className="text-base font-bold text-foreground">
                      Software Engineering Benchmark
                    </CardTitle>
                    <CardDescription className="text-xs mt-1">
                      Data structures, algorithms, Spring Boot patterns, and database operations.
                    </CardDescription>
                  </div>
                </CardHeader>
                <CardContent className="flex-1 space-y-3 text-xs text-muted-foreground">
                  <div className="space-y-2 border-t border-border pt-3">
                    <div className="flex items-center justify-between">
                      <span>Coding Questions:</span>
                      <span className="font-semibold text-foreground">4 Problems</span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span>Execution Environment:</span>
                      <span className="font-semibold text-foreground">Docker Sandboxed</span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span>Allowed Languages:</span>
                      <span className="font-semibold text-foreground">Java, C++, Python</span>
                    </div>
                  </div>
                </CardContent>
                <CardFooter className="border-t border-border/60 pt-4">
                  <Button
                    onClick={() =>
                      toast.add({
                        title: "Assessment Initialization",
                        description: "Assessment launcher is being implemented next in the Assessments module.",
                        type: "info",
                      })
                    }
                    className="w-full gap-2 text-xs font-semibold"
                  >
                    <span>Begin Assessment</span>
                    <ArrowRight className="size-3.5" />
                  </Button>
                </CardFooter>
              </Card>

              {/* Assessment 2 */}
              <Card className="flex flex-col border-border/80 transition-all hover:border-primary/40 hover:shadow-md">
                <CardHeader className="space-y-3 pb-4">
                  <div className="flex items-center justify-between">
                    <Badge variant="secondary" className="gap-1 text-[10px] bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20">
                      <Terminal className="size-3" />
                      <span>Algorithms</span>
                    </Badge>
                    <div className="flex items-center gap-1 text-xs text-muted-foreground">
                      <Clock className="size-3.5" />
                      <span>60 Mins</span>
                    </div>
                  </div>
                  <div>
                    <CardTitle className="text-base font-bold text-foreground">
                      Core Algorithms & Data Structures
                    </CardTitle>
                    <CardDescription className="text-xs mt-1">
                      Array processing, two-pointer techniques, recursion, and dynamic programming.
                    </CardDescription>
                  </div>
                </CardHeader>
                <CardContent className="flex-1 space-y-3 text-xs text-muted-foreground">
                  <div className="space-y-2 border-t border-border pt-3">
                    <div className="flex items-center justify-between">
                      <span>Coding Questions:</span>
                      <span className="font-semibold text-foreground">3 Problems</span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span>Execution Environment:</span>
                      <span className="font-semibold text-foreground">High Speed Isolate</span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span>Allowed Languages:</span>
                      <span className="font-semibold text-foreground">Python, Java, JS, C++</span>
                    </div>
                  </div>
                </CardContent>
                <CardFooter className="border-t border-border/60 pt-4">
                  <Button
                    onClick={() =>
                      toast.add({
                        title: "Assessment Initialization",
                        description: "Assessment launcher is being implemented next in the Assessments module.",
                        type: "info",
                      })
                    }
                    className="w-full gap-2 text-xs font-semibold"
                  >
                    <span>Begin Assessment</span>
                    <ArrowRight className="size-3.5" />
                  </Button>
                </CardFooter>
              </Card>

              {/* Assessment 3 */}
              <Card className="flex flex-col border-border/80 transition-all hover:border-primary/40 hover:shadow-md">
                <CardHeader className="space-y-3 pb-4">
                  <div className="flex items-center justify-between">
                    <Badge variant="secondary" className="gap-1 text-[10px] bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20">
                      <Database className="size-3" />
                      <span>SQL & Data</span>
                    </Badge>
                    <div className="flex items-center gap-1 text-xs text-muted-foreground">
                      <Clock className="size-3.5" />
                      <span>45 Mins</span>
                    </div>
                  </div>
                  <div>
                    <CardTitle className="text-base font-bold text-foreground">
                      Database Queries & SQL Optimization
                    </CardTitle>
                    <CardDescription className="text-xs mt-1">
                      Complex table joins, window functions, aggregations, and subquery optimization.
                    </CardDescription>
                  </div>
                </CardHeader>
                <CardContent className="flex-1 space-y-3 text-xs text-muted-foreground">
                  <div className="space-y-2 border-t border-border pt-3">
                    <div className="flex items-center justify-between">
                      <span>Query Questions:</span>
                      <span className="font-semibold text-foreground">3 Queries</span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span>Execution Environment:</span>
                      <span className="font-semibold text-foreground">Live PostgreSQL Sandbox</span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span>Allowed Languages:</span>
                      <span className="font-semibold text-foreground">PostgreSQL, MySQL</span>
                    </div>
                  </div>
                </CardContent>
                <CardFooter className="border-t border-border/60 pt-4">
                  <Button
                    onClick={() =>
                      toast.add({
                        title: "Assessment Initialization",
                        description: "Assessment launcher is being implemented next in the Assessments module.",
                        type: "info",
                      })
                    }
                    className="w-full gap-2 text-xs font-semibold"
                  >
                    <span>Begin Assessment</span>
                    <ArrowRight className="size-3.5" />
                  </Button>
                </CardFooter>
              </Card>
            </div>
          </div>
        )}

        {/* Tab 2: Practice Arena */}
        {activeTab === "practice" && (
          <div className="flex min-h-[380px] items-center justify-center rounded-xl border border-dashed border-border bg-card/40 p-12 text-center">
            <span className="text-xl font-semibold tracking-tight text-muted-foreground">
              Coming Soon
            </span>
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="mt-auto border-t border-border bg-card/40 py-6 text-center text-xs text-muted-foreground">
        <div className="mx-auto flex max-w-7xl flex-col items-center justify-between gap-3 px-4 sm:flex-row sm:px-6 lg:px-8">
          <div className="flex items-center gap-2">
            <Logo size={18} />
            <span className="font-semibold text-foreground">Stacked</span>
            <span>— Developer Assessment & Benchmarking</span>
          </div>
          <div className="flex items-center gap-4 text-xs">
            <span>Powered by Spring Boot 4</span>
            <span>•</span>
            <span>MongoDB 8.0</span>
            <span>•</span>
            <span>Docker Execution</span>
          </div>
        </div>
      </footer>
    </div>
  )
}
