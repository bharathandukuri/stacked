import { CheckSquare, Code2 } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { MarkdownPreview } from "@/components/markdown-preview/markdown-preview"
import { TableEmptyState } from "@/components/common/table-empty-state"
import type { AssessmentItemResponse, OptionSnapshot } from "@/types/assessment"

interface AssessmentQuestionsTabProps {
  items: AssessmentItemResponse[] | undefined
}

export function AssessmentQuestionsTab({ items }: AssessmentQuestionsTabProps) {
  if (!items || items.length === 0) {
    return (
      <TableEmptyState
        title="No Questions Configured"
        description="This assessment does not have any questions attached."
      />
    )
  }

  return (
    <div className="space-y-4">
      {items.map((item, index) => (
        <Card key={item.questionId} className="border bg-card">
          <CardHeader className="pb-3">
            <div className="flex items-start justify-between gap-4">
              <div className="flex items-center gap-3">
                <span className="flex h-7 w-7 items-center justify-center rounded-full bg-primary/10 text-xs font-bold text-primary shrink-0">
                  {index + 1}
                </span>
                <div>
                  <CardTitle className="text-base flex items-center gap-2">
                    <span>{item.title}</span>
                    <Badge variant="outline" className="text-xs">
                      {item.points} pts
                    </Badge>
                  </CardTitle>
                  <CardDescription className="flex items-center gap-2 mt-1">
                    {item.questionType === "MCQ" ? (
                      <span className="inline-flex items-center gap-1 text-xs text-sky-600 dark:text-sky-400">
                        <CheckSquare className="h-3 w-3" />
                        <span>MCQ ({item.mcqType?.replace("_", " ")})</span>
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 text-xs text-purple-600 dark:text-purple-400">
                        <Code2 className="h-3 w-3" />
                        <span>Coding Problem</span>
                      </span>
                    )}
                  </CardDescription>
                </div>
              </div>
            </div>
          </CardHeader>

          <CardContent className="space-y-3 pt-0">
            {item.questionType === "MCQ" && (
              <div className="space-y-2">
                {item.mcqContent && (
                  <div className="text-sm text-foreground/90 prose dark:prose-invert max-w-none">
                    <MarkdownPreview content={item.mcqContent} />
                  </div>
                )}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-2">
                  {item.options?.map((opt: OptionSnapshot) => {
                    const isCorrect = item.correctOptionIds?.includes(opt.id)
                    return (
                      <div
                        key={opt.id}
                        className={`flex items-center justify-between p-2.5 rounded-md border text-xs ${
                          isCorrect
                            ? "border-emerald-500/50 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300 font-medium"
                            : "border-border/60 bg-muted/40 text-foreground"
                        }`}
                      >
                        <div className="prose dark:prose-invert max-w-none text-xs">
                          <MarkdownPreview content={opt.content} />
                        </div>
                        {isCorrect && (
                          <Badge className="bg-emerald-600 text-white text-[10px] px-1.5 py-0 h-4 ml-2 shrink-0">
                            Correct
                          </Badge>
                        )}
                      </div>
                    )
                  })}
                </div>
              </div>
            )}

            {item.questionType === "CODING" && (
              <div className="space-y-2 text-xs">
                {item.codingStatement && (
                  <div className="text-sm text-muted-foreground line-clamp-3 prose dark:prose-invert max-w-none">
                    <MarkdownPreview content={item.codingStatement} />
                  </div>
                )}
                <div className="flex flex-wrap gap-2 pt-1">
                  <span className="text-muted-foreground font-medium">
                    Supported Languages:
                  </span>
                  {item.supportedLanguageIds?.map((lang: string) => (
                    <Badge
                      key={lang}
                      variant="secondary"
                      className="text-[11px] font-mono"
                    >
                      {lang}
                    </Badge>
                  ))}
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      ))}
    </div>
  )
}
