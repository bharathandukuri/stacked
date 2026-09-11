import * as React from "react"
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
import { Link as LinkIcon } from "lucide-react"
import { toast } from "@/components/ui/toast"

export interface LinkDialogProps {
  isOpen: boolean
  onClose: () => void
  onSubmit: (linkMarkdown: string) => void
  initialText?: string
}

interface LinkDialogFormProps {
  onClose: () => void
  onSubmit: (linkMarkdown: string) => void
  initialText: string
}

function LinkDialogForm({
  onClose,
  onSubmit,
  initialText,
}: LinkDialogFormProps) {
  const [url, setUrl] = React.useState("")
  const [text, setText] = React.useState(initialText)
  const [title, setTitle] = React.useState("")

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const cleanUrl = url.trim()
    if (!cleanUrl) {
      toast.add({
        title: "URL Required",
        description: "Please enter a valid URL.",
        type: "error",
      })
      return
    }

    const displayText = text.trim() || cleanUrl
    const titlePart = title.trim() ? ` "${title.trim()}"` : ""
    const linkMarkdown = `[${displayText}](${cleanUrl}${titlePart})`

    onSubmit(linkMarkdown)
    onClose()
  }

  return (
    <DialogContent className="sm:max-w-md">
      <DialogHeader>
        <DialogTitle className="flex items-center gap-2 text-sm font-semibold">
          <LinkIcon className="h-4 w-4 text-primary" />
          Insert Hyperlink
        </DialogTitle>
      </DialogHeader>

      <form onSubmit={handleSubmit} className="space-y-4 py-2">
        <div className="space-y-1.5">
          <Label htmlFor="link-url" className="text-xs font-semibold">
            URL <span className="text-destructive">*</span>
          </Label>
          <Input
            id="link-url"
            type="text"
            placeholder="https://example.com"
            value={url}
            onChange={(e) => setUrl(e.target.value)}
            autoFocus
            className="text-xs"
          />
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="link-text" className="text-xs font-semibold">
            Text to Display
          </Label>
          <Input
            id="link-text"
            type="text"
            placeholder="Descriptive anchor text"
            value={text}
            onChange={(e) => setText(e.target.value)}
            className="text-xs"
          />
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="link-title" className="text-xs font-semibold">
            Tooltip / Title (Optional)
          </Label>
          <Input
            id="link-title"
            type="text"
            placeholder="Hover tooltip text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
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
          <Button type="submit" size="sm" className="text-xs">
            Insert Link
          </Button>
        </DialogFooter>
      </form>
    </DialogContent>
  )
}

export function LinkDialog({
  isOpen,
  onClose,
  onSubmit,
  initialText = "",
}: LinkDialogProps) {
  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      {isOpen && (
        <LinkDialogForm
          onClose={onClose}
          onSubmit={onSubmit}
          initialText={initialText}
        />
      )}
    </Dialog>
  )
}
