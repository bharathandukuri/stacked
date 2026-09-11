import * as React from "react"
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip"
import { Separator } from "@/components/ui/separator"
import { Badge } from "@/components/ui/badge"
import { Kbd } from "@/components/ui/kbd"
import { Image as ImageIcon, Link as LinkIcon, Images } from "lucide-react"
import { cn } from "@/lib/utils"
import { formatForDisplay } from "@tanstack/react-hotkeys"
import { TOOLBAR_ACTIONS, type ToolbarActionDef } from "./markdown-config"
import { ToolbarAction } from "./toolbar-action"
import type { ToolbarConfig, ToolbarItem } from "./types"

const PRESET_SIMPLE: ToolbarItem[] = [
  "heading",
  "inline",
  "list",
  "insert",
  "link",
  "image",
  "session-images",
]

const PRESET_MINIMAL: ToolbarItem[] = ["inline", "list", "link"]

export interface MarkdownToolbarProps {
  onExecuteAction: (action: ToolbarActionDef) => void
  onOpenLinkDialog: () => void
  onOpenImageDialog: () => void
  onOpenSessionImagesDialog: () => void
  sessionImageCount?: number
  canUndo?: boolean
  canRedo?: boolean
  disabled?: boolean
  className?: string
  toolbar?: boolean | ToolbarConfig
}

