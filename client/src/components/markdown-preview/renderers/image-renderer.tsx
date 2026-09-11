import * as React from "react"
import { cn } from "@/lib/utils"
import { ImageOff, Loader2, ZoomIn } from "lucide-react"
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog"
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip"

export interface ImageRendererProps extends React.ImgHTMLAttributes<HTMLImageElement> {
  src?: string
  alt?: string
  className?: string
}

export function ImageRenderer({
  src,
  alt = "Illustration",
  className,
  ...props
}: ImageRendererProps) {
  const [isLoading, setIsLoading] = React.useState(true)
  const [hasError, setHasError] = React.useState(false)
  const [isZoomed, setIsZoomed] = React.useState(false)
  const [prevSrc, setPrevSrc] = React.useState(src)
  const imgRef = React.useRef<HTMLImageElement>(null)

  if (prevSrc !== src) {
    setPrevSrc(src)
    setIsLoading(true)
    setHasError(false)
  }

  React.useEffect(() => {
    if (imgRef.current?.complete && imgRef.current.naturalWidth > 0) {
      setIsLoading(false)
    }
  }, [src])

  if (!src) {
    return null
  }

  if (hasError) {
    return (
      <span
        className={cn(
          "my-2 inline-flex items-center gap-2 rounded-md border border-border bg-muted/40 px-3 py-2 text-xs text-muted-foreground select-none",
          className
        )}
      >
        <ImageOff className="size-4 text-muted-foreground/80" />
        <span>Failed to load image: {alt}</span>
      </span>
    )
  }

  return (
    <>
      <Tooltip>
        <TooltipTrigger
          render={(triggerProps) => (
            <button
              type="button"
              {...triggerProps}
              onClick={() => setIsZoomed(true)}
              aria-label={`View full-size image: ${alt}`}
              className="group relative my-2 inline-block max-w-full cursor-zoom-in rounded-md border-0 bg-transparent p-0 text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-primary/60"
            >
              {isLoading && (
                <span className="absolute inset-0 flex items-center justify-center rounded-md border border-border/60 bg-muted/30 p-4">
                  <Loader2 className="size-4 animate-spin text-muted-foreground" />
                </span>
              )}
              <img
                ref={imgRef}
                src={src}
                alt={alt}
                onLoad={() => {
                  setIsLoading(false)
                  setHasError(false)
                }}
                onError={() => {
                  setIsLoading(false)
                  setHasError(true)
                }}
                className={cn(
                  "inline-block h-auto max-h-[520px] max-w-full rounded-md border border-border bg-muted/20 object-contain shadow-xs transition-opacity duration-200",
                  isLoading ? "opacity-40" : "opacity-100",
                  className
                )}
                {...props}
              />
              {!isLoading && (
                <span className="absolute right-2 bottom-2 hidden rounded bg-background/80 p-1 text-muted-foreground shadow-xs group-hover:flex">
                  <ZoomIn className="size-3.5" />
                </span>
              )}
            </button>
          )}
        />
        <TooltipContent side="top" className="text-xs">
          <span>Click to expand &bull; {alt}</span>
        </TooltipContent>
      </Tooltip>

      {/* Lightbox Zoom Dialog */}
      <Dialog open={isZoomed} onOpenChange={setIsZoomed}>
        <DialogContent className="flex max-w-4xl flex-col items-center justify-center border-0 bg-transparent p-2 shadow-none focus:outline-none">
          <DialogTitle className="sr-only">
            {alt ? `Full-size image view: ${alt}` : "Full-size image preview"}
          </DialogTitle>
          <img
            src={src}
            alt={alt}
            className="max-h-[85vh] max-w-full rounded-lg border border-border bg-background/95 object-contain p-2 shadow-2xl"
          />
          {alt && (
            <p className="mt-2 text-center text-xs font-medium text-muted-foreground">
              {alt}
            </p>
          )}
        </DialogContent>
      </Dialog>
    </>
  )
}
