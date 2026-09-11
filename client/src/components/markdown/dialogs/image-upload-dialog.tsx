import * as React from "react"
import axios from "axios"
import { cn } from "@/lib/utils"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Badge } from "@/components/ui/badge"
import { Spinner } from "@/components/ui/spinner"
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip"
import {
  Upload,
  Image as ImageIcon,
  RotateCw,
  FlipHorizontal,
  FlipVertical,
  Crop as CropIcon,
  RefreshCw,
  Maximize2,
  Link as LinkIcon,
} from "lucide-react"
import { toast } from "@/components/ui/toast"
import { fileService } from "@/services/file-service"
import { createUploadedImageFromFileResponse } from "../utils/image-tracker"
import type { UploadedImage } from "../types"

export interface ImageInsertResult {
  markdown: string
  uploadedImage?: UploadedImage
}

export interface ImageUploadDialogProps {
  isOpen: boolean
  initialFile?: File | null
  onClose: () => void
  onSubmit: (result: ImageInsertResult) => void
}

type AspectPreset = "free" | "1:1" | "4:3" | "16:9"
type DragHandle = "move" | "nw" | "ne" | "se" | "sw" | "n" | "s" | "e" | "w"

interface CropBox {
  x: number
  y: number
  w: number
  h: number
}

interface ImageEditorContentProps {
  initialFile?: File | null
  onClose: () => void
  onSubmit: (result: ImageInsertResult) => void
  isUploading: boolean
  setIsUploading: (uploading: boolean) => void
}

// Canvas-based rotation: produces a new cleanly oriented image without CORS or matrix conflicts
const rotateImageOnCanvas = (
  src: string,
  degrees: 90 | 180 | 270
): Promise<{ url: string; width: number; height: number; blob: Blob }> => {
  return new Promise((resolve, reject) => {
    const img = new Image()
    if (src.startsWith("http://") || src.startsWith("https://")) {
      img.crossOrigin = "anonymous"
    }
    img.onload = () => {
      const canvas = document.createElement("canvas")
      const isQuarterTurn = degrees === 90 || degrees === 270
      canvas.width = isQuarterTurn ? img.naturalHeight : img.naturalWidth
      canvas.height = isQuarterTurn ? img.naturalWidth : img.naturalHeight

      const ctx = canvas.getContext("2d")
      if (!ctx) {
        reject(new Error("Canvas context unavailable"))
        return
      }

      ctx.translate(canvas.width / 2, canvas.height / 2)
      ctx.rotate((degrees * Math.PI) / 180)
      ctx.drawImage(img, -img.naturalWidth / 2, -img.naturalHeight / 2)

      canvas.toBlob((blob) => {
        if (!blob) {
          try {
            const dataUrl = canvas.toDataURL("image/png")
            resolve({
              url: dataUrl,
              width: canvas.width,
              height: canvas.height,
              blob: new Blob([], { type: "image/png" }),
            })
          } catch (e) {
            reject(e)
          }
          return
        }
        const url = URL.createObjectURL(blob)
        resolve({
          url,
          width: canvas.width,
          height: canvas.height,
          blob,
        })
      }, "image/png")
    }
    img.onerror = (err) => reject(err)
    img.src = src
  })
}

// Canvas-based flip: directly flips pixels on canvas, keeping coordinates 1:1 with the crop box
const flipImageOnCanvas = (
  src: string,
  horizontal: boolean,
  vertical: boolean
): Promise<{ url: string; width: number; height: number; blob: Blob }> => {
  return new Promise((resolve, reject) => {
    const img = new Image()
    if (src.startsWith("http://") || src.startsWith("https://")) {
      img.crossOrigin = "anonymous"
    }
    img.onload = () => {
      const canvas = document.createElement("canvas")
      canvas.width = img.naturalWidth
      canvas.height = img.naturalHeight

      const ctx = canvas.getContext("2d")
      if (!ctx) {
        reject(new Error("Canvas context unavailable"))
        return
      }

      ctx.translate(canvas.width / 2, canvas.height / 2)
      ctx.scale(horizontal ? -1 : 1, vertical ? -1 : 1)
      ctx.drawImage(img, -img.naturalWidth / 2, -img.naturalHeight / 2)

      canvas.toBlob((blob) => {
        if (!blob) {
          try {
            const dataUrl = canvas.toDataURL("image/png")
            resolve({
              url: dataUrl,
              width: canvas.width,
              height: canvas.height,
              blob: new Blob([], { type: "image/png" }),
            })
          } catch (e) {
            reject(e)
          }
          return
        }
        const url = URL.createObjectURL(blob)
        resolve({
          url,
          width: canvas.width,
          height: canvas.height,
          blob,
        })
      }, "image/png")
    }
    img.onerror = (err) => reject(err)
    img.src = src
  })
}

