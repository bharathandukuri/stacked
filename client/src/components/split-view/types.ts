import type * as React from "react"

export interface SplitViewProps {
  /** First pane element (left pane in horizontal, top pane in vertical) */
  firstPane: React.ReactNode
  /** Second pane element (right pane in horizontal, bottom pane in vertical) */
  secondPane: React.ReactNode
  /** Split orientation (default: "horizontal") */
  orientation?: "horizontal" | "vertical"
  /** Default percentage size for the first panel (default: 50) */
  defaultSize?: number
  /** Minimum percentage size for panels (default: 20) */
  minSize?: number
  /** Maximum percentage size for panels (default: 80) */
  maxSize?: number
  /** Shows the visual grip indicator in the divider handle (default: true) */
  withHandle?: boolean
  /** Whether the first panel can be collapsed (default: false) */
  collapsibleFirst?: boolean
  /** Whether the second panel can be collapsed (default: false) */
  collapsibleSecond?: boolean
  /** Callback fired when panels are resized */
  onResize?: (sizes: number[]) => void
  /** Additional container classes */
  className?: string
  /** Additional classes for the first panel container */
  firstPaneClassName?: string
  /** Additional classes for the second panel container */
  secondPaneClassName?: string
}
