import * as React from "react"
import axios from "axios"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Spinner } from "@/components/ui/spinner"
import { ScrollArea } from "@/components/ui/scroll-area"
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip"
import {
  Images,
  Trash2,
  Plus,
  ExternalLink,
  CheckCircle,
  AlertCircle,
} from "lucide-react"
import { toast } from "@/components/ui/toast"
import { fileService } from "@/services/file-service"
import { syncImageReferences } from "../utils/image-tracker"
import type { UploadedImage } from "../types"

export interface SessionImagesDialogProps {
  isOpen: boolean
  onClose: () => void
  content: string
  images: UploadedImage[]
  onInsertImage: (imageMarkdown: string) => void
  onDeleteImage: (fileId: string) => void
  onPurgeUnused?: () => Promise<void>
}

export function SessionImagesDialog({
  isOpen,
  onClose,
  content,
  images,
  onInsertImage,
  onDeleteImage,
  onPurgeUnused,
}: SessionImagesDialogProps) {
  const [deletingId, setDeletingId] = React.useState<string | null>(null)
  const [isPurging, setIsPurging] = React.useState(false)

  const { referencedImages, unusedImages } = React.useMemo(
    () => syncImageReferences(content, images),
    [content, images]
  )

  const referencedIdSet = React.useMemo(
    () => new Set(referencedImages.map((i) => i.fileId)),
    [referencedImages]
  )

  const handleDelete = async (image: UploadedImage) => {
    setDeletingId(image.fileId)
    try {
      await fileService.deleteFile(image.fileId)
      onDeleteImage(image.fileId)
      toast.add({
        title: "Image Deleted",
        description: `${image.originalFileName} was permanently deleted.`,
        type: "success",
      })
    } catch (err: unknown) {
      console.error("Failed to delete image:", err)
      let message = "Failed to delete file from storage."
      if (axios.isAxiosError(err) && err.response?.data?.message) {
        message = err.response.data.message
      } else if (err instanceof Error) {
        message = err.message
      }
      toast.add({
        title: "Delete Failed",
        description: message,
        type: "error",
      })
    } finally {
      setDeletingId(null)
    }
  }

  const handlePurgeUnused = async () => {
    if (unusedImages.length === 0) return
    setIsPurging(true)
    try {
      if (onPurgeUnused) {
        await onPurgeUnused()
      } else {
        for (const img of unusedImages) {
          try {
            await fileService.deleteFile(img.fileId)
            onDeleteImage(img.fileId)
          } catch (e) {
            console.error(e)
          }
        }
      }
      toast.add({
        title: "Unused Images Cleaned Up",
        description: `Removed ${unusedImages.length} unused temporary image(s).`,
        type: "success",
      })
    } catch (err: unknown) {
      console.error("Failed to purge unused images:", err)
      toast.add({
        title: "Cleanup Incomplete",
        description: "Some images could not be removed.",
        type: "error",
      })
    } finally {
      setIsPurging(false)
    }
  }

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="flex max-h-[85vh] flex-col sm:max-w-2xl">
        <DialogHeader>
          <div className="flex items-center justify-between pr-6">
            <DialogTitle className="flex items-center gap-2 text-sm font-semibold">
              <Images className="h-4 w-4 text-primary" />
              Session Images ({images.length})
            </DialogTitle>
            {unusedImages.length > 0 && (
              <Button
                variant="outline"
                size="sm"
                onClick={handlePurgeUnused}
                disabled={isPurging}
                className="h-7 gap-1.5 text-xs text-destructive hover:bg-destructive/10"
              >
                {isPurging && <Spinner className="h-3 w-3" />}
                Purge Unused ({unusedImages.length})
              </Button>
            )}
          </div>
        </DialogHeader>

        <ScrollArea className="max-h-[60vh] py-2 pr-2">
          {images.length === 0 ? (
            <div className="py-12 text-center text-muted-foreground">
              <Images className="mx-auto mb-2 size-10 opacity-30" />
              <p className="text-xs font-medium">
                No images uploaded in this session yet.
              </p>
              <p className="mt-1 text-[11px] text-muted-foreground">
                Upload or drop images into the editor to track them here.
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2">
              {images.map((image) => {
                const isReferenced = referencedIdSet.has(image.fileId)
                const isDeleting = deletingId === image.fileId

                return (
                  <div
                    key={image.fileId}
                    className="flex flex-col space-y-2 rounded-lg border border-border bg-card p-3 shadow-xs"
                  >
                    <div className="flex items-center gap-3">
                      <Tooltip>
                        <TooltipTrigger
                          render={(triggerProps) => (
                            <div
                              {...triggerProps}
                              className="relative flex size-14 shrink-0 cursor-pointer items-center justify-center overflow-hidden rounded-md border border-border bg-muted/40"
                              onClick={() => window.open(image.url, "_blank")}
                            >
                              <img
                                src={image.url}
                                alt={image.originalFileName}
                                className="size-full object-cover"
                                onError={(e) => {
                                  ;(e.target as HTMLElement).style.display =
                                    "none"
                                }}
                              />
                            </div>
                          )}
                        />
                        <TooltipContent side="top" className="text-xs">
                          <span>{image.originalFileName} (Click to open)</span>
                        </TooltipContent>
                      </Tooltip>
                      <div className="min-w-0 flex-1 space-y-0.5">
                        <p
                          className="truncate text-xs font-semibold text-foreground"
                          title={image.originalFileName}
                        >
                          {image.originalFileName}
                        </p>
                        <p className="text-[10px] text-muted-foreground">
                          {(image.size / 1024).toFixed(1)} KB &bull;{" "}
                          {image.contentType.replace("image/", "")}
                        </p>
                        <div className="flex items-center gap-1.5 pt-0.5">
                          <Badge
                            variant={isReferenced ? "default" : "secondary"}
                            className="h-4 px-1.5 py-0 text-[9px] font-medium"
                          >
                            {isReferenced ? (
                              <span className="flex items-center gap-1">
                                <CheckCircle className="h-2.5 w-2.5 text-emerald-400" />
                                In Use
                              </span>
                            ) : (
                              <span className="flex items-center gap-1 text-muted-foreground">
                                <AlertCircle className="h-2.5 w-2.5" />
                                Unused
                              </span>
                            )}
                          </Badge>
                          <Badge
                            variant="outline"
                            className={`h-4 px-1.5 py-0 font-mono text-[9px] ${
                              image.isPermanent
                                ? "border-emerald-500/30 text-emerald-500"
                                : "border-amber-500/30 text-amber-500"
                            }`}
                          >
                            {image.isPermanent ? "Permanent" : "Temporary"}
                          </Badge>
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center justify-between border-t border-border/50 pt-1 text-xs">
                      <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        onClick={() => {
                          const markdown = `![${image.originalFileName}](${image.url})`
                          onInsertImage(markdown)
                          toast.add({
                            title: "Image Inserted",
                            description: `Inserted ${image.originalFileName} into editor.`,
                            type: "success",
                          })
                        }}
                        className="h-7 gap-1 px-2 text-xs text-primary hover:text-primary"
                      >
                        <Plus className="h-3 w-3" /> Insert
                      </Button>

                      <div className="flex items-center gap-1">
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon"
                          className="h-7 w-7 text-muted-foreground hover:text-foreground"
                          onClick={() => window.open(image.url, "_blank")}
                          title="Open full image"
                        >
                          <ExternalLink className="h-3 w-3" />
                        </Button>
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon"
                          disabled={isDeleting}
                          className="h-7 w-7 text-destructive hover:bg-destructive/10"
                          onClick={() => handleDelete(image)}
                          title="Delete image"
                        >
                          {isDeleting ? (
                            <Spinner className="h-3 w-3" />
                          ) : (
                            <Trash2 className="h-3 w-3" />
                          )}
                        </Button>
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </ScrollArea>

        <DialogFooter className="border-t border-border/60 pt-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={onClose}
            className="text-xs"
          >
            Close
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
