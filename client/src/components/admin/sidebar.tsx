import { Link } from "@tanstack/react-router"
import {
  LayoutDashboard,
  FileQuestion,
  GraduationCap,
  Users,
  BarChart3,
  Settings,
  LogOut,
  ShieldCheck,
} from "lucide-react"

import { Logo } from "@/components/logo"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { toast } from "@/components/ui/toast"
import { useAdmin } from "@/stores/auth-store"
import { useLogout } from "@/hooks/use-auth"

interface SidebarProps {
  currentPath?: string
}

export function AdminSidebar({ currentPath = "/admin" }: SidebarProps) {
  const admin = useAdmin()
  const logoutMutation = useLogout()

  const navItems = [
    {
      title: "Dashboard",
      icon: LayoutDashboard,
      to: "/admin",
      active: currentPath === "/admin",
    },
    {
      title: "Assessments",
      icon: GraduationCap,
      to: "/admin/assessments",
      badge: "Soon",
      active: currentPath === "/admin/assessments",
    },
    {
      title: "Question Bank",
      icon: FileQuestion,
      to: "/admin/questions",
      badge: "Soon",
      active: currentPath === "/admin/questions",
    },
    {
      title: "Candidates",
      icon: Users,
      to: "/admin/candidates",
      badge: "Soon",
      active: currentPath === "/admin/candidates",
    },
    {
      title: "Analytics",
      icon: BarChart3,
      to: "/admin/analytics",
      badge: "Soon",
      active: currentPath === "/admin/analytics",
    },
    {
      title: "Settings",
      icon: Settings,
      to: "/admin/settings",
      badge: "Soon",
      active: currentPath === "/admin/settings",
    },
  ]

  return (
    <aside className="flex h-screen w-64 flex-col border-r border-border bg-card/60 backdrop-blur-md">
      {/* Brand Header */}
      <div className="flex h-16 items-center gap-3 border-b border-border px-6">
        <Logo size={32} />
        <div className="flex flex-col">
          <span className="leading-tight font-bold tracking-tight text-foreground">
            Stacked
          </span>
          <span className="text-[11px] font-medium tracking-wider text-muted-foreground uppercase">
            Assessment Admin
          </span>
        </div>
      </div>

      {/* Navigation */}
      <div className="flex-1 overflow-y-auto px-3 py-4">
        <div className="mb-2 px-3 text-xs font-semibold tracking-wider text-muted-foreground uppercase">
          Navigation
        </div>
        <nav className="space-y-1">
          {navItems.map((item) => {
            const Icon = item.icon
            const isActive = item.active

            return (
              <Link
                key={item.title}
                to={item.to}
                className={`group flex items-center justify-between rounded-lg px-3 py-2 text-sm font-medium transition-all ${
                  isActive
                    ? "bg-primary text-primary-foreground shadow-sm"
                    : "text-muted-foreground hover:bg-muted hover:text-foreground"
                }`}
              >
                <div className="flex items-center gap-3">
                  <Icon
                    className={`size-4 ${isActive ? "text-primary-foreground" : "text-muted-foreground group-hover:text-foreground"}`}
                  />
                  <span>{item.title}</span>
                </div>
                {item.badge && (
                  <Badge
                    variant="outline"
                    className={`px-1.5 py-0 text-[10px] font-normal ${
                      isActive
                        ? "border-primary-foreground/30 text-primary-foreground"
                        : "border-border text-muted-foreground"
                    }`}
                  >
                    {item.badge}
                  </Badge>
                )}
              </Link>
            )
          })}
        </nav>
      </div>

      {/* Admin User Footer */}
      <div className="border-t border-border bg-muted/20 p-4">
        <div className="mb-3 flex items-center justify-between">
          <div className="flex flex-col overflow-hidden pr-2">
            <span className="truncate text-sm font-semibold text-foreground">
              {admin?.name || "Platform Admin"}
            </span>
            <span className="truncate text-xs text-muted-foreground">
              {admin?.email || "admin@stacked.com"}
            </span>
          </div>
          <Badge
            variant="secondary"
            className="flex items-center gap-1 border border-emerald-500/30 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
          >
            <ShieldCheck className="size-3" />
            ADMIN
          </Badge>
        </div>

        <Button
          variant="outline"
          size="sm"
          onClick={() => {
            logoutMutation.mutate(undefined, {
              onSettled: () => {
                toast.add({
                  title: "Signed Out",
                  description: "You have been signed out.",
                  type: "info",
                })
              },
            })
          }}
          disabled={logoutMutation.isPending}
          className="w-full gap-2 border-border text-muted-foreground transition-colors hover:border-destructive/30 hover:bg-destructive/10 hover:text-destructive"
        >
          <LogOut className="size-3.5" />
          <span>
            {logoutMutation.isPending ? "Signing Out..." : "Sign Out"}
          </span>
        </Button>
      </div>
    </aside>
  )
}
