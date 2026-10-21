import { useState, useDeferredValue } from "react"
import {
  Search,
  Code2,
  CheckSquare,
  Check,
  Loader2,
  ChevronLeft,
  ChevronRight,
  Sparkles,
} from "lucide-react"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog"
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { Checkbox } from "@/components/ui/checkbox"
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { useMcqQuestions, useMcqTopics } from "@/hooks/use-mcq"
import { useCodingProblems } from "@/hooks/use-coding-problems"
import type { BankQuestionType } from "@/types/question-bank"
import type { McqQuestionItem, McqDifficulty, McqType } from "@/types/mcq"
import type { CodingProblemSummary } from "@/services/coding-problem-service"

export interface SelectedQuestionItem {
  questionType: BankQuestionType
  questionId: string
  title: string
  slug: string
  snippet?: string
  difficulty?: string
  topics?: string[]
  defaultPoints?: number
}

interface QuestionPickerDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  existingQuestionIds: Set<string>
  onAddQuestions: (selected: SelectedQuestionItem[]) => void
}

export function QuestionPickerDialog({
  open,
  onOpenChange,
  existingQuestionIds,
  onAddQuestions,
}: QuestionPickerDialogProps) {
  const [activeTab, setActiveTab] = useState<string>("mcq")
  const [stagedMap, setStagedMap] = useState<Map<string, SelectedQuestionItem>>(
    new Map()
  )

  // MCQ Tab Filters & Pagination
  const [mcqSearch, setMcqSearch] = useState("")
  const deferredMcqSearch = useDeferredValue(mcqSearch)
  const [mcqDifficulty, setMcqDifficulty] = useState<string>("ALL")
  const [mcqType, setMcqType] = useState<string>("ALL")
  const [mcqTopic, setMcqTopic] = useState<string>("ALL")
  const [mcqPage, setMcqPage] = useState(0)

  // Coding Tab Filters & Pagination
  const [codingSearch, setCodingSearch] = useState("")
  const deferredCodingSearch = useDeferredValue(codingSearch)
  const [codingDifficulty, setCodingDifficulty] = useState<string>("ALL")
  const [codingType, setCodingType] = useState<string>("ALL")
  const [codingPage, setCodingPage] = useState(0)

  // Data queries
  const { data: mcqTopics = [] } = useMcqTopics()

  const { data: mcqData, isLoading: mcqLoading } = useMcqQuestions({
    search: deferredMcqSearch.trim() || undefined,
    status: "PUBLISHED",
    difficulty:
      mcqDifficulty !== "ALL" ? (mcqDifficulty as McqDifficulty) : undefined,
    type: mcqType !== "ALL" ? (mcqType as McqType) : undefined,
    topic: mcqTopic !== "ALL" ? mcqTopic : undefined,
    page: mcqPage,
    size: 6,
  })

  const { data: codingData, isLoading: codingLoading } = useCodingProblems({
    search: deferredCodingSearch.trim() || undefined,
    status: "PUBLISHED",
    difficulty:
      codingDifficulty !== "ALL"
        ? (codingDifficulty as "EASY" | "MEDIUM" | "HARD")
        : undefined,
    problemType:
      codingType !== "ALL"
        ? (codingType as "GENERIC" | "DATABASE")
        : undefined,
    page: codingPage,
    size: 6,
  })

  const toggleSelectMcq = (mcq: McqQuestionItem) => {
    setStagedMap((prev) => {
      const next = new Map(prev)
      if (next.has(mcq.id)) {
        next.delete(mcq.id)
      } else {
        next.set(mcq.id, {
          questionType: "MCQ",
          questionId: mcq.id,
          title: mcq.title,
          slug: mcq.slug,
          snippet: mcq.contentSnippet,
          difficulty: mcq.difficulty,
          topics: mcq.topics,
          defaultPoints: mcq.defaultPoints,
        })
      }
      return next
    })
  }

  const toggleSelectCoding = (coding: CodingProblemSummary) => {
    setStagedMap((prev) => {
      const next = new Map(prev)
      if (next.has(coding.id)) {
        next.delete(coding.id)
      } else {
        const fallbackPoints =
          coding.difficulty === "HARD"
            ? 30
            : coding.difficulty === "MEDIUM"
              ? 20
              : 10
        next.set(coding.id, {
          questionType: "CODING",
          questionId: coding.id,
          title: coding.title,
          slug: coding.slug,
          difficulty: coding.difficulty,
          topics: coding.topics,
          defaultPoints: fallbackPoints,
        })
      }
      return next
    })
  }

  const handleConfirm = () => {
    onAddQuestions(Array.from(stagedMap.values()))
    setStagedMap(new Map())
    onOpenChange(false)
  }

  const stagedCount = stagedMap.size
  const stagedMcqs = Array.from(stagedMap.values()).filter(
    (i) => i.questionType === "MCQ"
  ).length
  const stagedCodings = stagedCount - stagedMcqs

  const renderDifficultyBadge = (difficulty?: string) => {
    switch (difficulty) {
      case "EASY":
        return (
          <Badge
            variant="outline"
            className="border-emerald-500/30 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
          >
            Easy
          </Badge>
        )
      case "MEDIUM":
        return (
          <Badge
            variant="outline"
            className="border-amber-500/30 bg-amber-500/10 text-amber-600 dark:text-amber-400"
          >
            Medium
          </Badge>
        )
      case "HARD":
        return (
          <Badge
            variant="outline"
            className="border-rose-500/30 bg-rose-500/10 text-rose-600 dark:text-rose-400"
          >
            Hard
          </Badge>
        )
      default:
        return null
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="flex max-h-[88vh] w-full max-w-4xl flex-col gap-4 overflow-hidden p-6 sm:max-w-4xl">
        <DialogHeader className="border-b border-border pb-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <div className="flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <Sparkles className="size-5" />
              </div>
              <div>
                <DialogTitle className="text-xl font-bold">
                  Select Questions for Bank
                </DialogTitle>
                <DialogDescription className="text-xs text-muted-foreground">
                  Browse and pick curated MCQs and Coding Problems to bundle into this bank.
                </DialogDescription>
              </div>
            </div>
            {stagedCount > 0 && (
              <Badge variant="secondary" className="px-3 py-1 text-xs">
                {stagedCount} selected ({stagedMcqs} MCQ, {stagedCodings} Coding)
              </Badge>
            )}
          </div>
        </DialogHeader>

        <Tabs
          value={activeTab}
          onValueChange={setActiveTab}
          className="flex min-h-0 flex-1 flex-col"
        >
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="mcq" className="flex items-center gap-2">
              <CheckSquare className="size-4 text-primary" />
              <span>Multiple Choice ({mcqData?.totalElements ?? 0})</span>
            </TabsTrigger>
            <TabsTrigger value="coding" className="flex items-center gap-2">
              <Code2 className="size-4 text-blue-500" />
              <span>Coding Problems ({codingData?.totalElements ?? 0})</span>
            </TabsTrigger>
          </TabsList>

          {/* MCQ TAB */}
          <TabsContent value="mcq" className="mt-3 flex min-h-0 flex-1 flex-col gap-3">
            {/* Filters Bar */}
            <div className="flex flex-wrap items-center gap-2">
              <div className="relative min-w-[200px] flex-1">
                <Search className="absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  placeholder="Search questions by title or text..."
                  value={mcqSearch}
                  onChange={(e) => {
                    setMcqSearch(e.target.value)
                    setMcqPage(0)
                  }}
                  className="pl-9 text-xs"
                />
              </div>

              <NativeSelect
                value={mcqDifficulty}
                onChange={(e) => {
                  setMcqDifficulty(e.target.value)
                  setMcqPage(0)
                }}
                size="sm"
                className="w-32"
              >
                <NativeSelectOption value="ALL">Difficulty: All</NativeSelectOption>
                <NativeSelectOption value="EASY">Easy</NativeSelectOption>
                <NativeSelectOption value="MEDIUM">Medium</NativeSelectOption>
                <NativeSelectOption value="HARD">Hard</NativeSelectOption>
              </NativeSelect>

              <NativeSelect
                value={mcqType}
                onChange={(e) => {
                  setMcqType(e.target.value)
                  setMcqPage(0)
                }}
                size="sm"
                className="w-36"
              >
                <NativeSelectOption value="ALL">Type: All</NativeSelectOption>
                <NativeSelectOption value="SINGLE_CHOICE">Single Choice</NativeSelectOption>
                <NativeSelectOption value="MULTIPLE_CHOICE">Multiple Choice</NativeSelectOption>
                <NativeSelectOption value="BOOLEAN">True / False</NativeSelectOption>
              </NativeSelect>

              {mcqTopics.length > 0 && (
                <NativeSelect
                  value={mcqTopic}
                  onChange={(e) => {
                    setMcqTopic(e.target.value)
                    setMcqPage(0)
                  }}
                  size="sm"
                  className="w-32"
                >
                  <NativeSelectOption value="ALL">Topic: All</NativeSelectOption>
                  {mcqTopics.map((topic) => (
                    <NativeSelectOption key={topic.id} value={topic.name}>
                      {topic.name}
                    </NativeSelectOption>
                  ))}
                </NativeSelect>
              )}
            </div>

            {/* MCQ List Table */}
            <div className="min-h-0 flex-1 overflow-auto rounded-md border border-border">
              {mcqLoading ? (
                <div className="flex h-56 flex-col items-center justify-center gap-2 text-muted-foreground">
                  <Loader2 className="size-6 animate-spin text-primary" />
                  <span className="text-xs">Loading MCQ questions...</span>
                </div>
              ) : !mcqData || mcqData.items.length === 0 ? (
                <div className="flex h-56 flex-col items-center justify-center gap-2 text-muted-foreground">
                  <CheckSquare className="size-8 opacity-40" />
                  <p className="text-sm font-medium">No MCQ questions found</p>
                  <span className="text-xs">Try adjusting your search or filters.</span>
                </div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="w-12 text-center">Select</TableHead>
                      <TableHead>Question Title</TableHead>
                      <TableHead className="w-24">Difficulty</TableHead>
                      <TableHead className="w-28">Type</TableHead>
                      <TableHead className="w-20 text-right">Points</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {mcqData.items.map((mcq) => {
                      const alreadyInBank = existingQuestionIds.has(mcq.id)
                      const isStaged = stagedMap.has(mcq.id)

                      return (
                        <TableRow
                          key={mcq.id}
                          className={`cursor-pointer transition-colors ${
                            alreadyInBank
                              ? "opacity-60 bg-muted/30"
                              : isStaged
                                ? "bg-primary/5 hover:bg-primary/10"
                                : "hover:bg-muted/50"
                          }`}
                          onClick={() => {
                            if (!alreadyInBank) toggleSelectMcq(mcq)
                          }}
                        >
                          <TableCell
                            className="text-center"
                            onClick={(e) => e.stopPropagation()}
                          >
                            <Checkbox
                              checked={isStaged || alreadyInBank}
                              disabled={alreadyInBank}
                              onCheckedChange={() => {
                                if (!alreadyInBank) toggleSelectMcq(mcq)
                              }}
                            />
                          </TableCell>
                          <TableCell>
                            <div className="flex flex-col gap-0.5">
                              <div className="flex items-center gap-2">
                                <span className="font-medium text-foreground text-sm line-clamp-1">
                                  {mcq.title}
                                </span>
                                {alreadyInBank && (
                                  <Badge variant="outline" className="text-[10px] py-0 px-1.5 text-muted-foreground">
                                    Already in Bank
                                  </Badge>
                                )}
                              </div>
                              {mcq.topics && mcq.topics.length > 0 && (
                                <div className="flex flex-wrap gap-1 mt-0.5">
                                  {mcq.topics.slice(0, 3).map((topic) => (
                                    <span
                                      key={topic}
                                      className="text-[10px] text-muted-foreground bg-secondary px-1.5 py-0.2 rounded"
                                    >
                                      {topic}
                                    </span>
                                  ))}
                                </div>
                              )}
                            </div>
                          </TableCell>
                          <TableCell>{renderDifficultyBadge(mcq.difficulty)}</TableCell>
                          <TableCell>
                            <span className="text-xs text-muted-foreground">
                              {mcq.type === "SINGLE_CHOICE"
                                ? "Single Choice"
                                : mcq.type === "MULTIPLE_CHOICE"
                                  ? "Multiple Choice"
                                  : "True / False"}
                            </span>
                          </TableCell>
                          <TableCell className="text-right text-xs font-semibold">
                            {mcq.defaultPoints} pts
                          </TableCell>
                        </TableRow>
                      )
                    })}
                  </TableBody>
                </Table>
              )}
            </div>

            {/* MCQ Pagination */}
            {mcqData && mcqData.totalPages > 1 && (
              <div className="flex items-center justify-between border-t border-border pt-2 text-xs text-muted-foreground">
                <span>
                  Page {mcqData.page + 1} of {mcqData.totalPages} ({mcqData.totalElements} total)
                </span>
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={mcqData.isFirst}
                    onClick={() => setMcqPage((p) => Math.max(0, p - 1))}
                  >
                    <ChevronLeft className="size-4" />
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={mcqData.isLast}
                    onClick={() => setMcqPage((p) => p + 1)}
                  >
                    Next
                    <ChevronRight className="size-4" />
                  </Button>
                </div>
              </div>
            )}
          </TabsContent>

          {/* CODING PROBLEMS TAB */}
          <TabsContent value="coding" className="mt-3 flex min-h-0 flex-1 flex-col gap-3">
            {/* Filters Bar */}
            <div className="flex flex-wrap items-center gap-2">
              <div className="relative min-w-[200px] flex-1">
                <Search className="absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  placeholder="Search coding problems..."
                  value={codingSearch}
                  onChange={(e) => {
                    setCodingSearch(e.target.value)
                    setCodingPage(0)
                  }}
                  className="pl-9 text-xs"
                />
              </div>

              <NativeSelect
                value={codingDifficulty}
                onChange={(e) => {
                  setCodingDifficulty(e.target.value)
                  setCodingPage(0)
                }}
                size="sm"
                className="w-32"
              >
                <NativeSelectOption value="ALL">Difficulty: All</NativeSelectOption>
                <NativeSelectOption value="EASY">Easy</NativeSelectOption>
                <NativeSelectOption value="MEDIUM">Medium</NativeSelectOption>
                <NativeSelectOption value="HARD">Hard</NativeSelectOption>
              </NativeSelect>

              <NativeSelect
                value={codingType}
                onChange={(e) => {
                  setCodingType(e.target.value)
                  setCodingPage(0)
                }}
                size="sm"
                className="w-36"
              >
                <NativeSelectOption value="ALL">Type: All</NativeSelectOption>
                <NativeSelectOption value="GENERIC">Algorithmic</NativeSelectOption>
                <NativeSelectOption value="DATABASE">Database / SQL</NativeSelectOption>
              </NativeSelect>
            </div>

            {/* Coding Problems List Table */}
            <div className="min-h-0 flex-1 overflow-auto rounded-md border border-border">
              {codingLoading ? (
                <div className="flex h-56 flex-col items-center justify-center gap-2 text-muted-foreground">
                  <Loader2 className="size-6 animate-spin text-primary" />
                  <span className="text-xs">Loading coding problems...</span>
                </div>
              ) : !codingData || codingData.items.length === 0 ? (
                <div className="flex h-56 flex-col items-center justify-center gap-2 text-muted-foreground">
                  <Code2 className="size-8 opacity-40" />
                  <p className="text-sm font-medium">No coding problems found</p>
                  <span className="text-xs">Try adjusting your search or filters.</span>
                </div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="w-12 text-center">Select</TableHead>
                      <TableHead>Problem Title</TableHead>
                      <TableHead className="w-24">Difficulty</TableHead>
                      <TableHead className="w-28">Type</TableHead>
                      <TableHead className="w-20 text-right">Default Pts</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {codingData.items.map((prob) => {
                      const alreadyInBank = existingQuestionIds.has(prob.id)
                      const isStaged = stagedMap.has(prob.id)
                      const defaultPts =
                        prob.difficulty === "HARD"
                          ? 30
                          : prob.difficulty === "MEDIUM"
                            ? 20
                            : 10

                      return (
                        <TableRow
                          key={prob.id}
                          className={`cursor-pointer transition-colors ${
                            alreadyInBank
                              ? "opacity-60 bg-muted/30"
                              : isStaged
                                ? "bg-primary/5 hover:bg-primary/10"
                                : "hover:bg-muted/50"
                          }`}
                          onClick={() => {
                            if (!alreadyInBank) toggleSelectCoding(prob)
                          }}
                        >
                          <TableCell
                            className="text-center"
                            onClick={(e) => e.stopPropagation()}
                          >
                            <Checkbox
                              checked={isStaged || alreadyInBank}
                              disabled={alreadyInBank}
                              onCheckedChange={() => {
                                if (!alreadyInBank) toggleSelectCoding(prob)
                              }}
                            />
                          </TableCell>
                          <TableCell>
                            <div className="flex flex-col gap-0.5">
                              <div className="flex items-center gap-2">
                                <span className="font-medium text-foreground text-sm line-clamp-1">
                                  {prob.title}
                                </span>
                                {alreadyInBank && (
                                  <Badge variant="outline" className="text-[10px] py-0 px-1.5 text-muted-foreground">
                                    Already in Bank
                                  </Badge>
                                )}
                              </div>
                              {prob.topics && prob.topics.length > 0 && (
                                <div className="flex flex-wrap gap-1 mt-0.5">
                                  {prob.topics.slice(0, 3).map((topic) => (
                                    <span
                                      key={topic}
                                      className="text-[10px] text-muted-foreground bg-secondary px-1.5 py-0.2 rounded"
                                    >
                                      {topic}
                                    </span>
                                  ))}
                                </div>
                              )}
                            </div>
                          </TableCell>
                          <TableCell>{renderDifficultyBadge(prob.difficulty)}</TableCell>
                          <TableCell>
                            <span className="text-xs text-muted-foreground">
                              {prob.problemType === "DATABASE" ? "Database / SQL" : "Algorithmic"}
                            </span>
                          </TableCell>
                          <TableCell className="text-right text-xs font-semibold">
                            {defaultPts} pts
                          </TableCell>
                        </TableRow>
                      )
                    })}
                  </TableBody>
                </Table>
              )}
            </div>

            {/* Coding Pagination */}
            {codingData && codingData.totalPages > 1 && (
              <div className="flex items-center justify-between border-t border-border pt-2 text-xs text-muted-foreground">
                <span>
                  Page {codingData.page + 1} of {codingData.totalPages} ({codingData.totalElements} total)
                </span>
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={codingData.isFirst}
                    onClick={() => setCodingPage((p) => Math.max(0, p - 1))}
                  >
                    <ChevronLeft className="size-4" />
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={codingData.isLast}
                    onClick={() => setCodingPage((p) => p + 1)}
                  >
                    Next
                    <ChevronRight className="size-4" />
                  </Button>
                </div>
              </div>
            )}
          </TabsContent>
        </Tabs>

        <DialogFooter className="mt-2 flex items-center justify-between border-t border-border pt-3">
          <div className="text-xs text-muted-foreground">
            {stagedCount === 0 ? (
              <span>Select questions above to add them to this bank</span>
            ) : (
              <span className="font-medium text-foreground">
                Ready to add {stagedCount} question{stagedCount > 1 ? "s" : ""}
              </span>
            )}
          </div>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                setStagedMap(new Map())
                onOpenChange(false)
              }}
            >
              Cancel
            </Button>
            <Button
              size="sm"
              disabled={stagedCount === 0}
              onClick={handleConfirm}
              className="gap-1.5"
            >
              <Check className="size-4" />
              Add Selected ({stagedCount})
            </Button>
          </div>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
