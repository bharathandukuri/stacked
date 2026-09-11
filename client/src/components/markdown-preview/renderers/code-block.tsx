import * as React from "react"
import hljs from "highlight.js"
import { cn } from "@/lib/utils"

export interface CodeBlockProps {
  children?: React.ReactNode
  className?: string
  inline?: boolean
}

export function CodeBlock({
  children,
  className,
  inline = false,
}: CodeBlockProps) {
  const codeRef = React.useRef<HTMLElement>(null)

  const match = /language-(\w+)/.exec(className || "")
  const language = match ? match[1] : ""
  const codeString = String(children).replace(/\n$/, "")

  React.useEffect(() => {
    if (codeRef.current && !inline) {
      hljs.highlightElement(codeRef.current)
    }
  }, [codeString, inline, language])

  if (inline) {
    return (
      <code className="rounded-md border border-border/80 bg-muted/60 px-1.5 py-0.5 font-mono text-[12px] font-medium text-foreground">
        {children}
      </code>
    )
  }

  return (
    <div className="my-3.5 w-full overflow-hidden rounded-lg border border-border bg-muted/20 shadow-xs dark:bg-muted/10">
      {/* Code Header */}
      <div className="flex h-7 items-center border-b border-border/70 bg-muted/50 px-3">
        <span className="font-mono text-[11px] font-semibold tracking-wider text-muted-foreground uppercase">
          {language || "code"}
        </span>
      </div>

      {/* Code Body */}
      <div className="overflow-x-auto p-3.5 text-xs leading-relaxed">
        <pre className="m-0 p-0">
          <code
            ref={codeRef}
            className={cn(
              "hljs rounded-md bg-transparent p-0 font-mono text-foreground",
              language ? `language-${language}` : ""
            )}
          >
            {codeString}
          </code>
        </pre>
      </div>
    </div>
  )
}
