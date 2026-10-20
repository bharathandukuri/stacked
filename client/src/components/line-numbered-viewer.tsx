import * as React from "react";
import { Copy, Check } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { cn } from "@/lib/utils";

export interface LineNumberedViewerProps {
  content?: string | null;
  language?: string;
  maxHeight?: string | number;
  minHeight?: string | number;
  className?: string;
  showCopy?: boolean;
  showLineNumbers?: boolean;
  emptyText?: string;
  variant?: "default" | "error" | "muted";
  wrapLines?: boolean;
}

export function LineNumberedViewer({
  content,
  language,
  maxHeight = "280px",
  minHeight = "60px",
  className,
  showCopy = true,
  showLineNumbers = true,
  emptyText = "(Empty)",
  variant = "default",
  wrapLines = true,
}: LineNumberedViewerProps) {
  const [copied, setCopied] = React.useState(false);

  const rawText = content ?? "";
  const lines = React.useMemo(() => {
    if (!rawText) return [];
    return rawText.split("\n");
  }, [rawText]);

  const handleCopy = React.useCallback(async () => {
    if (!rawText) return;
    try {
      await navigator.clipboard.writeText(rawText);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // ignore
    }
  }, [rawText]);

  if (!rawText) {
    return (
      <div
        style={{
          minHeight: typeof minHeight === "number" ? `${minHeight}px` : minHeight,
        }}
        className={cn(
          "flex items-center justify-center rounded-lg border border-border/70 bg-card/60 p-4 font-mono text-xs text-muted-foreground italic select-none",
          className
        )}
      >
        {emptyText}
      </div>
    );
  }

  return (
    <div
      data-language={language}
      style={{
        maxHeight: typeof maxHeight === "number" ? `${maxHeight}px` : maxHeight,
        minHeight: typeof minHeight === "number" ? `${minHeight}px` : minHeight,
      }}
      className={cn(
        "group relative flex w-full min-h-0 flex-col overflow-hidden rounded-lg border border-border/80 bg-background font-mono text-xs leading-5",
        variant === "error" && "border-destructive/40 bg-destructive/5 text-destructive",
        className
      )}
    >
      {/* Quick Copy Action in Top Right */}
      {showCopy && (
        <div className="absolute top-2 right-2 z-10 opacity-0 transition-opacity group-hover:opacity-100 focus-within:opacity-100">
          <Tooltip>
            <TooltipTrigger
              render={
                <Button
                  type="button"
                  variant="outline"
                  size="icon-xs"
                  onClick={handleCopy}
                  className="size-6 cursor-pointer border-border/80 bg-background/90 text-muted-foreground shadow-2xs backdrop-blur-xs hover:text-foreground"
                >
                  {copied ? (
                    <Check className="size-3 text-emerald-500" />
                  ) : (
                    <Copy className="size-3" />
                  )}
                </Button>
              }
            />
            <TooltipContent side="left">
              {copied ? "Copied!" : "Copy to Clipboard"}
            </TooltipContent>
          </Tooltip>
        </div>
      )}

      {/* Line-numbered content scroll container */}
      <div className="min-h-0 w-full flex-1 overflow-auto p-2">
        <table className="w-full border-collapse">
          <tbody>
            {lines.map((lineText, idx) => (
              <tr key={idx} className="hover:bg-muted/30">
                {/* Gutter: Line Number */}
                {showLineNumbers && (
                  <td className="w-10 shrink-0 pr-3 text-right font-mono text-[11px] select-none text-muted-foreground/60 align-top">
                    {idx + 1}
                  </td>
                )}
                {/* Content: Code / Output Line */}
                <td
                  className={cn(
                    "font-mono text-xs align-top",
                    wrapLines ? "whitespace-pre-wrap break-all" : "whitespace-pre",
                    variant === "error" ? "text-destructive" : "text-foreground"
                  )}
                >
                  {lineText || "\u00A0"}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default LineNumberedViewer;
