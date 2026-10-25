import { AlertCircle, ArrowLeft, RefreshCw } from "lucide-react"
import { Link } from "@tanstack/react-router"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

interface PageErrorStateProps {
  title?: string
  message?: string
  onRetry?: () => void
  returnTo?: string
  returnLabel?: string
  fullScreen?: boolean
  className?: string
}

export function PageErrorState({
  title = "Something went wrong",
  message = "An unexpected error occurred while loading this page.",
  onRetry,
  returnTo = "/",
  returnLabel = "Return to Dashboard",
  fullScreen = true,
  className,
}: PageErrorStateProps) {
  return (
    <div
      className={cn(
        "flex w-full flex-col items-center justify-center bg-background p-6",
        fullScreen ? "min-h-screen" : "py-16",
        className
      )}
    >
      <div className="max-w-md text-center space-y-4">
        <div className="size-12 rounded-full bg-destructive/10 text-destructive flex items-center justify-center mx-auto">
          <AlertCircle className="size-6" />
        </div>
        <div className="space-y-1.5">
          <h2 className="text-xl font-bold tracking-tight text-foreground">
            {title}
          </h2>
          <p className="text-xs text-muted-foreground leading-relaxed">
            {message}
          </p>
        </div>
        <div className="flex items-center justify-center gap-3 pt-2">
          {onRetry && (
            <Button
              onClick={onRetry}
              variant="outline"
              size="sm"
              className="text-xs gap-1.5"
            >
              <RefreshCw className="size-3.5" />
              <span>Try Again</span>
            </Button>
          )}
          {returnTo && (
            <Link to={returnTo}>
              <Button size="sm" className="text-xs gap-1.5">
                <ArrowLeft className="size-3.5" />
                <span>{returnLabel}</span>
              </Button>
            </Link>
          )}
        </div>
      </div>
    </div>
  )
}

