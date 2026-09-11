import * as React from "react"
import { cn } from "@/lib/utils"

export interface TableRendererProps extends React.TableHTMLAttributes<HTMLTableElement> {
  children?: React.ReactNode
  className?: string
}

export function TableRenderer({
  children,
  className,
  ...props
}: TableRendererProps) {
  return (
    <div className="my-4 w-full overflow-x-auto rounded-lg border border-border shadow-xs">
      <table
        className={cn(
          "w-full caption-bottom border-collapse font-sans text-xs",
          className
        )}
        {...props}
      >
        {children}
      </table>
    </div>
  )
}

export function TableHeaderRenderer({
  children,
  className,
  ...props
}: React.HTMLAttributes<HTMLTableSectionElement>) {
  return (
    <thead
      className={cn(
        "border-b border-border bg-muted/60 text-foreground",
        className
      )}
      {...props}
    >
      {children}
    </thead>
  )
}

export function TableRowRenderer({
  children,
  className,
  ...props
}: React.HTMLAttributes<HTMLTableRowElement>) {
  return (
    <tr
      className={cn(
        "border-b border-border/40 transition-colors last:border-b-0 hover:bg-muted/30 data-[state=selected]:bg-muted",
        className
      )}
      {...props}
    >
      {children}
    </tr>
  )
}

export function TableHeadCellRenderer({
  children,
  className,
  ...props
}: React.ThHTMLAttributes<HTMLTableCellElement>) {
  return (
    <th
      className={cn(
        "h-9 px-3 text-left align-middle text-xs font-bold text-foreground [&:has([role=checkbox])]:pr-0",
        className
      )}
      {...props}
    >
      {children}
    </th>
  )
}

export function TableCellRenderer({
  children,
  className,
  ...props
}: React.TdHTMLAttributes<HTMLTableCellElement>) {
  return (
    <td
      className={cn(
        "p-2.5 align-middle text-xs text-foreground/90 [&:has([role=checkbox])]:pr-0",
        className
      )}
      {...props}
    >
      {children}
    </td>
  )
}
