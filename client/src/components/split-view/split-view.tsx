import {
  ResizablePanelGroup,
  ResizablePanel,
  ResizableHandle,
} from "@/components/ui/resizable"
import { cn } from "@/lib/utils"
import type { SplitViewProps } from "./types"

export function SplitView({
  firstPane,
  secondPane,
  orientation = "horizontal",
  defaultSize = 50,
  minSize = 20,
  maxSize = 80,
  withHandle = true,
  collapsibleFirst = false,
  collapsibleSecond = false,
  onResize,
  className,
  firstPaneClassName,
  secondPaneClassName,
}: SplitViewProps) {
  const secondPanelDefaultSize = Math.max(0, 100 - defaultSize)

  return (
    <div
      className={cn("relative flex h-full w-full overflow-hidden", className)}
    >
      <ResizablePanelGroup
        orientation={orientation}
        className="h-full w-full"
        onLayoutChanged={
          onResize
            ? (layout) => {
                const s1 = layout["first-pane"] ?? 50
                const s2 = layout["second-pane"] ?? 100 - s1
                onResize([s1, s2])
              }
            : undefined
        }
      >
        <ResizablePanel
          id="first-pane"
          defaultSize={`${defaultSize}%`}
          minSize={`${minSize}%`}
          maxSize={`${maxSize}%`}
          collapsible={collapsibleFirst}
          className={cn("h-full min-h-0 w-full min-w-0", firstPaneClassName)}
        >
          {firstPane}
        </ResizablePanel>

        <ResizableHandle withHandle={withHandle} />

        <ResizablePanel
          id="second-pane"
          defaultSize={`${secondPanelDefaultSize}%`}
          minSize={`${minSize}%`}
          maxSize={`${maxSize}%`}
          collapsible={collapsibleSecond}
          className={cn("h-full min-h-0 w-full min-w-0", secondPaneClassName)}
        >
          {secondPane}
        </ResizablePanel>
      </ResizablePanelGroup>
    </div>
  )
}
