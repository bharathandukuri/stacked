import { CheckSquare, Code2, FileCode } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { MarkdownPreview } from "@/components/markdown-preview/markdown-preview"
import type { AssessmentItemResponse } from "@/types/assessment"

interface ExamStatementProps {
  question: AssessmentItemResponse
  currentIndex: number
}

export function ExamStatement({ question, currentIndex }: ExamStatementProps) {
  const statement =
    question.questionType === "MCQ"
      ? question.mcqContent || ""
      : question.codingStatement || ""

  return (
    <div className="h-full overflow-y-auto p-5 space-y-5">
      {/* Question Header & Badges */}
      <div className="space-y-3 pb-3 border-b border-border/60">
        <div className="flex flex-wrap items-center gap-2">
          <Badge variant="outline" className="text-xs font-mono">
            Question #{currentIndex + 1}
          </Badge>

          <Badge
            variant="secondary"
            className="text-xs font-medium gap-1 text-foreground"
          >
            {question.questionType === "MCQ" ? (
              <>
                <CheckSquare className="size-3 text-sky-500" />
                <span>Multiple Choice</span>
              </>
            ) : (
              <>
                <Code2 className="size-3 text-emerald-500" />
                <span>Coding Problem</span>
              </>
            )}
          </Badge>

          <Badge
            variant="outline"
            className="text-xs font-semibold ml-auto text-primary border-primary/30"
          >
            {question.points} Points
          </Badge>
        </div>

        <h2 className="text-lg font-bold text-foreground tracking-tight">
          {question.title}
        </h2>
      </div>

      {/* Statement Markdown Content */}
      <div className="prose prose-sm dark:prose-invert max-w-none text-xs sm:text-sm leading-relaxed">
        <MarkdownPreview content={statement} />
      </div>

      {/* Public Sample Testcases (for Coding Problems) */}
      {question.questionType === "CODING" &&
        question.sampleTestCases &&
        question.sampleTestCases.length > 0 && (
          <div className="pt-4 space-y-3 border-t border-border/60">
            <h3 className="text-xs font-bold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
              <FileCode className="size-3.5 text-primary" />
              <span>Sample Test Cases</span>
            </h3>

            <div className="space-y-3">
              {question.sampleTestCases.map((tc, idx) => (
                <div
                  key={tc.id || idx}
                  className="rounded-lg border border-border bg-card/40 p-3 space-y-2 text-xs"
                >
                  <div className="font-semibold text-muted-foreground text-[11px]">
                    Example {idx + 1}
                  </div>

                  {tc.stdin && (
                    <div className="space-y-1">
                      <span className="text-[11px] font-medium text-muted-foreground">
                        Input:
                      </span>
                      <pre className="p-2 rounded bg-background border font-mono text-xs whitespace-pre-wrap">
                        {tc.stdin}
                      </pre>
                    </div>
                  )}

                  {tc.output && (
                    <div className="space-y-1">
                      <span className="text-[11px] font-medium text-muted-foreground">
                        Expected Output:
                      </span>
                      <pre className="p-2 rounded bg-background border font-mono text-xs whitespace-pre-wrap">
                        {tc.output}
                      </pre>
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}
    </div>
  )
}
