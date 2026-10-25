import { type ReactNode } from "react"
import { Play, CheckCircle2, RotateCcw, Loader2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  NativeSelect,
  NativeSelectOption,
} from "@/components/ui/native-select"
import {
  ResizablePanelGroup,
  ResizablePanel,
  ResizableHandle,
} from "@/components/ui/resizable"
import { MonacoEditor } from "@/components/monaco-editor"
import { getLanguageById } from "@/config/languages"
import { resolveMonacoLanguage } from "@/lib/monaco-languages"
import type { AssessmentItemResponse } from "@/types/assessment"

interface ExamCodeWorkspaceProps {
  question: AssessmentItemResponse
  currentLanguage: string
  currentCode: string
  activeBottomTab: "sample" | "custom" | "submit"
  isSampleRunning: boolean
  isCustomRunning: boolean
  isSubmittingCode: boolean
  onLanguageChange: (langId: string) => void
  onCodeChange: (code: string) => void
  onResetStarter: () => void
  onRunTests: () => void
  onSubmitCode: () => void
  bottomPanelContent: ReactNode
}

export function ExamCodeWorkspace({
  question,
  currentLanguage,
  currentCode,
  activeBottomTab,
  isSampleRunning,
  isCustomRunning,
  isSubmittingCode,
  onLanguageChange,
  onCodeChange,
  onResetStarter,
  onRunTests,
  onSubmitCode,
  bottomPanelContent,
}: ExamCodeWorkspaceProps) {
  const supportedLanguages =
    question.supportedLanguageIds && question.supportedLanguageIds.length > 0
      ? question.supportedLanguageIds
      : ["java-21", "python-3.12", "cpp-23", "javascript-node-20"]

  return (
    <div className="h-full flex flex-col min-w-0">
      {/* Code Editor Top Bar */}
      <div className="h-11 border-b border-border bg-card/60 px-4 flex items-center justify-between shrink-0 gap-3">
        {/* Language Selector */}
        <div className="flex items-center gap-2">
          <span className="text-xs text-muted-foreground font-medium hidden sm:inline">
            Language:
          </span>
          <NativeSelect
            value={currentLanguage}
            onChange={(e) => onLanguageChange(e.target.value)}
            className="h-7 text-xs w-36 font-medium"
          >
            {supportedLanguages.map((langId) => {
              const langConfig = getLanguageById(langId)
              return (
                <NativeSelectOption key={langId} value={langId}>
                  {langConfig ? langConfig.name : langId}
                </NativeSelectOption>
              )
            })}
          </NativeSelect>
        </div>

        {/* Code Action Buttons */}
        <div className="flex items-center gap-2">
          <Button
            size="sm"
            variant="ghost"
            onClick={onResetStarter}
            title="Reset to blank starter code"
            className="h-7 text-xs text-muted-foreground hover:text-foreground gap-1 px-2"
          >
            <RotateCcw className="size-3" />
            <span className="hidden sm:inline">Reset Code</span>
          </Button>

          <Button
            size="sm"
            variant="outline"
            disabled={isSampleRunning || isCustomRunning}
            onClick={onRunTests}
            className="h-7 text-xs gap-1.5"
          >
            {(activeBottomTab === "custom"
              ? isCustomRunning
              : isSampleRunning) ? (
              <Loader2 className="size-3 animate-spin text-primary" />
            ) : (
              <Play className="size-3 text-emerald-500 fill-emerald-500" />
            )}
            <span>
              {activeBottomTab === "custom" ? "Run Custom Input" : "Run Tests"}
            </span>
          </Button>

          <Button
            size="sm"
            disabled={isSubmittingCode}
            onClick={onSubmitCode}
            className="h-7 text-xs gap-1.5 font-semibold"
          >
            {isSubmittingCode ? (
              <Loader2 className="size-3 animate-spin" />
            ) : (
              <CheckCircle2 className="size-3" />
            )}
            <span>Submit Code</span>
          </Button>
        </div>
      </div>

      {/* Vertical Resizable Panels: Editor Top, Bottom Output Below */}
      <ResizablePanelGroup orientation="vertical" className="flex-1">
        <ResizablePanel defaultSize={60} minSize={25}>
          <div className="h-full w-full">
            <MonacoEditor
              language={resolveMonacoLanguage(currentLanguage)}
              value={currentCode}
              onChange={onCodeChange}
              height="100%"
              options={{
                minimap: { enabled: false },
                fontSize: 13,
                lineNumbers: "on",
                roundedSelection: true,
                scrollBeyondLastLine: false,
                automaticLayout: true,
                padding: { top: 12, bottom: 12 },
              }}
            />
          </div>
        </ResizablePanel>

        <ResizableHandle withHandle />

        <ResizablePanel defaultSize={40} minSize={20}>
          {bottomPanelContent}
        </ResizablePanel>
      </ResizablePanelGroup>
    </div>
  )
}

