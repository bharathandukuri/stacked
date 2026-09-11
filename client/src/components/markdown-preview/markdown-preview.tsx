import * as React from "react"
import ReactMarkdown from "react-markdown"
import remarkGfm from "remark-gfm"
import remarkMath from "remark-math"
import rehypeKatex from "rehype-katex"
import rehypeRaw from "rehype-raw"
import "katex/dist/katex.min.css"
import { cn } from "@/lib/utils"
import {
  CodeBlock,
  TableRenderer,
  TableHeaderRenderer,
  TableRowRenderer,
  TableHeadCellRenderer,
  TableCellRenderer,
  ImageRenderer,
} from "./renderers"
import {
  Info,
  CheckCircle2,
  AlertTriangle,
  AlertOctagon,
  Flame,
} from "lucide-react"
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
  TooltipProvider,
} from "@/components/ui/tooltip"
import type { MarkdownPreviewProps } from "./types"

export function MarkdownPreview({
  value,
  content,
  className,
  emptyMessage = "No content to preview.",
  allowHtml = true,
  bordered = false,
}: MarkdownPreviewProps) {
  const rawText = (value ?? content ?? "").trim()

  const remarkPlugins = React.useMemo(() => [remarkGfm, remarkMath], [])
  const rehypePlugins = React.useMemo(() => {
    return allowHtml ? [rehypeRaw, rehypeKatex] : [rehypeKatex]
  }, [allowHtml])

  if (!rawText) {
    return (
      <div
        className={cn(
          "flex h-full min-h-32 items-center justify-center p-6 text-center text-xs text-muted-foreground italic select-none",
          bordered && "rounded-lg border border-dashed border-border/80",
          className
        )}
      >
        {emptyMessage}
      </div>
    )
  }

  return (
    <TooltipProvider>
      <div
        tabIndex={0}
        role="region"
        aria-label="Markdown preview"
        className={cn(
          "markdown-preview h-full w-full overflow-y-auto text-sm leading-relaxed text-foreground select-text focus:outline-none focus-visible:ring-1 focus-visible:ring-ring/30",
          bordered && "rounded-lg border border-border bg-card p-4",
          className
        )}
      >
        <ReactMarkdown
          remarkPlugins={remarkPlugins}
          rehypePlugins={rehypePlugins}
          components={{
            code({ className, children, ...props }) {
              const isInline = !className && !String(children).includes("\n")
              return (
                <CodeBlock className={className} inline={isInline} {...props}>
                  {children}
                </CodeBlock>
              )
            },
            pre({ children }) {
              return <div className="my-3.5 w-full">{children}</div>
            },

            table: TableRenderer,
            thead: TableHeaderRenderer,
            tr: TableRowRenderer,
            th: TableHeadCellRenderer,
            td: TableCellRenderer,
            img: ImageRenderer,

            blockquote({ children, className, ...props }) {
              // Check for GitHub-style alerts: > [!NOTE], > [!TIP], > [!WARNING], > [!IMPORTANT], > [!CAUTION]
              const childrenArray = React.Children.toArray(children)
              const firstChild = childrenArray[0]
              let firstLine = ""
              if (
                React.isValidElement<{ children?: React.ReactNode }>(firstChild)
              ) {
                const childProps = firstChild.props
                if (Array.isArray(childProps.children)) {
                  firstLine =
                    typeof childProps.children[0] === "string"
                      ? childProps.children[0]
                      : ""
                } else if (typeof childProps.children === "string") {
                  firstLine = childProps.children
                }
              }
              const alertMatch =
                /^\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)\]/i.exec(
                  firstLine.trim()
                )

              if (alertMatch) {
                const alertType = alertMatch[1].toUpperCase()
                const config: Record<
                  string,
                  {
                    icon: React.ReactNode
                    border: string
                    bg: string
                    title: string
                  }
                > = {
                  NOTE: {
                    icon: <Info className="h-4 w-4 text-blue-500" />,
                    border: "border-l-blue-500",
                    bg: "bg-blue-500/10",
                    title: "Note",
                  },
                  TIP: {
                    icon: <CheckCircle2 className="h-4 w-4 text-emerald-500" />,
                    border: "border-l-emerald-500",
                    bg: "bg-emerald-500/10",
                    title: "Tip",
                  },
                  IMPORTANT: {
                    icon: <Flame className="h-4 w-4 text-purple-500" />,
                    border: "border-l-purple-500",
                    bg: "bg-purple-500/10",
                    title: "Important",
                  },
                  WARNING: {
                    icon: <AlertTriangle className="h-4 w-4 text-amber-500" />,
                    border: "border-l-amber-500",
                    bg: "bg-amber-500/10",
                    title: "Warning",
                  },
                  CAUTION: {
                    icon: <AlertOctagon className="h-4 w-4 text-destructive" />,
                    border: "border-l-destructive",
                    bg: "bg-destructive/10",
                    title: "Caution",
                  },
                }

                const alertStyle = config[alertType] || config.NOTE

                return (
                  <div
                    className={cn(
                      "my-3.5 rounded-r-lg border-l-4 p-3 text-xs leading-relaxed",
                      alertStyle.border,
                      alertStyle.bg
                    )}
                  >
                    <div className="mb-1 flex items-center gap-1.5 font-bold">
                      {alertStyle.icon}
                      <span>{alertStyle.title}</span>
                    </div>
                    <div className="text-foreground/90">{children}</div>
                  </div>
                )
              }

              return (
                <blockquote
                  className={cn(
                    "my-3.5 border-l-2 border-primary/60 bg-muted/30 px-3.5 py-2 text-sm text-muted-foreground italic",
                    className
                  )}
                  {...props}
                >
                  {children}
                </blockquote>
              )
            },

            h1: ({ children, className, ...props }) => (
              <h1
                className={cn(
                  "mt-6 mb-3 text-xl font-bold tracking-tight text-foreground first:mt-0",
                  className
                )}
                {...props}
              >
                {children}
              </h1>
            ),
            h2: ({ children, className, ...props }) => (
              <h2
                className={cn(
                  "mt-5 mb-2.5 text-lg font-bold tracking-tight text-foreground first:mt-0",
                  className
                )}
                {...props}
              >
                {children}
              </h2>
            ),
            h3: ({ children, className, ...props }) => (
              <h3
                className={cn(
                  "mt-4 mb-2 text-base font-bold tracking-tight text-foreground first:mt-0",
                  className
                )}
                {...props}
              >
                {children}
              </h3>
            ),
            h4: ({ children, className, ...props }) => (
              <h4
                className={cn(
                  "mt-3 mb-1.5 text-sm font-bold text-foreground first:mt-0",
                  className
                )}
                {...props}
              >
                {children}
              </h4>
            ),
            p: ({ children, className, ...props }) => (
              <p
                className={cn(
                  "mb-3.5 text-sm leading-relaxed text-foreground/90 last:mb-0",
                  className
                )}
                {...props}
              >
                {children}
              </p>
            ),
            ul: ({ children, className, ...props }) => (
              <ul
                className={cn(
                  "my-2.5 list-disc space-y-1.5 pl-5 text-sm leading-relaxed text-foreground/90",
                  className
                )}
                {...props}
              >
                {children}
              </ul>
            ),
            ol: ({ children, className, ...props }) => (
              <ol
                className={cn(
                  "my-2.5 list-decimal space-y-1.5 pl-5 text-sm leading-relaxed text-foreground/90",
                  className
                )}
                {...props}
              >
                {children}
              </ol>
            ),
            a: ({ children, href, title, className, ...props }) => {
              const isExternal =
                href?.startsWith("http://") || href?.startsWith("https://")
              const tooltipText = title || href

              const linkElement = (
                <a
                  href={href}
                  target={isExternal ? "_blank" : undefined}
                  rel={isExternal ? "noopener noreferrer" : undefined}
                  className={cn(
                    "font-semibold text-primary underline underline-offset-2 transition-colors hover:text-primary/80",
                    className
                  )}
                  {...props}
                >
                  {children}
                </a>
              )

              if (!tooltipText) {
                return linkElement
              }

              return (
                <Tooltip>
                  <TooltipTrigger
                    render={(triggerProps) => (
                      <a
                        {...triggerProps}
                        href={href}
                        target={isExternal ? "_blank" : undefined}
                        rel={isExternal ? "noopener noreferrer" : undefined}
                        className={cn(
                          "font-semibold text-primary underline underline-offset-2 transition-colors hover:text-primary/80",
                          className
                        )}
                        {...props}
                      >
                        {children}
                      </a>
                    )}
                  />
                  <TooltipContent
                    side="top"
                    className="max-w-xs text-xs break-all"
                  >
                    {title ? (
                      <div className="flex flex-col gap-0.5">
                        <span className="font-semibold">{title}</span>
                        {href && (
                          <span className="text-[10px] text-muted-foreground">
                            {href}
                          </span>
                        )}
                      </div>
                    ) : (
                      <span>{href}</span>
                    )}
                  </TooltipContent>
                </Tooltip>
              )
            },
            hr: ({ className, ...props }) => (
              <hr
                className={cn("my-5 border-border/80", className)}
                {...props}
              />
            ),
            em: ({ children, className, ...props }) => (
              <em
                className={cn("text-foreground italic", className)}
                {...props}
              >
                {children}
              </em>
            ),
            strong: ({ children, className, ...props }) => (
              <strong
                className={cn("font-bold text-foreground", className)}
                {...props}
              >
                {children}
              </strong>
            ),
          }}
        >
          {rawText}
        </ReactMarkdown>
      </div>
    </TooltipProvider>
  )
}
