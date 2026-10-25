import {
  GraduationCap,
  Layers,
  FileQuestion,
  CheckSquare,
  Code2,
  CheckCircle2,
  AlertCircle,
  Sparkles,
  Loader2,
} from "lucide-react"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Input } from "@/components/ui/input"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { cn } from "@/lib/utils"
import type {
  AssessmentSelectionMode,
  QuestionBankPreviewDto,
  QuestionBankPreviewItemDto,
} from "@/types/assessment"
import type { QuestionBankSummary } from "@/types/question-bank"

export interface CuratedQuestionItem {
  questionId: string
  title: string
  questionType: "MCQ" | "CODING"
  difficulty?: string
  points: number
  slug?: string
  order?: number
}

interface StepQuestionSelectionProps {
  selectionMode: AssessmentSelectionMode
  onSelectionModeChange: (mode: AssessmentSelectionMode) => void
  selectedQuestionBankId: string
  onSelectedQuestionBankIdChange: (bankId: string) => void
  curatedQuestions: CuratedQuestionItem[]
  onOpenPicker: () => void
  onUpdatePoints: (questionId: string, points: number) => void
  onRemoveQuestion: (questionId: string) => void
  publishedBanks: QuestionBankSummary[]
  loadingBanks: boolean
  bankPreview: QuestionBankPreviewDto | undefined
  loadingBankPreview: boolean
}

