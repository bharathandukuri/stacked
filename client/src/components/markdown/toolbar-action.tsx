import * as React from "react"
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip"
import { Kbd } from "@/components/ui/kbd"
import { cn } from "@/lib/utils"
import { formatForDisplay } from "@tanstack/react-hotkeys"
import type { ToolbarActionDef } from "./markdown-config"

export interface ToolbarActionProps {
  action: ToolbarActionDef
  onExecute: (action: ToolbarActionDef) => void
  disabled?: boolean
  isActive?: boolean
  className?: string
}

export const ToolbarAction = React.memo(function ToolbarAction({
  action,
  onExecute,
  disabled = false,
  isActive = false,
  className,
}: ToolbarActionProps) {
  const Icon = action.icon

  return (
    <Tooltip>
      <TooltipTrigger
        type="button"
        disabled={disabled}
        onClick={() => onExecute(action)}
        className={cn(
          "inline-flex h-7 w-7 cursor-pointer items-center justify-center rounded-md text-muted-foreground transition-colors",
          "hover:bg-muted hover:text-foreground focus-visible:bg-muted focus-visible:outline-none",
          isActive && "bg-muted font-bold text-foreground",
          disabled &&
            "cursor-not-allowed opacity-40 hover:bg-transparent hover:text-muted-foreground",
          className
        )}
        aria-label={action.label}
      >
        <Icon className="h-3.5 w-3.5" />
      </TooltipTrigger>
      <TooltipContent side="top" className="rounded-md text-xs">
        <div className="flex items-center gap-1.5 font-medium">
          <span>{action.label}</span>
          {action.shortcut && (
            <Kbd className="h-4 px-1 font-mono text-[10px]">
              {formatForDisplay(action.shortcut)}
            </Kbd>
          )}
        </div>
      </TooltipContent>
    </Tooltip>
  )
})
