import { useState, useEffect } from "react"
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import {
  Lock,
  Mail,
  AlertCircle,
  Loader2,
  Eye,
  EyeOff,
  ArrowRight,
} from "lucide-react"

import { Logo } from "@/components/logo"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { useLogin } from "@/hooks/use-auth"
import { useIsAuthenticated, useIsInitialized } from "@/stores/auth-store"
import { toast } from "@/components/ui/toast"
import type { ApiError } from "@/types/api"

export const Route = createFileRoute("/login")({
  component: LoginPage,
})

export function LoginPage() {
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [showPassword, setShowPassword] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const loginMutation = useLogin()
  const navigate = useNavigate()
  const isAuthenticated = useIsAuthenticated()
  const isInitialized = useIsInitialized()

  // If already authenticated, redirect to dashboard
  useEffect(() => {
    if (isInitialized && isAuthenticated) {
      navigate({ to: "/dashboard", replace: true })
    }
  }, [isInitialized, isAuthenticated, navigate])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    e.stopPropagation()

    if (!email.trim() || !password) {
      setErrorMessage("Please enter both email and password.")
      return
    }

    setErrorMessage(null)
    try {
      const res = await loginMutation.mutateAsync({
        email: email.trim(),
        password,
      })

      const user = res?.admin
      toast.add({
        title: "Welcome Back",
        description: `Signed in as ${user?.name || "User"}.`,
        type: "success",
      })

      await navigate({ to: "/dashboard", replace: true })
    } catch (err: unknown) {
      const apiError = err as ApiError
      const message =
        apiError?.message ||
        "Invalid email or password. Please verify your credentials."
      setErrorMessage(message)
    }
  }

  return (
    <div className="grid min-h-screen w-full lg:grid-cols-2 bg-background font-sans">
      {/* Left Column: Visual Showcase (Desktop) */}
      <div className="relative hidden lg:flex flex-col justify-between overflow-hidden border-r border-border/50 bg-zinc-950 p-12 text-zinc-200">
        {/* Subtle ambient lighting */}
        <div className="pointer-events-none absolute -top-24 -left-24 size-96 rounded-full bg-primary/15 blur-3xl" />
        <div className="pointer-events-none absolute bottom-0 right-0 size-96 rounded-full bg-indigo-500/10 blur-3xl" />
        <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(#27272a_1px,transparent_1px)] [background-size:24px_24px] opacity-30" />

        {/* Top Branding */}
        <div className="relative z-10 flex items-center gap-3">
          <Logo size={32} />
          <span className="text-xl font-bold tracking-tight text-white">
            Stacked
          </span>
        </div>

        {/* Middle Visual Element */}
        <div className="relative z-10 my-auto max-w-lg space-y-8">
          {/* Code / Evaluation Preview Card */}
          <div className="rounded-xl border border-zinc-800/80 bg-zinc-900/70 p-5 backdrop-blur-md shadow-2xl">
            <div className="flex items-center justify-between pb-3 border-b border-zinc-800/80">
              <div className="flex items-center gap-2">
                <div className="size-2.5 rounded-full bg-rose-500/80" />
                <div className="size-2.5 rounded-full bg-amber-500/80" />
                <div className="size-2.5 rounded-full bg-emerald-500/80" />
                <span className="ml-2 font-mono text-[11px] text-zinc-500">
                  evaluate.ts
                </span>
              </div>
              <span className="font-mono text-[10px] text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full border border-emerald-500/20">
                ✓ Passed (14ms)
              </span>
            </div>
            <div className="mt-4 font-mono text-xs leading-relaxed space-y-1">
              <div className="text-zinc-500">
                // Run candidate submission in isolated sandbox
              </div>
              <div>
                <span className="text-purple-400">const</span>{" "}
                <span className="text-blue-400">result</span> ={" "}
                <span className="text-purple-400">await</span>{" "}
                <span className="text-yellow-300">judge</span>&#40;&#123;
              </div>
              <div className="pl-4">
                <span className="text-zinc-400">problem:</span>{" "}
                <span className="text-emerald-300">&quot;distributed-rate-limiter&quot;</span>,
              </div>
              <div className="pl-4">
                <span className="text-zinc-400">language:</span>{" "}
                <span className="text-emerald-300">&quot;java-21&quot;</span>,
              </div>
              <div className="pl-4">
                <span className="text-zinc-400">security:</span>{" "}
                <span className="text-blue-300">&quot;strict-isolate&quot;</span>,
              </div>
              <div>&#125;&#41;;</div>
              <div className="pt-2 text-zinc-500">
                // Output: 42/42 testcases passed
              </div>
            </div>
          </div>

          <div className="space-y-2">
            <h2 className="text-2xl font-bold tracking-tight text-white sm:text-3xl">
              Engineering assessment, built with precision.
            </h2>
            <p className="text-sm text-zinc-400 leading-relaxed">
              Secure, multi-language execution runtimes for evaluating engineering talent at scale.
            </p>
          </div>
        </div>

        {/* Bottom Platform Status */}
        <div className="relative z-10 flex items-center justify-between border-t border-zinc-800/80 pt-6 text-xs text-zinc-500">
          <div className="flex items-center gap-2">
            <span className="relative flex size-2">
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-75" />
              <span className="relative inline-flex size-2 rounded-full bg-emerald-500" />
            </span>
            <span>All execution engines operational</span>
          </div>
          <span>v2.0 Platform</span>
        </div>
      </div>

      {/* Right Column: Clean Login Form */}
      <div className="flex flex-col justify-center items-center px-6 py-12 sm:px-12 lg:px-16">
        <div className="w-full max-w-sm">
          {/* Mobile Header Logo */}
          <div className="mb-8 flex items-center gap-2.5 lg:hidden">
            <Logo size={32} />
            <span className="text-lg font-bold tracking-tight text-foreground">
              Stacked
            </span>
          </div>

          {/* Title & Subtitle */}
          <div className="space-y-1.5 mb-8">
            <h1 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
              Sign in
            </h1>
            <p className="text-xs text-muted-foreground">
              Welcome back! Please enter your credentials to continue.
            </p>
          </div>

          {/* Error Alert */}
          {errorMessage && (
            <div className="mb-6 flex items-start gap-2.5 rounded-lg border border-destructive/30 bg-destructive/10 p-3 text-xs text-destructive">
              <AlertCircle className="size-4 shrink-0 mt-0.5" />
              <span className="leading-relaxed font-medium">{errorMessage}</span>
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Email Field */}
            <div className="space-y-1.5">
              <Label
                htmlFor="email"
                className="text-xs font-medium text-foreground"
              >
                Email
              </Label>
              <div className="relative">
                <Mail className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  id="email"
                  type="email"
                  placeholder="you@example.com"
                  value={email}
                  onChange={(e) => {
                    setEmail(e.target.value)
                    if (errorMessage) setErrorMessage(null)
                  }}
                  className="h-10 pl-9 text-xs"
                  autoComplete="email"
                  autoCapitalize="none"
                  autoFocus
                  required
                />
              </div>
            </div>

            {/* Password Field */}
            <div className="space-y-1.5">
              <Label
                htmlFor="password"
                className="text-xs font-medium text-foreground"
              >
                Password
              </Label>
              <div className="relative">
                <Lock className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  id="password"
                  type={showPassword ? "text" : "password"}
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => {
                    setPassword(e.target.value)
                    if (errorMessage) setErrorMessage(null)
                  }}
                  className="h-10 pl-9 pr-9 text-xs"
                  autoComplete="current-password"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute top-1/2 right-2.5 -translate-y-1/2 p-1 text-muted-foreground hover:text-foreground transition-colors"
                  aria-label={showPassword ? "Hide password" : "Show password"}
                >
                  {showPassword ? (
                    <EyeOff className="size-3.5" />
                  ) : (
                    <Eye className="size-3.5" />
                  )}
                </button>
              </div>
            </div>

            {/* Submit Button */}
            <Button
              type="submit"
              disabled={loginMutation.isPending}
              className="mt-2 h-10 w-full text-xs font-semibold gap-2 shadow-xs transition-all"
            >
              {loginMutation.isPending ? (
                <>
                  <Loader2 className="size-3.5 animate-spin" />
                  <span>Signing in...</span>
                </>
              ) : (
                <>
                  <span>Sign In</span>
                  <ArrowRight className="size-3.5" />
                </>
              )}
            </Button>
          </form>

          {/* Minimal Footer */}
          <p className="mt-8 text-center text-[11px] text-muted-foreground/60">
            By signing in, you agree to our Terms of Service &bull; Privacy Policy
          </p>
        </div>
      </div>
    </div>
  )
}