export function StepQuestionSelection({
  selectionMode,
  onSelectionModeChange,
  selectedQuestionBankId,
  onSelectedQuestionBankIdChange,
  curatedQuestions,
  onOpenPicker,
  onUpdatePoints,
  onRemoveQuestion,
  publishedBanks,
  loadingBanks,
  bankPreview,
  loadingBankPreview,
}: StepQuestionSelectionProps) {
  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Question Source Strategy</CardTitle>
          <CardDescription>
            Question Banks provide validated, peer-reviewed questions with complete scoring rubrics.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <button
              type="button"
              onClick={() => onSelectionModeChange("QUESTION_BANK")}
              className={cn(
                "flex flex-col items-start p-4 rounded-xl border text-left transition-all cursor-pointer",
                selectionMode === "QUESTION_BANK"
                  ? "border-primary bg-primary/5 ring-2 ring-primary/20 shadow-xs"
                  : "border-border hover:bg-muted/40"
              )}
            >
              <div className="flex items-center gap-2 mb-1.5">
                <GraduationCap className="size-5 text-primary" />
                <span className="font-semibold text-foreground">
                  Published Question Bank (Recommended)
                </span>
              </div>
              <p className="text-xs text-muted-foreground">
                Select a published question bank. Its questions, judge configurations, and starter code are safely snapshotted into this immutable assessment.
              </p>
            </button>

            <button
              type="button"
              onClick={() => onSelectionModeChange("INDIVIDUAL_QUESTIONS")}
              className={cn(
                "flex flex-col items-start p-4 rounded-xl border text-left transition-all cursor-pointer",
                selectionMode === "INDIVIDUAL_QUESTIONS"
                  ? "border-primary bg-primary/5 ring-2 ring-primary/20 shadow-xs"
                  : "border-border hover:bg-muted/40"
              )}
            >
              <div className="flex items-center gap-2 mb-1.5">
                <Layers className="size-5 text-primary" />
                <span className="font-semibold text-foreground">
                  Custom Question Selection
                </span>
              </div>
              <p className="text-xs text-muted-foreground">
                Hand-pick individual published questions and customize question points manually.
              </p>
            </button>
          </div>
        </CardContent>
      </Card>

      {/* Primary Mode: Question Bank Selection & Live Preview */}
      {selectionMode === "QUESTION_BANK" && (
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Choose a Published Question Bank</CardTitle>
              <CardDescription>
                Only question banks in PUBLISHED status can be selected for assessments.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              {loadingBanks ? (
                <div className="py-8 flex items-center justify-center text-muted-foreground gap-2">
                  <Loader2 className="size-4 animate-spin text-primary" />
                  <span className="text-xs">Loading published question banks...</span>
                </div>
              ) : publishedBanks.length === 0 ? (
                <div className="p-8 border border-dashed rounded-lg text-center text-muted-foreground">
                  <AlertCircle className="size-8 mx-auto text-muted-foreground/50 mb-2" />
                  <p className="font-medium text-foreground">No Published Question Banks Found</p>
                  <p className="text-xs text-muted-foreground mt-1">
                    Go to Question Banks and publish a bank first, or switch to Custom Question Selection.
                  </p>
                </div>
              ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 max-h-72 overflow-y-auto p-1">
                  {publishedBanks.map((bank) => {
                    const isSelected = selectedQuestionBankId === bank.id
                    return (
                      <div
                        key={bank.id}
                        onClick={() => onSelectedQuestionBankIdChange(bank.id)}
                        className={cn(
                          "p-4 rounded-lg border transition-all cursor-pointer flex flex-col justify-between",
                          isSelected
                            ? "border-primary bg-primary/5 ring-2 ring-primary shadow-xs"
                            : "border-border hover:bg-muted/30"
                        )}
                      >
                        <div>
                          <div className="flex items-center justify-between mb-1">
                            <span className="font-semibold text-sm text-foreground">
                              {bank.title}
                            </span>
                            {isSelected && (
                              <CheckCircle2 className="size-4 text-primary shrink-0" />
                            )}
                          </div>
                          {bank.description && (
                            <p className="text-xs text-muted-foreground line-clamp-2 mb-2">
                              {bank.description}
                            </p>
                          )}
                          {bank.category && (
                            <Badge variant="outline" className="text-[10px] uppercase font-mono mb-2">
                              {bank.category}
                            </Badge>
                          )}
                        </div>
                        <div className="flex items-center gap-2 pt-2 border-t text-[11px] text-muted-foreground">
                          <span>{bank.totalQuestions} Questions</span>
                          <span>•</span>
                          <span>{bank.totalPoints} Pts</span>
                          <span>•</span>
                          <span>
                            {bank.mcqCount} MCQ / {bank.codingCount} Coding
                          </span>
                        </div>
                      </div>
                    )
                  })}
                </div>
              )}
            </CardContent>
          </Card>

          {/* Rich Bank Preview Panel */}
          {selectedQuestionBankId && (
            <Card className="border-primary/30">
              <CardHeader className="pb-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Sparkles className="size-4 text-primary" />
                    <CardTitle className="text-base">Selected Bank Snapshot Preview</CardTitle>
                  </div>
                  {loadingBankPreview ? (
                    <Loader2 className="size-3.5 animate-spin text-muted-foreground" />
                  ) : bankPreview?.eligibleForAssessment ? (
                    <Badge className="bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/30 gap-1">
                      <CheckCircle2 className="size-3" />
                      <span>Eligible for Assessment</span>
                    </Badge>
                  ) : (
                    <Badge variant="destructive" className="gap-1">
                      <AlertCircle className="size-3" />
                      <span>Ineligible Questions</span>
                    </Badge>
                  )}
                </div>
                <CardDescription>
                  This configuration and all questions below will be snapshotted permanently into this assessment upon creation.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                {loadingBankPreview ? (
                  <div className="py-6 flex items-center justify-center gap-2 text-muted-foreground text-xs">
                    <Loader2 className="size-4 animate-spin text-primary" />
                    Loading bank snapshot preview and judging configurations...
                  </div>
                ) : bankPreview ? (
                  <>
                    {!bankPreview.eligibleForAssessment && bankPreview.ineligibilityReason && (
                      <Alert variant="destructive">
                        <AlertCircle className="size-4" />
                        <AlertTitle>Bank Has Ineligible Questions</AlertTitle>
                        <AlertDescription className="text-xs">
                          {bankPreview.ineligibilityReason}
                        </AlertDescription>
                      </Alert>
                    )}

                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 p-3 bg-muted/40 rounded-lg border text-center text-xs">
                      <div>
                        <span className="text-muted-foreground">Total Questions</span>
                        <div className="text-base font-bold text-foreground mt-0.5">
                          {bankPreview.totalQuestions}
                        </div>
                      </div>
                      <div>
                        <span className="text-muted-foreground">MCQ vs Coding</span>
                        <div className="text-base font-bold text-foreground mt-0.5">
                          {bankPreview.mcqCount} MCQ / {bankPreview.codingCount} Code
                        </div>
                      </div>
                      <div>
                        <span className="text-muted-foreground">Total Score</span>
                        <div className="text-base font-bold text-foreground mt-0.5">
                          {bankPreview.totalPoints} Points
                        </div>
                      </div>
                      <div>
                        <span className="text-muted-foreground">Category</span>
                        <div className="text-base font-bold text-foreground mt-0.5">
                          {bankPreview.category || "General"}
                        </div>
                      </div>
                    </div>

                    <div className="flex flex-wrap items-center gap-2 text-xs">
                      <span className="text-muted-foreground font-medium">Difficulty Breakdown:</span>
                      {Object.entries(bankPreview.difficultyDistribution || {}).map(([diff, count]) => (
                        <Badge key={diff} variant="secondary" className="text-xs">
                          {diff}: {String(count)}
                        </Badge>
                      ))}
                    </div>

                    {bankPreview.supportedCodingLanguages && bankPreview.supportedCodingLanguages.length > 0 && (
                      <div className="flex flex-wrap items-center gap-2 text-xs">
                        <span className="text-muted-foreground font-medium flex items-center gap-1">
                          <Code2 className="size-3.5 text-primary" />
                          Supported Languages:
                        </span>
                        {bankPreview.supportedCodingLanguages.map((lang: string) => (
                          <Badge key={lang} variant="outline" className="font-mono text-[10px]">
                            {lang}
                          </Badge>
                        ))}
                      </div>
                    )}

                    <div className="border rounded-lg overflow-hidden">
                      <div className="px-3 py-2 bg-muted/50 border-b text-xs font-semibold text-muted-foreground">
                        Question Bank Contents ({bankPreview.items.length})
                      </div>
                      <div className="max-h-60 overflow-y-auto divide-y divide-border">
                        {bankPreview.items.map((item: QuestionBankPreviewItemDto, idx: number) => (
                          <div key={item.questionId} className="p-3 flex items-center justify-between text-xs gap-3">
                            <div className="flex items-center gap-2.5 min-w-0">
                              <span className="font-mono text-muted-foreground w-5 text-right">
                                {idx + 1}.
                              </span>
                              {item.questionType === "MCQ" ? (
                                <CheckSquare className="size-3.5 text-amber-500 shrink-0" />
                              ) : (
                                <Code2 className="size-3.5 text-blue-500 shrink-0" />
                              )}
                              <span className="font-medium text-foreground truncate">
                                {item.title}
                              </span>
                              <Badge variant="outline" className="text-[10px] uppercase shrink-0">
                                {item.difficulty}
                              </Badge>
                              {!item.eligible && (
                                <Badge variant="destructive" className="text-[10px] shrink-0" title={item.ineligibilityReason}>
                                  Issues
                                </Badge>
                              )}
                            </div>
                            <div className="flex items-center gap-3 shrink-0">
                              <span className="font-mono font-medium text-foreground">
                                {item.points} pts
                              </span>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  </>
                ) : null}
              </CardContent>
            </Card>
          )}
        </div>
      )}

      {/* Mode B: Individual Questions */}
      {selectionMode === "INDIVIDUAL_QUESTIONS" && (
        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <div>
              <CardTitle className="text-lg">Curated Assessment Questions</CardTitle>
              <CardDescription>
                Pick individual questions from the repository.
              </CardDescription>
            </div>
            <Button
              size="sm"
              onClick={onOpenPicker}
              className="gap-1.5 text-xs"
            >
              <span>Add Questions</span>
            </Button>
          </CardHeader>
          <CardContent>
            {curatedQuestions.length === 0 ? (
              <div className="p-8 border border-dashed rounded-lg text-center text-muted-foreground">
                <FileQuestion className="size-8 mx-auto text-muted-foreground/50 mb-2" />
                <p className="font-medium text-foreground">No Questions Added Yet</p>
                <p className="text-xs text-muted-foreground mt-1">
                  Click "Add Questions" to select published questions.
                </p>
              </div>
            ) : (
              <div className="border rounded-lg overflow-hidden divide-y divide-border">
                {curatedQuestions.map((q, idx) => (
                  <div key={q.questionId} className="p-3 flex items-center justify-between text-xs gap-3">
                    <div className="flex items-center gap-2.5 min-w-0">
                      <span className="font-mono text-muted-foreground w-5 text-right">
                        {idx + 1}.
                      </span>
                      {q.questionType === "MCQ" ? (
                        <CheckSquare className="size-3.5 text-amber-500 shrink-0" />
                      ) : (
                        <Code2 className="size-3.5 text-blue-500 shrink-0" />
                      )}
                      <span className="font-medium text-foreground truncate">
                        {q.title}
                      </span>
                    </div>
                    <div className="flex items-center gap-3 shrink-0">
                      <Input
                        type="number"
                        min={1}
                        className="w-16 h-7 text-xs font-mono text-center"
                        value={q.points}
                        onChange={(e) => onUpdatePoints(q.questionId, Number(e.target.value))}
                      />
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => onRemoveQuestion(q.questionId)}
                        className="h-7 px-2 text-destructive hover:bg-destructive/10"
                      >
                        Remove
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  )
}
