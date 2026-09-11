import * as React from "react"
import axios from "axios"
import { createFileRoute } from "@tanstack/react-router"
import {
  MarkdownEditor,
  finalizeMarkdownImages,
  type MarkdownEditorValue,
  type MarkdownViewMode,
} from "@/components/markdown"
import { MarkdownPreview } from "@/components/markdown-preview"
import { SplitView } from "@/components/split-view"
import { Button } from "@/components/ui/button"
import { toast } from "@/components/ui/toast"
import { cn } from "@/lib/utils"
import {
  FileText,
  Save,
  RefreshCw,
  Edit3,
  Columns,
  Eye,
  RotateCcw,
} from "lucide-react"

export const Route = createFileRoute("/workspace")({
  component: WorkspaceComponent,
})

const WORKSPACE_STORAGE_KEY = "stacked_workspace_content_v1"

const INITIAL_MARKDOWN = `# Dynamic Programming: 0/1 Knapsack Problem

> [!NOTE]
> This workspace demonstrates the **Monaco-powered Markdown Editor** with full LaTeX math rendering, syntax highlighting, image tracking, and live preview.

## Mathematical Formulation

Given weights $w_i$ and values $v_i$ for $n$ items, maximize total value subject to capacity $W$:

$$\\max \\sum_{i=1}^{n} v_i x_i \\quad \\text{subject to} \\quad \\sum_{i=1}^{n} w_i x_i \\le W, \\quad x_i \\in \\{0, 1\\}$$

The optimal recurrence relation is given by:

$$DP[i][w] = \\max(DP[i-1][w], DP[i-1][w - w_i] + v_i)$$

### Complexity Analysis

| Approach | Time Complexity | Space Complexity | Description |
| :--- | :---: | :---: | ---: |
| Recursive | $O(2^n)$ | $O(n)$ | Exponential brute-force traversal |
| Memoization (Top-Down) | $O(n \\cdot W)$ | $O(n \\cdot W)$ | Recursive with lookup cache |
| Tabulation (Bottom-Up) | $O(n \\cdot W)$ | $O(W)$ | Space-optimized 1D rolling array |

## Java 25 Implementation

\`\`\`java
public class Knapsack {
    public static int solveKnapsack(int[] values, int[] weights, int capacity) {
        int[] dp = new int[capacity + 1];

        for (int i = 0; i < values.length; i++) {
            for (int w = capacity; w >= weights[i]; w--) {
                dp[w] = Math.max(dp[w], dp[w - weights[i]] + values[i]);
            }
        }
        return dp[capacity];
    }
}
\`\`\`

> [!TIP]
> Drag and drop or paste any image directly into this editor, or use the toolbar to insert files and web URLs!
`

