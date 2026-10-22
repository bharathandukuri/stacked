export type BankQuestionType = "CODING" | "MCQ"
export type QuestionBankStatus = "DRAFT" | "PUBLISHED"

export interface QuestionBankItemPayload {
  questionType: BankQuestionType
  questionId: string
  order: number
  customPoints?: number | null
}

export interface HydratedQuestionItem {
  questionType: BankQuestionType
  questionId: string
  order: number
  customPoints?: number | null
  effectivePoints: number
  title: string
  slug: string
  snippet?: string | null
  difficulty?: string | null
  topics?: string[]
}

export interface QuestionBankSummary {
  id: string
  slug: string
  title: string
  description?: string | null
  category?: string | null
  status: QuestionBankStatus
  tags?: string[]
  totalQuestions: number
  mcqCount: number
  codingCount: number
  totalPoints: number
  createdAt?: string
  updatedAt?: string
}

export interface QuestionBankDetail {
  id: string
  slug: string
  title: string
  description?: string | null
  category?: string | null
  status: QuestionBankStatus
  tags?: string[]
  totalQuestions: number
  mcqCount: number
  codingCount: number
  totalPoints: number
  items: HydratedQuestionItem[]
  authorId?: string | null
  createdAt?: string
  updatedAt?: string
}

export interface CreateQuestionBankPayload {
  title: string
  description?: string
  category?: string
  status?: QuestionBankStatus
  tags?: string[]
  items?: QuestionBankItemPayload[]
}

export interface UpdateQuestionBankPayload {
  title: string
  description?: string
  category?: string
  status?: QuestionBankStatus
  tags?: string[]
  items?: QuestionBankItemPayload[]
}

export interface QuestionBankFilterParams {
  search?: string
  category?: string
  status?: QuestionBankStatus | ""
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: "ASC" | "DESC"
}

export interface QuestionBankStats {
  totalBanks: number
  publishedBanks: number
  draftBanks: number
  totalCuratedQuestions: number
  totalCategories: number
}

