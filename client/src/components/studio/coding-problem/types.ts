export type Difficulty = "EASY" | "MEDIUM" | "HARD"

export type ProblemType = "GENERIC" | "DATABASE"

export interface LanguageOption {
  id: string
  name: string
}

export const GENERIC_LANGUAGES: readonly LanguageOption[] = [
  { id: "javascript", name: "JavaScript" },
  { id: "typescript", name: "TypeScript" },
  { id: "python", name: "Python 3" },
  { id: "java", name: "Java" },
  { id: "cpp", name: "C++" },
  { id: "c", name: "C" },
  { id: "csharp", name: "C#" },
  { id: "go", name: "Go" },
  { id: "rust", name: "Rust" },
] as const

// Database problems strictly support only MySQL, PostgreSQL, and SQLite
export const DATABASE_LANGUAGES: readonly LanguageOption[] = [
  { id: "mysql", name: "MySQL" },
  { id: "postgresql", name: "PostgreSQL" },
  { id: "sqlite", name: "SQLite" },
] as const

export type InputStructure =
  "VARIABLE" | "ARRAY_1D" | "ARRAY_2D" | "ARRAY_3D" | "ARRAY_ND"

export interface InputStructureOption {
  id: InputStructure
  label: string
  shortLabel: string
  placeholder: string
  defaultBrackets: string
}

export const INPUT_STRUCTURES: readonly InputStructureOption[] = [
  {
    id: "VARIABLE",
    label: "Normal Variable",
    shortLabel: "Variable",
    placeholder: 'e.g. 9 or "target"',
    defaultBrackets: "",
  },
  {
    id: "ARRAY_1D",
    label: "1D Array",
    shortLabel: "1D Array",
    placeholder: "e.g. [2,7,11,15]",
    defaultBrackets: "[]",
  },
  {
    id: "ARRAY_2D",
    label: "2D Array",
    shortLabel: "2D Array",
    placeholder: "e.g. [[1,2],[3,4]]",
    defaultBrackets: "[[]]",
  },
  {
    id: "ARRAY_3D",
    label: "3D Array",
    shortLabel: "3D Array",
    placeholder: "e.g. [[[1,2]],[[3,4]]]",
    defaultBrackets: "[[[]]]",
  },
  {
    id: "ARRAY_ND",
    label: "N-D Array / Custom",
    shortLabel: "N-D Array",
    placeholder: "e.g. [[[[1]]]]",
    defaultBrackets: "[[]]",
  },
] as const

export interface ProblemParameter {
  name: string
  structure: InputStructure
}

export interface DatabaseColumn {
  name: string
  type: string
}

export interface DatabaseTableSchema {
  id: string
  name: string
  columns: DatabaseColumn[]
}

export interface ProblemExample {
  id: string
  inputs: Record<string, string> // paramName/tableName -> formatted value string or table JSON
  output: string // expected result string or output table JSON
  explanation?: string
}

export interface TestCase {
  id: string
  inputs: Record<string, string>
  expectedOutput: string
  isSample: boolean
  explanation?: string
  source?: string
}

export const SQL_COLUMN_TYPES = [
  "int",
  "bigint",
  "varchar",
  "text",
  "date",
  "datetime",
  "timestamp",
  "decimal",
  "float",
  "boolean",
] as const

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
