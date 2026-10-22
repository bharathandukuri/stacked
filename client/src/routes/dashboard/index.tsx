import { useState, useEffect } from "react";
import { createFileRoute, useNavigate, Link } from "@tanstack/react-router";
import {
  Shield,
  LogOut,
  Clock,
  ArrowRight,
  Loader2,
  FileQuestion,
} from "lucide-react";

import { Logo } from "@/components/logo";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
  useUser,
  useIsAdmin,
  useIsAuthenticated,
  useIsInitialized,
} from "@/stores/auth-store";
import { useLogout, initializeAuthSession } from "@/hooks/use-auth";
import {
  useMyAssessments,
  useJoinAssessmentByCode,
} from "@/hooks/use-assessments";
import { toast } from "@/components/ui/toast";
import type { CandidateParticipationStatus } from "@/types/assessment";

export const Route = createFileRoute("/dashboard/")({
  component: DashboardPage,
});

export function DashboardPage() {
  const user = useUser();
  const isAdmin = useIsAdmin();
  const isAuthenticated = useIsAuthenticated();
  const isInitialized = useIsInitialized();
  const navigate = useNavigate();
  const logoutMutation = useLogout();

  const [inviteCode, setInviteCode] = useState("");

  const { data: myAssessments, isLoading: loadingAssessments } =
    useMyAssessments();
  const joinMutation = useJoinAssessmentByCode();

  // Authentication Guard
  useEffect(() => {
    if (!isInitialized) {
      initializeAuthSession();
    } else if (!isAuthenticated) {
      navigate({ to: "/login", replace: true });
    }
  }, [isInitialized, isAuthenticated, navigate]);

  const handleLogout = async () => {
    try {
      await logoutMutation.mutateAsync();
      toast.add({
        title: "Logged Out",
        description: "You have been signed out successfully.",
        type: "success",
      });
      navigate({ to: "/login", replace: true });
    } catch {
      navigate({ to: "/login", replace: true });
    }
  };

  const handleJoinAssessment = async (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = inviteCode.trim().toUpperCase();
    if (!trimmed) {
      toast.add({
        title: "Invite Code Required",
        description: "Please enter a valid assessment code or ID.",
        type: "error",
      });
      return;
    }

    try {
      const summary = await joinMutation.mutateAsync({ code: trimmed });
      setInviteCode("");
      navigate({
        to: "/assessments/$assessmentId",
        params: { assessmentId: summary.assessmentId },
      });
    } catch {
      // error handled in mutation
    }
  };

  if (!isInitialized || !isAuthenticated || !user) {
    return (
      <div className="flex h-screen w-full items-center justify-center bg-background">
        <div className="flex flex-col items-center gap-3 text-muted-foreground">
          <div className="size-8 animate-spin rounded-full border-2 border-primary border-t-transparent" />
          <p className="text-sm font-medium">Loading your dashboard...</p>
        </div>
      </div>
    );
  }

  const userInitial = user.name ? user.name[0].toUpperCase() : "U";

  return (
    <div className="flex min-h-screen w-full flex-col bg-background font-sans text-foreground">
      {/* Top Navbar */}
      <header className="sticky top-0 z-40 border-b border-border bg-card/80 backdrop-blur-md">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
          {/* Logo & Platform Name */}
          <div className="flex items-center gap-6">
            <Link
              to="/dashboard"
              className="flex items-center gap-3 transition-opacity hover:opacity-90"
            >
              <Logo size={32} />
              <div className="flex flex-col">
                <span className="text-base font-bold tracking-tight text-foreground">
                  Stacked
                </span>
                {/* <span className="text-[10px] font-semibold tracking-wider text-muted-foreground uppercase">
                  Candidate Portal
                </span> */}
              </div>
            </Link>
          </div>

          {/* Right Action Bar */}
          <div className="flex items-center gap-3">
            {/* If user is an ADMIN, prominently display Admin Portal button */}
            {/* {isAdmin && (
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
            )} */}

            {/* User Profile Dropdown Menu */}
            <DropdownMenu>
              <DropdownMenuTrigger
                render={
                  <Button
                    variant="ghost"
                    className="flex items-center gap-2.5 rounded-full p-1 pl-2 hover:bg-muted"
                  >
                    <span className="hidden max-w-[120px] truncate text-xs font-medium text-foreground sm:inline-block">
                      {user.name}
                    </span>
                    <Avatar className="size-8 border border-border">
                      <AvatarFallback className="bg-primary/10 text-xs font-bold text-primary">
                        {userInitial}
                      </AvatarFallback>
                    </Avatar>
                  </Button>
                }
              />
              <DropdownMenuContent align="end" className="w-56 p-1">
                <DropdownMenuLabel className="px-2 py-1.5 font-normal">
                  <div className="flex flex-col space-y-1">
                    <p className="truncate text-sm leading-none font-semibold text-foreground">
                      {user.name}
                    </p>
                    <p className="truncate text-xs leading-none text-muted-foreground">
                      {user.email}
                    </p>
                    <div className="pt-1">
                      <Badge
                        variant={isAdmin ? "default" : "secondary"}
                        className="px-1.5 py-0 text-[10px]"
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
                      className="cursor-pointer gap-2 text-xs"
                    >
                      <Shield className="size-3.5 text-primary" />
                      <span>Admin Portal</span>
                    </DropdownMenuItem>
                    <DropdownMenuSeparator />
                  </>
                )}

                <DropdownMenuItem
                  onClick={handleLogout}
                  className="cursor-pointer gap-2 text-xs text-destructive focus:text-destructive"
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
      <main className="mx-auto w-full max-w-7xl flex-1 space-y-6 px-4 py-8 sm:px-6 lg:px-8">
        <div className="space-y-6">
            {/* Join with Invite Code Bar */}
            <Card className="border-border/80 shadow-xs">
              <CardContent className="p-6">
                <form
                  onSubmit={handleJoinAssessment}
                  className="flex flex-col items-stretch justify-between gap-4 sm:flex-row sm:items-center"
                >
                  <div className="space-y-1">
                    <h3 className="text-base font-semibold text-foreground">
                      Have an Assessment Invitation Code?
                    </h3>
                    <p className="text-xs text-muted-foreground">
                      Enter the passcode provided by your recruiter or test
                      administrator to begin your session.
                    </p>
                  </div>
                  <div className="flex w-full items-center gap-2 sm:max-w-md">
                    <Input
                      placeholder="e.g. STK-7X9B-4R2D"
                      value={inviteCode}
                      onChange={(e) =>
                        setInviteCode(e.target.value.toUpperCase())
                      }
                      className="h-10 font-mono text-xs uppercase"
                      disabled={joinMutation.isPending}
                    />
                    <Button
                      type="submit"
                      size="sm"
                      className="h-10 shrink-0 gap-1.5 px-4 font-medium"
                      disabled={joinMutation.isPending || !inviteCode.trim()}
                    >
                      {joinMutation.isPending ? (
                        <>
                          <Loader2 className="size-3.5 animate-spin" />
                          <span>Joining...</span>
                        </>
                      ) : (
                        <>
                          <span>Enter Assessment</span>
                          <ArrowRight className="size-3.5" />
                        </>
                      )}
                    </Button>
                  </div>
                </form>
              </CardContent>
            </Card>

            {/* Assessment Cards Header */}
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-lg font-bold text-foreground">
                  Your Assigned Assessments
                </h2>
                <p className="text-xs text-muted-foreground">
                  Benchmarking and technical tests assigned to your candidate
                  profile.
                </p>
              </div>
              <Badge variant="outline" className="text-xs">
                {myAssessments?.length ?? 0} Assessment(s) Assigned
              </Badge>
            </div>

            {/* Assessment Cards Grid */}
            {loadingAssessments ? (
              <div className="flex items-center justify-center gap-3 py-16 text-muted-foreground">
                <Loader2 className="h-6 w-6 animate-spin text-primary" />
                <span className="text-sm">Loading your assessments...</span>
              </div>
            ) : myAssessments?.length === 0 ? (
              <Card className="border-dashed border-border/80">
                <CardContent className="flex flex-col items-center justify-center gap-3 py-12 text-center">
                  <FileQuestion className="h-10 w-10 text-muted-foreground/60" />
                  <div className="space-y-1">
                    <h3 className="font-semibold text-foreground">
                      No Assessments Assigned Yet
                    </h3>
                    <p className="max-w-sm text-xs text-muted-foreground">
                      You currently have no pending tests. If you were provided
                      an assessment invite code, enter it in the box above to
                      access your test.
                    </p>
                  </div>
                </CardContent>
              </Card>
            ) : (
              <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
                {myAssessments?.map((a) => {
                  const getStatusBadge = (
                    status: CandidateParticipationStatus
                  ) => {
                    switch (status) {
                      case "INVITED":
                        return (
                          <Badge variant="secondary" className="text-[10px]">
                            Not Started
                          </Badge>
                        );
                      case "IN_PROGRESS":
                        return (
                          <Badge className="animate-pulse border-amber-500/30 bg-amber-500/15 text-[10px] text-amber-600 dark:text-amber-400">
                            In Progress
                          </Badge>
                        );
                      case "SUBMITTED":
                        return (
                          <Badge className="border-emerald-500/30 bg-emerald-500/15 text-[10px] text-emerald-600 dark:text-emerald-400">
                            Completed
                          </Badge>
                        );
                      case "EXPIRED":
                        return (
                          <Badge variant="destructive" className="text-[10px]">
                            Expired
                          </Badge>
                        );
                      default:
                        return (
                          <Badge variant="outline" className="text-[10px]">
                            {status}
                          </Badge>
                        );
                    }
                  };

                  return (
                    <Card
                      key={a.assessmentId}
                      className="flex flex-col border-border/80 transition-all hover:border-primary/40 hover:shadow-md"
                    >
                      <CardHeader className="space-y-3 pb-4">
                        <div className="flex items-center justify-between">
                          {getStatusBadge(a.assignmentStatus)}
                          <div className="flex items-center gap-1 text-xs text-muted-foreground">
                            <Clock className="size-3.5" />
                            <span>{a.durationMinutes} Mins</span>
                          </div>
                        </div>
                        <div>
                          <CardTitle className="line-clamp-1 text-base font-bold text-foreground">
                            {a.title}
                          </CardTitle>
                          {a.description && (
                            <CardDescription className="mt-1 line-clamp-2 text-xs">
                              {a.description}
                            </CardDescription>
                          )}
                        </div>
                      </CardHeader>

                      <CardContent className="flex-1 space-y-3 text-xs text-muted-foreground">
                        <div className="space-y-2 border-t border-border pt-3">
                          <div className="flex items-center justify-between">
                            <span>Questions:</span>
                            <span className="font-semibold text-foreground">
                              {a.totalQuestions} Questions
                            </span>
                          </div>
                          <div className="flex items-center justify-between">
                            <span>Total Points:</span>
                            <span className="font-semibold text-foreground">
                              {a.totalScore} Points
                            </span>
                          </div>
                          <div className="flex items-center justify-between">
                            <span>Attempts Allowed:</span>
                            <span className="font-semibold text-foreground">
                              {a.maxAttempts
                                ? `${a.attemptsUsed} / ${a.maxAttempts}`
                                : "1 Attempt"}
                            </span>
                          </div>
                        </div>
                      </CardContent>

                      <CardFooter className="border-t border-border/60 pt-4">
                        {a.assignmentStatus === "IN_PROGRESS" ? (
                          <Link
                            to="/assessments/$assessmentId/take"
                            params={{ assessmentId: a.assessmentId }}
                            className="w-full"
                          >
                            <Button className="w-full gap-2 bg-amber-600 text-xs font-semibold text-white hover:bg-amber-700">
                              <span>Resume Assessment</span>
                              <ArrowRight className="size-3.5" />
                            </Button>
                          </Link>
                        ) : a.assignmentStatus === "SUBMITTED" ? (
                          <Link
                            to="/assessments/$assessmentId/results"
                            params={{ assessmentId: a.assessmentId }}
                            className="w-full"
                          >
                            <Button className="w-full gap-2 text-xs font-semibold bg-emerald-600 hover:bg-emerald-700 text-white">
                              <span>View Results</span>
                              <ArrowRight className="size-3.5" />
                            </Button>
                          </Link>
                        ) : (
                          <Link
                            to="/assessments/$assessmentId"
                            params={{ assessmentId: a.assessmentId }}
                            className="w-full"
                          >
                            <Button className="w-full gap-2 text-xs font-semibold">
                              <span>
                                {a.assignmentStatus === "EXPIRED"
                                  ? "View Summary"
                                  : "Begin Assessment"}
                              </span>
                              <ArrowRight className="size-3.5" />
                            </Button>
                          </Link>
                        )}
                      </CardFooter>
                    </Card>
                  );
                })}
              </div>
            )}
          </div>
      </main>
      {/* Footer */}
      <footer className="mt-auto border-t border-border py-4 text-xs text-muted-foreground">
        <div className="mx-auto flex max-w-7xl items-center justify-center gap-2 px-4 sm:px-6 lg:px-8">
          <Logo size={16} />
          <span className="font-semibold text-foreground">Stacked</span>
          <span>© {new Date().getFullYear()}</span>
        </div>
      </footer>
    </div>
  );
}
