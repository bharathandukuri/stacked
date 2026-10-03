import { z } from "zod"

export const testCaseSchema = z.object({
  id: z.string(),
  title: z.string().optional(),
  input: z.string().trim().min(1, "Input is required"),
  inputTable: z.string().optional(),
  output: z.string().optional(),
  explanation: z.string().optional(),
  imageUrl: z.string().optional(),
  imageFileId: z.string().optional(),
})

export const problemExampleSchema = testCaseSchema

export const codingProblemSchema = z.object({
  title: z
    .string()
    .trim()
    .min(1, "Title is required")
    .max(150, "Title must be 150 characters or fewer"),
  slug: z
    .string()
    .trim()
    .min(1, "Slug is required")
    .regex(
      /^[a-z0-9-]+$/,
      "Slug must contain only lowercase letters, numbers, and hyphens"
    ),
  difficulty: z.enum(["EASY", "MEDIUM", "HARD"]),
  problemType: z.enum(["GENERIC", "DATABASE"]),
  languages: z
    .array(z.string())
    .min(1, "Select at least one supported language"),
  description: z.string().trim().min(1, "Problem description is required"),
  constraints: z.array(z.string()),
  hints: z.array(z.string()),
  topics: z.array(z.string()),
  companies: z.array(z.string()),
  testCases: z
    .array(testCaseSchema)
    .min(1, "At least one example / test case is required"),
})

export type CodingProblemValues = z.infer<typeof codingProblemSchema>

// Deprecated aliases kept for smooth transition
export const statementStepSchema = codingProblemSchema
export type StatementStepValues = CodingProblemValues
export const testcasesStepSchema = codingProblemSchema
export type TestcasesStepValues = CodingProblemValues
