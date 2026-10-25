import { useEffect, type ReactNode } from "react"
import { Link, useNavigate } from "@tanstack/react-router"
import { ChevronRight } from "lucide-react"
import { AdminSidebar } from "@/components/admin/sidebar"
import { useIsAuthenticated, useIsInitialized } from "@/stores/auth-store"
import { initializeAuthSession } from "@/hooks/use-auth"
import { PageLoadingState } from "@/components/common/page-loading-state"
import { cn } from "@/lib/utils"

interface AdminPageLayoutProps {
  currentPath: string
  title?: string
  description?: string
  actions?: ReactNode
  breadcrumbs?: { label: string; href?: string }[]
  children: ReactNode
  className?: string
  contentClassName?: string
}

export function AdminPageLayout({
  currentPath,
  title,
  description,
  actions,
  breadcrumbs,
  children,
  className,
  contentClassName,
}: AdminPageLayoutProps) {
  const navigate = useNavigate()
  const isAuthenticated = useIsAuthenticated()
  const isInitialized = useIsInitialized()

  useEffect(() => {
    let isMounted = true

    async function verifySession() {
      if (!isInitialized) {
        const hasSession = await initializeAuthSession()
        if (isMounted && !hasSession) {
          navigate({ to: "/admin/login", replace: true })
        }
      } else if (!isAuthenticated) {
        navigate({ to: "/admin/login", replace: true })
      }
    }

    verifySession()

    return () => {
      isMounted = false
    }
  }, [isAuthenticated, isInitialized, navigate])

  if (!isInitialized) {
    return <PageLoadingState message="Verifying administrative credentials..." />
  }

  return (
    <div className={cn("flex h-screen w-screen overflow-hidden bg-background", className)}>
      <AdminSidebar currentPath={currentPath} />

      <div className={cn("flex flex-1 flex-col overflow-y-auto", contentClassName)}>
        {(title || actions || (breadcrumbs && breadcrumbs.length > 0)) && (
          <header className="border-b border-border bg-card/40 px-6 py-5">
            {breadcrumbs && breadcrumbs.length > 0 && (
              <nav className="mb-3 flex items-center space-x-1.5 text-xs text-muted-foreground">
                {breadcrumbs.map((crumb, idx) => (
                  <div key={idx} className="flex items-center space-x-1.5">
                    {idx > 0 && <ChevronRight className="h-3.5 w-3.5 text-muted-foreground/50" />}
                    {crumb.href ? (
                      <Link
                        to={crumb.href}
                        className="hover:text-foreground transition-colors font-medium"
                      >
                        {crumb.label}
                      </Link>
                    ) : (
                      <span className="text-foreground font-medium">{crumb.label}</span>
                    )}
                  </div>
                ))}
              </nav>
            )}
            <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
              <div>
                {title && (
                  <h1 className="text-xl font-bold tracking-tight text-foreground">
                    {title}
                  </h1>
                )}
                {description && (
                  <p className="text-xs text-muted-foreground mt-1">
                    {description}
                  </p>
                )}
              </div>
              {actions && (
                <div className="flex flex-wrap items-center gap-2.5">
                  {actions}
                </div>
              )}
            </div>
          </header>
        )}

        <main className="flex-1 p-6">{children}</main>
      </div>
    </div>
  )
}
