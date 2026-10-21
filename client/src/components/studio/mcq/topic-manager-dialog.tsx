import { useState } from "react"
import { Plus, Trash2, Tag, Loader2, BookOpen } from "lucide-react"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { useMcqTopics, useCreateMcqTopic, useDeleteMcqTopic } from "@/hooks/use-mcq"
import type { McqTopicItem } from "@/types/mcq"

interface TopicManagerDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function TopicManagerDialog({ open, onOpenChange }: TopicManagerDialogProps) {
  const { data: topics = [], isLoading } = useMcqTopics()
  const createTopicMutation = useCreateMcqTopic()
  const deleteTopicMutation = useDeleteMcqTopic()

  const [name, setName] = useState("")
  const [description, setDescription] = useState("")
  const [topicToDelete, setTopicToDelete] = useState<McqTopicItem | null>(null)

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!name.trim()) return

    await createTopicMutation.mutateAsync({
      name: name.trim(),
      description: description.trim() || undefined,
    })

    setName("")
    setDescription("")
  }

  const handleDelete = async () => {
    if (!topicToDelete) return
    await deleteTopicMutation.mutateAsync(topicToDelete.id)
    setTopicToDelete(null)
  }

  return (
    <>
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="sm:max-w-xl">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Tag className="h-5 w-5 text-primary" />
              Manage Question Topics
            </DialogTitle>
            <DialogDescription>
              Create and manage topic categories used to categorize multiple choice questions.
            </DialogDescription>
          </DialogHeader>

          {/* Add New Topic Form */}
          <form onSubmit={handleCreate} className="space-y-3 rounded-lg border border-border/60 bg-muted/30 p-3.5">
            <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              Add New Topic
            </span>
            <div className="flex gap-2">
              <Input
                placeholder="Topic name (e.g. Algorithms, Spring Boot, React)"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="flex-1"
                disabled={createTopicMutation.isPending}
              />
              <Button
                type="submit"
                size="sm"
                disabled={!name.trim() || createTopicMutation.isPending}
                className="gap-1.5"
              >
                {createTopicMutation.isPending ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Plus className="h-4 w-4" />
                )}
                Add Topic
              </Button>
            </div>
            <Input
              placeholder="Short description (optional)"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="text-xs"
              disabled={createTopicMutation.isPending}
            />
          </form>

          {/* Existing Topics List */}
          <div className="mt-2 space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Existing Topics ({topics.length})
              </span>
            </div>

            <div className="max-h-64 space-y-1.5 overflow-y-auto pr-1">
              {isLoading ? (
                <div className="flex h-24 items-center justify-center text-muted-foreground">
                  <Loader2 className="h-5 w-5 animate-spin" />
                </div>
              ) : topics.length === 0 ? (
                <div className="flex flex-col items-center justify-center py-8 text-center text-muted-foreground">
                  <BookOpen className="h-8 w-8 stroke-1 text-muted-foreground/50 mb-1" />
                  <p className="text-sm">No topics created yet.</p>
                  <p className="text-xs text-muted-foreground/70">
                    Add topics above to start categorizing your MCQs.
                  </p>
                </div>
              ) : (
                topics.map((topic) => (
                  <div
                    key={topic.id}
                    className="flex items-center justify-between rounded-md border border-border/50 bg-card/60 px-3 py-2 text-sm transition-colors hover:bg-accent/40"
                  >
                    <div className="flex flex-col">
                      <div className="flex items-center gap-2">
                        <span className="font-medium text-foreground">{topic.name}</span>
                        <Badge variant="outline" className="text-[10px] px-1.5 py-0 h-4">
                          {topic.questionCount} {topic.questionCount === 1 ? "question" : "questions"}
                        </Badge>
                      </div>
                      {topic.description && (
                        <p className="text-xs text-muted-foreground line-clamp-1">
                          {topic.description}
                        </p>
                      )}
                    </div>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="h-8 w-8 p-0 text-muted-foreground hover:text-destructive"
                      onClick={() => setTopicToDelete(topic)}
                      title="Delete topic"
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                ))
              )}
            </div>
          </div>
        </DialogContent>
      </Dialog>

      {/* Delete Topic Confirmation Alert Dialog */}
      <AlertDialog open={Boolean(topicToDelete)} onOpenChange={(open) => !open && setTopicToDelete(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete Topic</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to delete the topic &ldquo;{topicToDelete?.name}&rdquo;?
              {topicToDelete && topicToDelete.questionCount > 0 && (
                <span className="block mt-2 text-destructive font-medium">
                  Notice: This topic is currently associated with {topicToDelete.questionCount} question(s).
                </span>
              )}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              Delete Topic
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}

