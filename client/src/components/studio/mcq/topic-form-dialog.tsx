import { useState, useEffect } from "react"
import { FolderPlus, Edit, Loader2 } from "lucide-react"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { useCreateMcqTopic, useUpdateMcqTopic } from "@/hooks/use-mcq"
import type { McqTopicItem } from "@/types/mcq"

interface TopicFormDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  topicToEdit?: McqTopicItem | null
}

export function TopicFormDialog({
  open,
  onOpenChange,
  topicToEdit,
}: TopicFormDialogProps) {
  const isEditMode = Boolean(topicToEdit)
  const createTopicMutation = useCreateMcqTopic()
  const updateTopicMutation = useUpdateMcqTopic()

  const [name, setName] = useState("")
  const [description, setDescription] = useState("")

  useEffect(() => {
    if (topicToEdit) {
      setName(topicToEdit.name)
      setDescription(topicToEdit.description || "")
    } else {
      setName("")
      setDescription("")
    }
  }, [topicToEdit, open])

  const isPending = createTopicMutation.isPending || updateTopicMutation.isPending

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const trimmedName = name.trim()
    if (!trimmedName) return

    if (isEditMode && topicToEdit) {
      await updateTopicMutation.mutateAsync({
        id: topicToEdit.id,
        payload: {
          name: trimmedName,
          description: description.trim() || undefined,
        },
      })
    } else {
      await createTopicMutation.mutateAsync({
        name: trimmedName,
        description: description.trim() || undefined,
      })
    }

    onOpenChange(false)
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <div className="flex items-center gap-2">
            <div className="flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
              {isEditMode ? <Edit className="size-4" /> : <FolderPlus className="size-4" />}
            </div>
            <div>
              <DialogTitle className="text-base font-semibold">
                {isEditMode ? "Edit Curriculum Topic" : "Create Curriculum Topic"}
              </DialogTitle>
              <DialogDescription className="text-xs text-muted-foreground">
                {isEditMode
                  ? "Update domain name and description."
                  : "Add a new domain track to categorize your MCQs."}
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4 pt-2">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-foreground">
              Topic Name <span className="text-destructive">*</span>
            </label>
            <Input
              placeholder="e.g. Quantitative Aptitude, Operating Systems"
              value={name}
              onChange={(e) => setName(e.target.value)}
              disabled={isPending}
              autoFocus
              className="text-sm"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-foreground">
              Description <span className="text-muted-foreground text-[10px] font-normal">(Optional)</span>
            </label>
            <Input
              placeholder="Brief description of the domain scope or curriculum"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              disabled={isPending}
              className="text-sm"
            />
          </div>

          <DialogFooter className="gap-2 pt-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => onOpenChange(false)}
              disabled={isPending}
              className="text-xs"
            >
              Cancel
            </Button>
            <Button
              type="submit"
              size="sm"
              disabled={!name.trim() || isPending}
              className="gap-1.5 text-xs font-semibold"
            >
              {isPending && <Loader2 className="size-3.5 animate-spin" />}
              {isEditMode ? "Save Changes" : "Create Topic"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

