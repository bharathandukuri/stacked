// src/routes/__root.tsx
import { useEffect } from "react"
import { createRootRoute, Outlet } from "@tanstack/react-router"
import { Toaster } from "@/components/ui/toast"
import { TooltipProvider } from "@/components/ui/tooltip"
import { initializeAuthSession } from "@/hooks/use-auth"

export const Route = createRootRoute({
  component: RootComponent,
})

function RootComponent() {
  useEffect(() => {
    initializeAuthSession()
  }, [])

  return (
    <TooltipProvider>
      <Outlet />
      <Toaster />
    </TooltipProvider>
  )
}
