import { Loader2 } from "lucide-react"
import { cn } from "@/lib/utils"

interface PageLoadingStateProps {
  message?: string
  fullScreen?: boolean
  className?: string
}

export function PageLoadingState({
  message = "Loading...",
  fullScreen = true,
  className,
}: PageLoadingStateProps) {
  return (
    <div
      className={cn(
        "flex w-full items-center justify-center bg-background",
        fullScreen ? "h-screen" : "py-16",
        className
      )}
    >
      <div className="flex flex-col items-center gap-3 text-muted-foreground">
        <Loader2 className="size-8 animate-spin text-primary" />
        <p className="text-xs font-medium">{message}</p>
      </div>
    </div>
  )
}

