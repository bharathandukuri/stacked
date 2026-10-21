import { useEffect } from "react"
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import { useIsAuthenticated, useIsInitialized } from "@/stores/auth-store"
import { initializeAuthSession } from "@/hooks/use-auth"
import { Logo } from "@/components/logo"

export const Route = createFileRoute("/")({
  component: HomeComponent,
})

function HomeComponent() {
  const navigate = useNavigate()
  const isAuthenticated = useIsAuthenticated()
  const isInitialized = useIsInitialized()

  useEffect(() => {
    if (!isInitialized) {
      initializeAuthSession()
    } else if (isAuthenticated) {
      navigate({ to: "/dashboard", replace: true })
    } else {
      navigate({ to: "/login", replace: true })
    }
  }, [isInitialized, isAuthenticated, navigate])

  return (
    <main className="flex min-h-screen items-center justify-center bg-background p-6 text-foreground">
      <div className="flex flex-col items-center gap-4 text-center">
        <Logo size={48} />
        <div className="size-6 animate-spin rounded-full border-2 border-primary border-t-transparent" />
        <p className="text-xs text-muted-foreground font-medium">Entering Stacked...</p>
      </div>
    </main>
  )
}
