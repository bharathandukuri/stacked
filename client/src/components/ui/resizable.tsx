"use client"

import { cn } from "cn"
import * as ResizablePrimitive from "react-resizable-panels"

function ResizablePanelGroup({
  className,
  ...props
}: ResizablePrimitive.GroupProps) {
  return (
    <ResizablePrimitive.Group
      data-slot="resizable-panel-group"
      className={cn(
        "flex h-full w-full aria-[orientation=vertical]:flex-col",
        className
      )}
      {...props}
    />
  )
}

function ResizablePanel({ ...props }: ResizablePrimitive.PanelProps) {
  return <ResizablePrimitive.Panel data-slot="resizable-panel" {...props} />
}

function ResizableHandle({
  withHandle,
  className,
  ...props
}: ResizablePrimitive.SeparatorProps & {
  withHandle?: boolean
}) {
  return (
    <ResizablePrimitive.Separator
      data-slot="resizable-handle"
      className={cn(
        "group relative flex items-center justify-center bg-border transition-colors select-none focus-visible:ring-1 focus-visible:ring-ring focus-visible:outline-hidden",
        "hover:bg-primary/60 data-[separator=active]:bg-primary",
        // Vertical separator (Group is horizontal: panels are side-by-side)
        "aria-[orientation=vertical]:w-1.5 aria-[orientation=vertical]:cursor-col-resize aria-[orientation=vertical]:after:absolute aria-[orientation=vertical]:after:inset-y-0 aria-[orientation=vertical]:after:-left-2 aria-[orientation=vertical]:after:z-10 aria-[orientation=vertical]:after:w-5",
        // Horizontal separator (Group is vertical: panels are stacked)
        "aria-[orientation=horizontal]:h-1.5 aria-[orientation=horizontal]:w-full aria-[orientation=horizontal]:cursor-row-resize aria-[orientation=horizontal]:after:absolute aria-[orientation=horizontal]:after:inset-x-0 aria-[orientation=horizontal]:after:-top-2 aria-[orientation=horizontal]:after:z-10 aria-[orientation=horizontal]:after:h-5",
        className
      )}
      {...props}
    >
      {withHandle && (
        <div className="z-10 flex shrink-0 rounded-full bg-muted-foreground/40 transition-colors group-hover:bg-primary group-data-[separator=active]:bg-primary aria-[orientation=horizontal]:h-1 aria-[orientation=horizontal]:w-6 aria-[orientation=vertical]:h-6 aria-[orientation=vertical]:w-1" />
      )}
    </ResizablePrimitive.Separator>
  )
}

export { ResizableHandle, ResizablePanel, ResizablePanelGroup }