export function MarkdownToolbar({
  onExecuteAction,
  onOpenLinkDialog,
  onOpenImageDialog,
  onOpenSessionImagesDialog,
  sessionImageCount = 0,
  canUndo = true,
  canRedo = true,
  disabled = false,
  className,
  toolbar = "all",
}: MarkdownToolbarProps) {
  const enabledSet = React.useMemo<Set<string> | null>(() => {
    if (toolbar === "all" || toolbar === true) return null
    if (toolbar === "simple") return new Set(PRESET_SIMPLE)
    if (toolbar === "minimal") return new Set(PRESET_MINIMAL)
    if (Array.isArray(toolbar)) return new Set(toolbar)
    return null
  }, [toolbar])

  const isEnabled = React.useCallback(
    (id: string, group?: string) => {
      if (!enabledSet) return true
      return enabledSet.has(id) || (group ? enabledSet.has(group) : false)
    },
    [enabledSet]
  )

  const historyActions = React.useMemo(
    () =>
      TOOLBAR_ACTIONS.filter(
        (a) => a.group === "history" && isEnabled(a.id, "history")
      ),
    [isEnabled]
  )
  const headingActions = React.useMemo(
    () =>
      TOOLBAR_ACTIONS.filter(
        (a) => a.group === "heading" && isEnabled(a.id, "heading")
      ),
    [isEnabled]
  )
  const inlineActions = React.useMemo(
    () =>
      TOOLBAR_ACTIONS.filter(
        (a) => a.group === "inline" && isEnabled(a.id, "inline")
      ),
    [isEnabled]
  )
  const mathActions = React.useMemo(
    () =>
      TOOLBAR_ACTIONS.filter(
        (a) => a.group === "math" && isEnabled(a.id, "math")
      ),
    [isEnabled]
  )
  const codeActions = React.useMemo(
    () =>
      TOOLBAR_ACTIONS.filter(
        (a) => a.group === "code" && isEnabled(a.id, "code")
      ),
    [isEnabled]
  )
  const listActions = React.useMemo(
    () =>
      TOOLBAR_ACTIONS.filter(
        (a) => a.group === "list" && isEnabled(a.id, "list")
      ),
    [isEnabled]
  )
  const insertActions = React.useMemo(
    () =>
      TOOLBAR_ACTIONS.filter(
        (a) => a.group === "insert" && isEnabled(a.id, "insert")
      ),
    [isEnabled]
  )

  const showLink = isEnabled("link", "link")
  const showImage = isEnabled("image", "image")
  const showSessionImages = isEnabled("session-images", "session-images")

  if (toolbar === false) {
    return null
  }

  // Collect active sections to render separators only between active groups
  const sections: React.ReactNode[] = []

  if (historyActions.length > 0) {
    sections.push(
      <React.Fragment key="history">
        {historyActions.map((action) => (
          <ToolbarAction
            key={action.id}
            action={action}
            disabled={
              disabled ||
              (action.id === "undo" && !canUndo) ||
              (action.id === "redo" && !canRedo)
            }
            onExecute={onExecuteAction}
          />
        ))}
      </React.Fragment>
    )
  }

  if (headingActions.length > 0) {
    sections.push(
      <React.Fragment key="heading">
        {headingActions.map((action) => (
          <ToolbarAction
            key={action.id}
            action={action}
            disabled={disabled}
            onExecute={onExecuteAction}
          />
        ))}
      </React.Fragment>
    )
  }

  if (inlineActions.length > 0) {
    sections.push(
      <React.Fragment key="inline">
        {inlineActions.map((action) => (
          <ToolbarAction
            key={action.id}
            action={action}
            disabled={disabled}
            onExecute={onExecuteAction}
          />
        ))}
      </React.Fragment>
    )
  }

  if (mathActions.length > 0) {
    sections.push(
      <React.Fragment key="math">
        {mathActions.map((action) => (
          <ToolbarAction
            key={action.id}
            action={action}
            disabled={disabled}
            onExecute={onExecuteAction}
          />
        ))}
      </React.Fragment>
    )
  }

  if (codeActions.length > 0) {
    sections.push(
      <React.Fragment key="code">
        {codeActions.map((action) => (
          <ToolbarAction
            key={action.id}
            action={action}
            disabled={disabled}
            onExecute={onExecuteAction}
          />
        ))}
      </React.Fragment>
    )
  }

  if (listActions.length > 0) {
    sections.push(
      <React.Fragment key="list">
        {listActions.map((action) => (
          <ToolbarAction
            key={action.id}
            action={action}
            disabled={disabled}
            onExecute={onExecuteAction}
          />
        ))}
      </React.Fragment>
    )
  }

  if (insertActions.length > 0) {
    sections.push(
      <React.Fragment key="insert">
        {insertActions.map((action) => (
          <ToolbarAction
            key={action.id}
            action={action}
            disabled={disabled}
            onExecute={onExecuteAction}
          />
        ))}
      </React.Fragment>
    )
  }

  if (showLink) {
    sections.push(
      <Tooltip key="link">
        <TooltipTrigger
          type="button"
          disabled={disabled}
          onClick={onOpenLinkDialog}
          className="inline-flex h-7 w-7 cursor-pointer items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-muted hover:text-foreground focus-visible:bg-muted focus-visible:outline-none"
          aria-label="Insert Link"
        >
          <LinkIcon className="h-3.5 w-3.5" />
        </TooltipTrigger>
        <TooltipContent side="top" className="rounded-md text-xs">
          <div className="flex items-center gap-1.5 font-medium">
            <span>Insert Link</span>
            <Kbd className="h-4 px-1 font-mono text-[10px]">
              {formatForDisplay("Mod+K")}
            </Kbd>
          </div>
        </TooltipContent>
      </Tooltip>
    )
  }

  if (showImage) {
    sections.push(
      <Tooltip key="image">
        <TooltipTrigger
          type="button"
          disabled={disabled}
          onClick={onOpenImageDialog}
          className="inline-flex h-7 w-7 cursor-pointer items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-muted hover:text-foreground focus-visible:bg-muted focus-visible:outline-none"
          aria-label="Insert or Upload Image"
        >
          <ImageIcon className="h-3.5 w-3.5" />
        </TooltipTrigger>
        <TooltipContent side="top" className="rounded-md text-xs">
          <div className="flex items-center gap-1.5 font-medium">
            <span>Insert / Upload Image</span>
            <Kbd className="h-4 px-1 font-mono text-[10px]">
              {formatForDisplay("Mod+Alt+I")}
            </Kbd>
          </div>
        </TooltipContent>
      </Tooltip>
    )
  }

  if (showSessionImages) {
    sections.push(
      <Tooltip key="session-images">
        <TooltipTrigger
          type="button"
          disabled={disabled}
          onClick={onOpenSessionImagesDialog}
          className="relative inline-flex h-7 cursor-pointer items-center gap-1.5 rounded-md px-2 text-xs font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground focus-visible:bg-muted focus-visible:outline-none"
          aria-label="Manage Uploaded Images"
        >
          <Images className="h-3.5 w-3.5" />
          <span className="hidden text-[11px] sm:inline">Images</span>
          {sessionImageCount > 0 && (
            <Badge
              variant="secondary"
              className="h-4 min-w-4 border-primary/20 bg-primary/10 px-1 text-[10px] font-bold text-primary"
            >
              {sessionImageCount}
            </Badge>
          )}
        </TooltipTrigger>
        <TooltipContent side="top" className="rounded-md text-xs">
          <span>Manage Tracked Images ({sessionImageCount})</span>
        </TooltipContent>
      </Tooltip>
    )
  }

  if (sections.length === 0) {
    return null
  }

  return (
    <div
      className={cn(
        "flex flex-wrap items-center justify-between gap-1 border-b border-border/80 bg-muted/40 px-2 py-1 select-none",
        className
      )}
    >
      <div className="flex flex-wrap items-center gap-0.5">
        {sections.map((section, idx) => (
          <React.Fragment key={idx}>
            {idx > 0 && (
              <Separator
                orientation="vertical"
                className="mx-1 h-4 bg-border/60"
              />
            )}
            {section}
          </React.Fragment>
        ))}
      </div>
    </div>
  )
}
