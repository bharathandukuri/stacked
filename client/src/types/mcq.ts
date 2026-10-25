export type McqType = "SINGLE_CHOICE" | "MULTIPLE_CHOICE" | "BOOLEAN"
export type McqDifficulty = "EASY" | "MEDIUM" | "HARD"
export type McqStatus = "DRAFT" | "PUBLISHED"

export interface McqOption {
  id: string
  text: string
  explanation?: string | null
}

export interface McqOptionPayload {
  id?: string
  text: string
  explanation?: string | null
}

export interface McqQuestionItem {
  id: string
  title: string
  contentSnippet: string
  type: McqType
  difficulty: McqDifficulty
  status: McqStatus
  topics: string[]
  tags: string[]
  optionsCount: number
  correctOptionsCount: number
  createdAt?: string
  updatedAt?: string
}

export interface McqQuestionDetail {
  id: string
  title: string
  content: string
  type: McqType
  difficulty: McqDifficulty
  status: McqStatus
  topics: string[]
  tags: string[]
  options: McqOption[]
  correctOptionIds: string[]
  explanation?: string | null
  shuffleOptions: boolean
  imageFileIds: string[]
  authorId?: string | null
  createdAt?: string
  updatedAt?: string
}

export interface CreateMcqPayload {
  title: string
  content: string
  type: McqType
  difficulty: McqDifficulty
  status?: McqStatus
  topics?: string[]
  tags?: string[]
  options: McqOptionPayload[]
  correctOptionIds: string[]
  explanation?: string | null
  shuffleOptions?: boolean
  imageFileIds?: string[]
}

export interface UpdateMcqPayload {
  title: string
  content: string
  type: McqType
  difficulty: McqDifficulty
  status: McqStatus
  topics?: string[]
  tags?: string[]
  options: McqOptionPayload[]
  correctOptionIds: string[]
  explanation?: string | null
  shuffleOptions?: boolean
  imageFileIds?: string[]
}

export interface McqTopicItem {
  id: string
  name: string
  slug: string
  description?: string | null
  questionCount: number
  createdAt?: string
  updatedAt?: string
}

export interface CreateMcqTopicPayload {
  name: string
  description?: string | null
}

export interface UpdateMcqTopicPayload {
  name: string
  description?: string | null
}

export interface McqFilterParams {
  search?: string
  topic?: string
  tag?: string
  difficulty?: McqDifficulty | ""
  type?: McqType | ""
  status?: McqStatus | ""
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: "ASC" | "DESC"
}

export interface McqStats {
  totalQuestions: number
  publishedQuestions: number
  draftQuestions: number
  singleChoiceCount: number
  multipleChoiceCount: number
  booleanCount: number
  easyCount: number
  mediumCount: number
  hardCount: number
  totalTopics: number
}

