export type Difficulty = "EASY" | "MEDIUM" | "HARD"

export type ProblemType = "GENERIC" | "DATABASE"

export type { LanguageConfig, ProblemCategory } from "@/config/languages"

export {
  LANGUAGE_REGISTRY,
  getAllLanguages,
  getGenericLanguages,
  getDatabaseLanguages,
  getLanguageById,
  getDefaultLanguageIds,
  getLanguageCheckboxOptions,
} from "@/config/languages"

import { getGenericLanguages, getDatabaseLanguages } from "@/config/languages"

export interface LanguageOption {
  id: string
  name: string
}

export const GENERIC_LANGUAGES: readonly LanguageOption[] =
  getGenericLanguages().map((l) => ({
    id: l.id,
    name: l.name,
  }))

export const DATABASE_LANGUAGES: readonly LanguageOption[] =
  getDatabaseLanguages().map((l) => ({
    id: l.id,
    name: l.name,
  }))

export interface TestCase {
  id: string
  title?: string
  input: string // Generic: raw input lines; Database: SQL Init Script
  inputTable?: string // Database: Input Table in Markdown for problem example display
  output?: string // Generic: expected return value; Database: Expected Result Table (Markdown)
  explanation?: string
  imageUrl?: string
  imageFileId?: string
}

export type ProblemExample = TestCase

export const POPULAR_TOPIC_TAGS = [
  "Array",
  "String",
  "Hash Table",
  "Dynamic Programming",
  "Math",
  "Sorting",
  "Greedy",
  "Depth-First Search",
  "Binary Search",
  "Matrix",
  "Tree",
  "Breadth-First Search",
  "Two Pointers",
  "Bit Manipulation",
  "Stack",
  "Graph",
  "Heap (Priority Queue)",
  "Sliding Window",
  "Backtracking",
  "Union Find",
  "Recursion",
  "Trie",
  "Database",
  "SQL",
] as const

export const POPULAR_COMPANIES = [
  "Google",
  "Amazon",
  "Meta",
  "Microsoft",
  "Apple",
  "Uber",
  "Netflix",
  "Bloomberg",
  "LinkedIn",
  "ByteDance",
  "Adobe",
  "Salesforce",
  "Goldman Sachs",
  "Oracle",
  "Walmart",
  "Stripe",
  "Airbnb",
] as const