function ImageEditorContent({
  initialFile,
  onClose,
  onSubmit,
  isUploading,
  setIsUploading,
}: ImageEditorContentProps) {
  const [activeTab, setActiveTab] = React.useState<"editor" | "url">("editor")

  // Original untouched image state for reset
  const [originalSrc, setOriginalSrc] = React.useState<string | null>(() => {
    if (initialFile && initialFile.type.startsWith("image/")) {
      return URL.createObjectURL(initialFile)
    }
    return null
  })
  const [originalWidth, setOriginalWidth] = React.useState(0)
  const [originalHeight, setOriginalHeight] = React.useState(0)

  // Current working image state (all rotations/flips update this directly)
  const [selectedFile, setSelectedFile] = React.useState<File | null>(() => {
    if (initialFile && initialFile.type.startsWith("image/")) {
      return initialFile
    }
    return null
  })
  const [imageSrc, setImageSrc] = React.useState<string | null>(originalSrc)
  const [imgWidth, setImgWidth] = React.useState(0)
  const [imgHeight, setImgHeight] = React.useState(0)
  const [isModified, setIsModified] = React.useState(false)

  // Markdown Options
  const [altText, setAltText] = React.useState(() => {
    if (initialFile && initialFile.type.startsWith("image/")) {
      return initialFile.name.replace(/\.[^/.]+$/, "")
    }
    return ""
  })

  // Crop State (Enabled by default!)
  const [cropBox, setCropBox] = React.useState<CropBox>({
    x: 0,
    y: 0,
    w: 100,
    h: 100,
  })
  const [cropAspectPreset, setCropAspectPreset] =
    React.useState<AspectPreset>("free")

  // Dragging for Crop Box
  const [isDragging, setIsDragging] = React.useState(false)
  const [dragType, setDragType] = React.useState<DragHandle | null>(null)
  const dragStartRef = React.useRef<{
    clientX: number
    clientY: number
    box: CropBox
  }>({
    clientX: 0,
    clientY: 0,
    box: { x: 0, y: 0, w: 100, h: 100 },
  })

  // URL Tab
  const [webUrl, setWebUrl] = React.useState("")

  // Refs
  const fileInputRef = React.useRef<HTMLInputElement>(null)
  const cropContainerRef = React.useRef<HTMLDivElement>(null)

  // Track created blob URLs to prevent memory leaks
  const createdBlobUrlsRef = React.useRef<Set<string>>(new Set())

  // Initial dimension load
  React.useEffect(() => {
    if (imageSrc) {
      const img = new Image()
      if (imageSrc.startsWith("http://") || imageSrc.startsWith("https://")) {
        img.crossOrigin = "anonymous"
      }
      img.onload = () => {
        setImgWidth(img.naturalWidth)
        setImgHeight(img.naturalHeight)
        if (originalWidth === 0) {
          setOriginalWidth(img.naturalWidth)
          setOriginalHeight(img.naturalHeight)
        }
      }
      img.src = imageSrc
    }
  }, [imageSrc, originalWidth])

  // Cleanup object URLs on unmount
  React.useEffect(() => {
    const urls = createdBlobUrlsRef.current
    return () => {
      if (originalSrc && originalSrc.startsWith("blob:")) {
        URL.revokeObjectURL(originalSrc)
      }
      urls.forEach((u) => {
        if (u.startsWith("blob:")) {
          URL.revokeObjectURL(u)
        }
      })
    }
  }, [originalSrc])

  const handleFileSelect = (file: File) => {
    if (!file.type.startsWith("image/")) {
      toast.add({
        title: "Invalid File Type",
        description: "Please select an image file (PNG, JPEG, WebP, GIF, SVG).",
        type: "error",
      })
      return
    }

    if (file.size > 10 * 1024 * 1024) {
      toast.add({
        title: "File Too Large",
        description: "Image size must be less than 10MB.",
        type: "error",
      })
      return
    }

    if (originalSrc && originalSrc.startsWith("blob:")) {
      URL.revokeObjectURL(originalSrc)
    }

    const url = URL.createObjectURL(file)
    setSelectedFile(file)
    setOriginalSrc(url)
    setImageSrc(url)
    setIsModified(false)
    setCropBox({ x: 0, y: 0, w: 100, h: 100 })
    setCropAspectPreset("free")

    const img = new Image()
    img.onload = () => {
      setImgWidth(img.naturalWidth)
      setImgHeight(img.naturalHeight)
      setOriginalWidth(img.naturalWidth)
      setOriginalHeight(img.naturalHeight)
    }
    img.src = url

    if (!altText) {
      setAltText(file.name.replace(/\.[^/.]+$/, ""))
    }
  }

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault()
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFileSelect(e.dataTransfer.files[0])
    }
  }

  // Helper to accurately set crop ratio based on actual pixel dimensions
  const calculatePresetBox = (
    preset: AspectPreset,
    width: number,
    height: number
  ): CropBox => {
    if (preset === "free") {
      return { x: 0, y: 0, w: 100, h: 100 }
    }

    const imgRatio = width / (height || 1)
    let targetRatio = 1
    if (preset === "4:3") targetRatio = 4 / 3
    if (preset === "16:9") targetRatio = 16 / 9

    const ratioFactor = imgRatio / targetRatio
    let w: number
    let h: number

    if (ratioFactor >= 1) {
      h = 80
      w = Math.min(96, Math.max(10, h / ratioFactor))
    } else {
      w = 80
      h = Math.min(96, Math.max(10, w * ratioFactor))
    }

    const x = Math.max(0, Math.round((100 - w) / 2))
    const y = Math.max(0, Math.round((100 - h) / 2))
    return { x, y, w, h }
  }

  // Rotate 90° Clockwise
  const handleRotate = async () => {
    if (!imageSrc) return
    try {
      const res = await rotateImageOnCanvas(imageSrc, 90)
      createdBlobUrlsRef.current.add(res.url)
      setImageSrc(res.url)
      setImgWidth(res.width)
      setImgHeight(res.height)
      setIsModified(true)

      // Transform crop box for 90° clockwise rotation
      if (cropAspectPreset === "free") {
        setCropBox((prev) => ({
          x: Math.max(0, Math.min(95, 100 - (prev.y + prev.h))),
          y: Math.max(0, Math.min(95, prev.x)),
          w: Math.max(5, Math.min(100, prev.h)),
          h: Math.max(5, Math.min(100, prev.w)),
        }))
      } else {
        const nextBox = calculatePresetBox(
          cropAspectPreset,
          res.width,
          res.height
        )
        setCropBox(nextBox)
      }
    } catch (err) {
      console.error("Rotate failed", err)
      toast.add({
        title: "Rotate Failed",
        description: "Could not rotate the image.",
        type: "error",
      })
    }
  }

  // Flip Horizontal
  const handleFlipH = async () => {
    if (!imageSrc) return
    try {
      const res = await flipImageOnCanvas(imageSrc, true, false)
      createdBlobUrlsRef.current.add(res.url)
      setImageSrc(res.url)
      setIsModified(true)

      setCropBox((prev) => ({
        ...prev,
        x: Math.max(0, Math.min(100 - prev.w, 100 - (prev.x + prev.w))),
      }))
    } catch (err) {
      console.error("Flip H failed", err)
      toast.add({
        title: "Flip Failed",
        description: "Could not flip the image horizontally.",
        type: "error",
      })
    }
  }

  // Flip Vertical
  const handleFlipV = async () => {
    if (!imageSrc) return
    try {
      const res = await flipImageOnCanvas(imageSrc, false, true)
      createdBlobUrlsRef.current.add(res.url)
      setImageSrc(res.url)
      setIsModified(true)

      setCropBox((prev) => ({
        ...prev,
        y: Math.max(0, Math.min(100 - prev.h, 100 - (prev.y + prev.h))),
      }))
    } catch (err) {
      console.error("Flip V failed", err)
      toast.add({
        title: "Flip Failed",
        description: "Could not flip the image vertically.",
        type: "error",
      })
    }
  }

  // Reset all modifications back to untouched original
  const handleResetAll = () => {
    if (!originalSrc) return
    setImageSrc(originalSrc)
    setImgWidth(originalWidth)
    setImgHeight(originalHeight)
    setCropBox({ x: 0, y: 0, w: 100, h: 100 })
    setCropAspectPreset("free")
    setIsModified(false)
    toast.add({
      title: "Reset to Original",
      description: "Restored original image orientation and crop area.",
      type: "info",
    })
  }

  const handleSetCropPreset = (preset: AspectPreset) => {
    setCropAspectPreset(preset)
    const nextBox = calculatePresetBox(preset, imgWidth, imgHeight)
    setCropBox(nextBox)
  }

  const handleResetCrop = () => {
    setCropAspectPreset("free")
    setCropBox({ x: 0, y: 0, w: 100, h: 100 })
  }

  // Pointer drag for Crop Box handles & move
  const handlePointerDown = (e: React.PointerEvent, type: DragHandle) => {
    e.preventDefault()
    e.stopPropagation()
    setIsDragging(true)
    setDragType(type)
    dragStartRef.current = {
      clientX: e.clientX,
      clientY: e.clientY,
      box: { ...cropBox },
    }
    ;(e.target as HTMLElement).setPointerCapture(e.pointerId)
  }

  const handlePointerMove = (e: React.PointerEvent) => {
    if (!isDragging || !dragType || !cropContainerRef.current) return
    e.preventDefault()

    const container = cropContainerRef.current
    const rect = container.getBoundingClientRect()
    if (rect.width === 0 || rect.height === 0) return

    const deltaX =
      ((e.clientX - dragStartRef.current.clientX) / rect.width) * 100
    const deltaY =
      ((e.clientY - dragStartRef.current.clientY) / rect.height) * 100
    const orig = dragStartRef.current.box

    const next = { ...orig }

    if (dragType === "move") {
      next.x = Math.max(0, Math.min(100 - orig.w, orig.x + deltaX))
      next.y = Math.max(0, Math.min(100 - orig.h, orig.y + deltaY))
    } else if (dragType === "se") {
      next.w = Math.max(5, Math.min(100 - orig.x, orig.w + deltaX))
      next.h = Math.max(5, Math.min(100 - orig.y, orig.h + deltaY))
    } else if (dragType === "nw") {
      const newX = Math.max(0, Math.min(orig.x + orig.w - 5, orig.x + deltaX))
      const newY = Math.max(0, Math.min(orig.y + orig.h - 5, orig.y + deltaY))
      next.w = orig.w + (orig.x - newX)
      next.h = orig.h + (orig.y - newY)
      next.x = newX
      next.y = newY
    } else if (dragType === "ne") {
      const newY = Math.max(0, Math.min(orig.y + orig.h - 5, orig.y + deltaY))
      next.w = Math.max(5, Math.min(100 - orig.x, orig.w + deltaX))
      next.h = orig.h + (orig.y - newY)
      next.y = newY
    } else if (dragType === "sw") {
      const newX = Math.max(0, Math.min(orig.x + orig.w - 5, orig.x + deltaX))
      next.w = orig.w + (orig.x - newX)
      next.h = Math.max(5, Math.min(100 - orig.y, orig.h + deltaY))
      next.x = newX
    } else if (dragType === "n") {
      const newY = Math.max(0, Math.min(orig.y + orig.h - 5, orig.y + deltaY))
      next.h = orig.h + (orig.y - newY)
      next.y = newY
    } else if (dragType === "s") {
      next.h = Math.max(5, Math.min(100 - orig.y, orig.h + deltaY))
    } else if (dragType === "w") {
      const newX = Math.max(0, Math.min(orig.x + orig.w - 5, orig.x + deltaX))
      next.w = orig.w + (orig.x - newX)
      next.x = newX
    } else if (dragType === "e") {
      next.w = Math.max(5, Math.min(100 - orig.x, orig.w + deltaX))
    }

    // Apply aspect ratio constraints if preset chosen
    if (cropAspectPreset !== "free") {
      let targetRatio = 1
      if (cropAspectPreset === "4:3") targetRatio = 4 / 3
      if (cropAspectPreset === "16:9") targetRatio = 16 / 9
      const ratioFactor = imgWidth / (imgHeight || 1) / targetRatio

      if (
        dragType === "se" ||
        dragType === "ne" ||
        dragType === "sw" ||
        dragType === "nw" ||
        dragType === "e" ||
        dragType === "w"
      ) {
        next.h = Math.min(100 - next.y, next.w * ratioFactor)
      } else if (dragType === "n" || dragType === "s") {
        next.w = Math.min(100 - next.x, next.h / ratioFactor)
      }
    }

    setCropBox(next)
  }

  const handlePointerUp = (e: React.PointerEvent) => {
    setIsDragging(false)
    setDragType(null)
    try {
      ;(e.target as HTMLElement).releasePointerCapture(e.pointerId)
    } catch {
      // ignored
    }
  }

  // Get the final cropped blob directly from the displayed oriented image
  const getCroppedBlob = async (): Promise<Blob> => {
    return new Promise((resolve, reject) => {
      if (!imageSrc) {
        reject(new Error("No image source available"))
        return
      }

      const img = new Image()
      if (imageSrc.startsWith("http://") || imageSrc.startsWith("https://")) {
        img.crossOrigin = "anonymous"
      }
      img.onload = () => {
        const isFull =
          cropBox.x <= 0.5 &&
          cropBox.y <= 0.5 &&
          cropBox.w >= 99.5 &&
          cropBox.h >= 99.5

        const cropX = isFull ? 0 : (cropBox.x / 100) * img.naturalWidth
        const cropY = isFull ? 0 : (cropBox.y / 100) * img.naturalHeight
        const cropW = isFull
          ? img.naturalWidth
          : (cropBox.w / 100) * img.naturalWidth
        const cropH = isFull
          ? img.naturalHeight
          : (cropBox.h / 100) * img.naturalHeight

        const canvas = document.createElement("canvas")
        canvas.width = Math.max(1, Math.round(cropW))
        canvas.height = Math.max(1, Math.round(cropH))

        const ctx = canvas.getContext("2d")
        if (!ctx) {
          reject(new Error("Canvas context unavailable"))
          return
        }

        ctx.drawImage(
          img,
          cropX,
          cropY,
          cropW,
          cropH,
          0,
          0,
          canvas.width,
          canvas.height
        )

        canvas.toBlob(
          (blob) => {
            if (blob) resolve(blob)
            else reject(new Error("Failed to generate image blob"))
          },
          "image/png",
          0.95
        )
      }
      img.onerror = (err) => reject(err)
      img.src = imageSrc
    })
  }

  const formatMarkdown = (url: string, alt: string) => {
    const cleanAlt = alt.trim() || "Illustration"
    return `![${cleanAlt}](${url})`
  }

  const handleUploadSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!imageSrc) {
      toast.add({
        title: "No Image",
        description: "Please select an image file first.",
        type: "error",
      })
      return
    }

    setIsUploading(true)
    try {
      const blob = await getCroppedBlob()

      const fileName = selectedFile
        ? selectedFile.name.replace(/\.[^/.]+$/, ".png")
        : "edited-image.png"
      const processedFile = new File([blob], fileName, { type: "image/png" })

      // Upload processed file to temporary storage
      const fileResponse = await fileService.uploadTemporary(processedFile)
      const trackedImage = createUploadedImageFromFileResponse(fileResponse)
      const markdown = formatMarkdown(trackedImage.url, altText)

      toast.add({
        title: "Image Uploaded",
        description: `${fileName} ready and inserted into editor.`,
        type: "success",
      })

      onSubmit({
        markdown,
        uploadedImage: trackedImage,
      })
      onClose()
    } catch (err: unknown) {
      console.error("Upload error:", err)
      let message = "Failed to upload image."
      if (axios.isAxiosError(err) && err.response?.data?.message) {
        message = err.response.data.message
      } else if (err instanceof Error) {
        message = err.message
      }
      toast.add({
        title: "Upload Failed",
        description: message,
        type: "error",
      })
    } finally {
      setIsUploading(false)
    }
  }

  const handleUrlSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const cleanUrl = webUrl.trim()
    if (!cleanUrl) {
      toast.add({
        title: "URL Required",
        description: "Please enter an image URL.",
        type: "error",
      })
      return
    }

    try {
      new URL(cleanUrl)
    } catch {
      toast.add({
        title: "Invalid URL",
        description: "Please enter a valid HTTP or HTTPS image URL.",
        type: "error",
      })
      return
    }

    const markdown = formatMarkdown(cleanUrl, altText)
    toast.add({
      title: "Image Inserted",
      type: "success",
    })

    onSubmit({ markdown })
    onClose()
  }

  const isCropActive =
    cropBox.x > 0.5 || cropBox.y > 0.5 || cropBox.w < 99.5 || cropBox.h < 99.5

  const cropPixelWidth = Math.round((cropBox.w / 100) * (imgWidth || 1))
  const cropPixelHeight = Math.round((cropBox.h / 100) * (imgHeight || 1))

  // Mode Switcher Pills
  const modeSwitcher = (
    <div className="mr-7 flex items-center gap-0.5 rounded-md border border-border/80 bg-muted/50 p-0.5">
      <button
        type="button"
        onClick={() => setActiveTab("editor")}
        className={cn(
          "cursor-pointer rounded px-2 py-0.5 text-[11px] font-medium transition-all",
          activeTab === "editor"
            ? "bg-background text-foreground shadow-xs"
            : "text-muted-foreground hover:text-foreground"
        )}
      >
        Upload / Edit
      </button>
      <button
        type="button"
        onClick={() => setActiveTab("url")}
        className={cn(
          "cursor-pointer rounded px-2 py-0.5 text-[11px] font-medium transition-all",
          activeTab === "url"
            ? "bg-background text-foreground shadow-xs"
            : "text-muted-foreground hover:text-foreground"
        )}
      >
        URL
      </button>
    </div>
  )

  // 1. URL MODE: Compact Modal (No blank space)
  if (activeTab === "url") {
    return (
      <DialogContent className="p-5 sm:max-w-lg">
        <DialogHeader className="flex flex-row items-center justify-between border-b border-border pb-3">
          <div className="flex items-center gap-2">
            <div className="flex size-7 items-center justify-center rounded-md bg-primary/10 text-primary">
              <LinkIcon className="size-4" />
            </div>
            <DialogTitle className="text-sm font-semibold text-foreground">
              Insert from Web URL
            </DialogTitle>
          </div>
          {modeSwitcher}
        </DialogHeader>

        <form onSubmit={handleUrlSubmit} className="space-y-4 pt-1">
          <div className="space-y-1.5">
            <Label htmlFor="web-url-input" className="text-xs font-semibold">
              Image Web URL <span className="text-destructive">*</span>
            </Label>
            <Input
              id="web-url-input"
              type="url"
              placeholder="https://example.com/image.png"
              value={webUrl}
              onChange={(e) => setWebUrl(e.target.value)}
              className="text-xs"
              autoFocus
            />
          </div>

          {/* Live Preview Box when URL provided */}
          {webUrl.trim().length > 10 && (
            <div className="flex h-32 w-full items-center justify-center overflow-hidden rounded-lg border border-border/80 bg-muted/20 p-2">
              <img
                src={webUrl.trim()}
                alt="Preview"
                className="max-h-full max-w-full rounded object-contain"
                onError={(e) => {
                  ;(e.target as HTMLElement).style.display = "none"
                }}
              />
            </div>
          )}

          <div className="space-y-1.5">
            <Label htmlFor="url-alt-input" className="text-xs font-semibold">
              Alt Description (Optional)
            </Label>
            <Input
              id="url-alt-input"
              type="text"
              placeholder="e.g. Architecture Diagram"
              value={altText}
              onChange={(e) => setAltText(e.target.value)}
              className="text-xs"
            />
          </div>

          <DialogFooter className="pt-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={onClose}
              className="text-xs"
            >
              Cancel
            </Button>
            <Button
              type="submit"
              size="sm"
              disabled={!webUrl.trim()}
              className="text-xs"
            >
              Insert Image URL
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    )
  }

  // 2. EMPTY DROPZONE STATE: Compact Modal (No blank space)
  if (!imageSrc) {
    return (
      <DialogContent className="p-5 sm:max-w-lg">
        <DialogHeader className="flex flex-row items-center justify-between border-b border-border pb-3">
          <div className="flex items-center gap-2">
            <div className="flex size-7 items-center justify-center rounded-md bg-primary/10 text-primary">
              <Upload className="size-4" />
            </div>
            <DialogTitle className="text-sm font-semibold text-foreground">
              Upload Image
            </DialogTitle>
          </div>
          {modeSwitcher}
        </DialogHeader>

        <div className="py-2">
          <div
            onDrop={handleDrop}
            onDragOver={(e) => e.preventDefault()}
            onClick={() => fileInputRef.current?.click()}
            className="flex w-full cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed border-border/80 bg-muted/20 px-6 py-10 text-center transition-colors hover:border-primary/50 hover:bg-muted/40"
          >
            <div className="mb-3 flex size-12 items-center justify-center rounded-full bg-primary/10 text-primary">
              <Upload className="size-6" />
            </div>
            <p className="text-sm font-semibold text-foreground">
              Click to browse or drag &amp; drop an image
            </p>
            <p className="mt-1 text-xs text-muted-foreground">
              PNG, JPG, WebP, GIF, SVG up to 10MB
            </p>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              className="hidden"
              onChange={(e) => {
                if (e.target.files && e.target.files[0]) {
                  handleFileSelect(e.target.files[0])
                }
              }}
            />
          </div>
        </div>

        <DialogFooter className="pt-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={onClose}
            className="text-xs"
          >
            Cancel
          </Button>
        </DialogFooter>
      </DialogContent>
    )
  }

  // 3. FULL PIC-EDITOR STUDIO: Expanded Canvas Layout
  return (
    <DialogContent className="flex h-[88vh] max-h-[88vh] w-full flex-col gap-0 overflow-hidden p-0 sm:max-w-4xl">
      {/* Edge-to-Edge Top Header */}
      <DialogHeader className="flex h-11 shrink-0 flex-row items-center justify-between border-b border-border bg-card px-4 py-0 sm:px-5">
        <div className="flex items-center gap-2">
          <div className="flex size-6 items-center justify-center rounded-md bg-primary/10 text-primary">
            <ImageIcon className="size-3.5" />
          </div>
          <DialogTitle className="text-xs font-semibold text-foreground">
            Crop &amp; Edit Image
          </DialogTitle>
          {imgWidth > 0 && (
            <Badge
              variant="outline"
              className="font-mono text-[10px] font-normal text-muted-foreground"
            >
              {imgWidth} &times; {imgHeight}px
            </Badge>
          )}
        </div>

        {modeSwitcher}
      </DialogHeader>

      {/* Editor Body */}
      <div className="flex min-h-0 flex-1 flex-col overflow-hidden">
        {/* Unified Toolbar: Aspect Presets + Rotations & Flips */}
        <div className="flex shrink-0 flex-wrap items-center justify-between gap-2 border-b border-border/80 bg-muted/40 px-3 py-1.5 text-xs select-none">
          {/* Aspect Presets */}
          <div className="flex items-center gap-1">
            <span className="text-[11px] font-medium text-muted-foreground">
              Aspect:
            </span>
            {(["free", "1:1", "4:3", "16:9"] as const).map((preset) => (
              <button
                key={preset}
                type="button"
                onClick={() => handleSetCropPreset(preset)}
                className={cn(
                  "cursor-pointer rounded px-2 py-0.5 text-[11px] font-medium transition-colors",
                  cropAspectPreset === preset
                    ? "bg-primary font-semibold text-primary-foreground shadow-2xs"
                    : "border border-border/70 bg-background text-muted-foreground hover:bg-muted hover:text-foreground"
                )}
              >
                {preset === "free" ? "Free" : preset}
              </button>
            ))}
            {isCropActive && (
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={handleResetCrop}
                className="h-6 gap-1 px-1.5 text-[10px] text-muted-foreground hover:text-foreground"
              >
                <Maximize2 className="size-2.5" /> Full
              </Button>
            )}
          </div>

          {/* Transformations: Rotate & Flips */}
          <div className="flex items-center gap-1.5">
            <Tooltip>
              <TooltipTrigger
                render={
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={handleRotate}
                    className="h-7 gap-1 px-2 text-xs"
                  >
                    <RotateCw className="size-3.5" />
                    <span className="hidden sm:inline">Rotate</span>
                  </Button>
                }
              />
              <TooltipContent side="bottom">
                Rotate 90&deg; Clockwise
              </TooltipContent>
            </Tooltip>

            <Tooltip>
              <TooltipTrigger
                render={
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={handleFlipH}
                    className="h-7 gap-1 px-2 text-xs"
                  >
                    <FlipHorizontal className="size-3.5" />
                    <span className="hidden sm:inline">Flip H</span>
                  </Button>
                }
              />
              <TooltipContent side="bottom">Flip Horizontally</TooltipContent>
            </Tooltip>

            <Tooltip>
              <TooltipTrigger
                render={
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={handleFlipV}
                    className="h-7 gap-1 px-2 text-xs"
                  >
                    <FlipVertical className="size-3.5" />
                    <span className="hidden sm:inline">Flip V</span>
                  </Button>
                }
              />
              <TooltipContent side="bottom">Flip Vertically</TooltipContent>
            </Tooltip>

            {isModified && (
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={handleResetAll}
                className="h-7 gap-1 px-2 text-xs text-muted-foreground hover:text-foreground"
              >
                <RefreshCw className="size-3" />
                <span>Reset</span>
              </Button>
            )}
          </div>
        </div>

        {/* Image Canvas Stage */}
        <div className="relative flex min-h-0 flex-1 items-center justify-center overflow-hidden bg-neutral-950 p-3 select-none">
          <div
            ref={cropContainerRef}
            onPointerMove={handlePointerMove}
            onPointerUp={handlePointerUp}
            className="relative inline-block max-h-full max-w-full"
          >
            <img
              src={imageSrc}
              alt="Preview"
              className="pointer-events-none max-h-[58vh] max-w-full rounded object-contain"
            />

            {/* Pic Editor Crop Overlay (Active by Default with 8 Handles + 3x3 Grid) */}
            <div
              style={{
                position: "absolute",
                left: `${cropBox.x}%`,
                top: `${cropBox.y}%`,
                width: `${cropBox.w}%`,
                height: `${cropBox.h}%`,
              }}
              onPointerDown={(e) => handlePointerDown(e, "move")}
              className="cursor-move touch-none border border-white shadow-[0_0_0_9999px_rgba(0,0,0,0.65)]"
            >
              {/* 3x3 Rule-of-Thirds Grid */}
              <div className="pointer-events-none absolute inset-0 grid grid-cols-3 grid-rows-3">
                <div className="border-r border-b border-white/25" />
                <div className="border-r border-b border-white/25" />
                <div className="border-b border-white/25" />
                <div className="border-r border-b border-white/25" />
                <div className="border-r border-b border-white/25" />
                <div className="border-b border-white/25" />
                <div className="border-r border-b border-white/25" />
                <div className="border-r border-b border-white/25" />
                <div />
              </div>

              {/* Live Crop Dimensions Badge */}
              <div className="pointer-events-none absolute right-1 bottom-1 rounded bg-black/75 px-1 py-0.5 font-mono text-[9px] text-white">
                {cropPixelWidth} &times; {cropPixelHeight}px
              </div>

              {/* 4 Corner Drag Handles */}
              <div
                onPointerDown={(e) => handlePointerDown(e, "nw")}
                className="absolute -top-2 -left-2 size-4 cursor-nwse-resize rounded-xs border-2 border-white bg-primary shadow-sm"
              />
              <div
                onPointerDown={(e) => handlePointerDown(e, "ne")}
                className="absolute -top-2 -right-2 size-4 cursor-nesw-resize rounded-xs border-2 border-white bg-primary shadow-sm"
              />
              <div
                onPointerDown={(e) => handlePointerDown(e, "se")}
                className="absolute -right-2 -bottom-2 size-4 cursor-nwse-resize rounded-xs border-2 border-white bg-primary shadow-sm"
              />
              <div
                onPointerDown={(e) => handlePointerDown(e, "sw")}
                className="absolute -bottom-2 -left-2 size-4 cursor-nesw-resize rounded-xs border-2 border-white bg-primary shadow-sm"
              />

              {/* 4 Edge Drag Handles */}
              <div
                onPointerDown={(e) => handlePointerDown(e, "n")}
                className="absolute -top-1.5 left-1/2 h-3 w-6 -translate-x-1/2 cursor-ns-resize rounded-full border border-white bg-primary shadow-sm"
              />
              <div
                onPointerDown={(e) => handlePointerDown(e, "s")}
                className="absolute -bottom-1.5 left-1/2 h-3 w-6 -translate-x-1/2 cursor-ns-resize rounded-full border border-white bg-primary shadow-sm"
              />
              <div
                onPointerDown={(e) => handlePointerDown(e, "w")}
                className="absolute top-1/2 -left-1.5 h-6 w-3 -translate-y-1/2 cursor-ew-resize rounded-full border border-white bg-primary shadow-sm"
              />
              <div
                onPointerDown={(e) => handlePointerDown(e, "e")}
                className="absolute top-1/2 -right-1.5 h-6 w-3 -translate-y-1/2 cursor-ew-resize rounded-full border border-white bg-primary shadow-sm"
              />
            </div>
          </div>
        </div>

        {/* Alt Text Caption Bar */}
        <div className="flex shrink-0 items-center gap-2 border-t border-border bg-card px-4 py-2">
          <Label
            htmlFor="image-alt-caption"
            className="shrink-0 text-xs font-semibold text-muted-foreground"
          >
            Alt Text:
          </Label>
          <Input
            id="image-alt-caption"
            type="text"
            placeholder="Image description (inserted as ![alt](url) in Markdown)"
            value={altText}
            onChange={(e) => setAltText(e.target.value)}
            className="h-8 flex-1 text-xs"
          />
        </div>
      </div>

      {/* Editor Footer */}
      <DialogFooter className="flex h-11 shrink-0 flex-row items-center justify-between border-t border-border bg-muted/20 px-4 py-0 sm:px-5">
        <div>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => fileInputRef.current?.click()}
            className="h-7 gap-1 text-xs text-muted-foreground hover:text-foreground"
          >
            <Upload className="size-3" />
            <span>Change Image</span>
          </Button>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            className="hidden"
            onChange={(e) => {
              if (e.target.files && e.target.files[0]) {
                handleFileSelect(e.target.files[0])
              }
            }}
          />
        </div>

        <div className="flex items-center gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={onClose}
            disabled={isUploading}
            className="h-7 text-xs"
          >
            Cancel
          </Button>

          <Button
            type="button"
            size="sm"
            onClick={handleUploadSubmit}
            disabled={isUploading}
            className="h-7 gap-1.5 text-xs font-semibold"
          >
            {isUploading ? (
              <>
                <Spinner className="size-3" />
                <span>Uploading...</span>
              </>
            ) : (
              <>
                <CropIcon className="size-3" />
                <span>Apply &amp; Insert Image</span>
              </>
            )}
          </Button>
        </div>
      </DialogFooter>
    </DialogContent>
  )
}

export function ImageUploadDialog({
  isOpen,
  initialFile,
  onClose,
  onSubmit,
}: ImageUploadDialogProps) {
  const [isUploading, setIsUploading] = React.useState(false)

  return (
    <Dialog
      open={isOpen}
      onOpenChange={(open) => !open && !isUploading && onClose()}
    >
      {isOpen && (
        <ImageEditorContent
          initialFile={initialFile}
          onClose={onClose}
          onSubmit={onSubmit}
          isUploading={isUploading}
          setIsUploading={setIsUploading}
        />
      )}
    </Dialog>
  )
}

export { ImageUploadDialog as ImageEditorDialog }