function WorkspaceComponent() {
  const [editorState, setEditorState] = React.useState<MarkdownEditorValue>(
    () => {
      try {
        const saved = localStorage.getItem(WORKSPACE_STORAGE_KEY)
        if (saved) {
          const parsed = JSON.parse(saved)
          if (parsed && typeof parsed.content === "string") {
            return {
              content: parsed.content,
              uploadedImages: Array.isArray(parsed.uploadedImages)
                ? parsed.uploadedImages
                : [],
            }
          }
        }
      } catch (e) {
        console.warn("Failed to load saved workspace:", e)
      }
      return {
        content: INITIAL_MARKDOWN,
        uploadedImages: [],
      }
    }
  )

  const [viewMode, setViewMode] = React.useState<MarkdownViewMode>("split")
  const [isSaving, setIsSaving] = React.useState(false)
  const [lastSavedTime, setLastSavedTime] = React.useState<string | null>(null)

  // Auto-sync workspace draft to localStorage
  React.useEffect(() => {
    try {
      localStorage.setItem(WORKSPACE_STORAGE_KEY, JSON.stringify(editorState))
    } catch {
      // ignore
    }
  }, [editorState])

  const handleSave = async (val: MarkdownEditorValue) => {
    setIsSaving(true)
    try {
      // Finalize and promote referenced temporary images to permanent storage
      const result = await finalizeMarkdownImages(val, { deleteUnused: false })
      setEditorState(result.updatedValue)
      const now = new Date().toLocaleTimeString()
      setLastSavedTime(now)

      try {
        localStorage.setItem(
          WORKSPACE_STORAGE_KEY,
          JSON.stringify(result.updatedValue)
        )
      } catch (storageErr) {
        console.warn("Failed to persist to localStorage:", storageErr)
      }

      toast.add({
        title: "Markdown Saved",
        description: `Content saved successfully.${
          result.promotedFileIds.length > 0
            ? ` Promoted ${result.promotedFileIds.length} image(s) to permanent storage.`
            : ""
        }`,
        type: "success",
      })
    } catch (err: unknown) {
      console.error("Save failed:", err)
      let message = "Failed to save markdown content."
      if (axios.isAxiosError(err) && err.response?.data?.message) {
        message = err.response.data.message
      } else if (err instanceof Error) {
        message = err.message
      }
      toast.add({
        title: "Save Failed",
        description: message,
        type: "error",
      })
    } finally {
      setIsSaving(false)
    }
  }

  const handleResetToDefault = () => {
    const defaultState: MarkdownEditorValue = {
      content: INITIAL_MARKDOWN,
      uploadedImages: [],
    }
    setEditorState(defaultState)
    localStorage.removeItem(WORKSPACE_STORAGE_KEY)
    toast.add({
      title: "Workspace Reset",
      description: "Restored sample content and defaults.",
      type: "info",
    })
  }

  return (
    <div className="mx-auto flex min-h-screen max-w-7xl flex-col space-y-6 bg-background p-4 text-foreground md:p-6">
      {/* Header */}
      <div className="flex flex-col justify-between gap-4 border-b border-border pb-2 sm:flex-row sm:items-center">
        <div>
          <div className="flex items-center gap-2.5">
            <div className="rounded-lg bg-primary/10 p-2 text-primary">
              <FileText className="h-5 w-5" />
            </div>
            <div>
              <h1 className="text-xl font-bold tracking-tight">
                Markdown Editor Workspace
              </h1>
              <p className="text-xs text-muted-foreground">
                Monaco Editor &bull; LaTeX Math &bull; GFM &bull; File Module
                Storage Sync
              </p>
            </div>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          {/* Workspace View Mode Switcher */}
          <div className="flex items-center gap-0.5 rounded-lg border border-border/80 bg-muted/50 p-1">
            <button
              type="button"
              onClick={() => setViewMode("edit")}
              className={cn(
                "flex cursor-pointer items-center gap-1.5 rounded-md px-2.5 py-1 text-xs font-medium transition-all",
                viewMode === "edit"
                  ? "bg-background text-foreground shadow-xs"
                  : "text-muted-foreground hover:text-foreground"
              )}
            >
              <Edit3 className="h-3.5 w-3.5" />
              <span>Edit</span>
            </button>
            <button
              type="button"
              onClick={() => setViewMode("split")}
              className={cn(
                "flex cursor-pointer items-center gap-1.5 rounded-md px-2.5 py-1 text-xs font-medium transition-all",
                viewMode === "split"
                  ? "bg-background text-foreground shadow-xs"
                  : "text-muted-foreground hover:text-foreground"
              )}
            >
              <Columns className="h-3.5 w-3.5" />
              <span>Split</span>
            </button>
            <button
              type="button"
              onClick={() => setViewMode("preview")}
              className={cn(
                "flex cursor-pointer items-center gap-1.5 rounded-md px-2.5 py-1 text-xs font-medium transition-all",
                viewMode === "preview"
                  ? "bg-primary text-primary-foreground shadow-xs"
                  : "text-muted-foreground hover:text-foreground"
              )}
            >
              <Eye className="h-3.5 w-3.5" />
              <span>Preview</span>
            </button>
          </div>

          {lastSavedTime && (
            <span className="text-[11px] text-muted-foreground">
              Last saved at {lastSavedTime}
            </span>
          )}

          <Button
            type="button"
            size="sm"
            variant="outline"
            onClick={handleResetToDefault}
            disabled={isSaving}
            className="gap-1.5 text-xs text-muted-foreground hover:text-foreground"
          >
            <RotateCcw className="h-3.5 w-3.5" />
            <span>Reset Demo</span>
          </Button>

          <Button
            type="button"
            size="sm"
            onClick={() => handleSave(editorState)}
            disabled={isSaving}
            className="gap-1.5 text-xs font-semibold shadow-xs"
          >
            {isSaving ? (
              <RefreshCw className="h-3.5 w-3.5 animate-spin" />
            ) : (
              <Save className="h-3.5 w-3.5" />
            )}
            Save & Promote Images
          </Button>
        </div>
      </div>

      {/* Main Workspace Area */}
      <div className="flex-1">
        {viewMode === "edit" && (
          <MarkdownEditor
            value={editorState}
            onChange={setEditorState}
            label="Question Content / Problem Description"
            description="Supports GitHub Flavored Markdown, inline $x$ and block $$...$$ KaTeX math, and direct image uploads."
            height={540}
            onSave={handleSave}
          />
        )}

        {viewMode === "preview" && (
          <div className="flex flex-col gap-1.5">
            <div className="flex items-center justify-between pb-0.5">
              <span className="text-xs font-semibold text-foreground">
                Markdown Preview
              </span>
              <span className="text-[11px] text-muted-foreground">
                Live output with KaTeX &bull; syntax highlighting
              </span>
            </div>
            <div className="h-135 w-full overflow-hidden rounded-lg border border-border bg-card p-4 shadow-xs">
              <MarkdownPreview content={editorState.content} />
            </div>
          </div>
        )}

        {viewMode === "split" && (
          <div className="flex flex-col gap-1.5">
            <div className="flex items-center justify-between pb-0.5">
              <span className="text-xs font-semibold text-foreground">
                Split View (Editor & Live Preview)
              </span>
              <span className="text-[11px] text-muted-foreground">
                Drag divider to resize panels
              </span>
            </div>
            <div className="h-140 w-full overflow-hidden rounded-lg border border-border bg-card shadow-xs">
              <SplitView
                firstPane={
                  <MarkdownEditor
                    value={editorState}
                    onChange={setEditorState}
                    height="100%"
                    className="rounded-none border-0 shadow-none"
                    onSave={handleSave}
                  />
                }
                secondPane={
                  <div className="h-full min-h-0 w-full overflow-hidden bg-background p-4">
                    <MarkdownPreview content={editorState.content} />
                  </div>
                }
                defaultSize={50}
                minSize={20}
                maxSize={80}
                className="h-full w-full"
              />
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
