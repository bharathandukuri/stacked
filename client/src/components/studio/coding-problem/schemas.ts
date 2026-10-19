import { z } from "zod";
import { DATABASE_LANGUAGES } from "./types";

const DATABASE_LANGUAGE_IDS: ReadonlySet<string> = new Set([
  ...DATABASE_LANGUAGES.map((l) => l.id),
  "mysql-8.0",
  "postgresql-16",
  "sqlite-3",
  "sqlite-3.45",
  "mongodb-8.0",
]);

/**
 * Difficulty levels supported by the platform.
 */
export const difficultySchema = z.enum(["EASY", "MEDIUM", "HARD"]);
export type Difficulty = z.infer<typeof difficultySchema>;

/**
 * Problem categories supported by the platform.
 */
export const problemTypeSchema = z.enum(["GENERIC", "DATABASE"]);
export type ProblemType = z.infer<typeof problemTypeSchema>;

/**
 * Problem slug schema:
 * lowercase letters, numbers, and single hyphens, 1-180 characters.
 */
export const slugSchema = z
  .string()
  .trim()
  .min(1, "Problem slug is required")
  .max(180, "Slug must not exceed 180 characters")
  .regex(
    /^[a-z0-9]+(?:-[a-z0-9]+)*$/,
    "Slug must contain only lowercase letters, numbers, and single hyphens"
  );

/**
 * Problem title schema: 1-150 characters.
 */
export const titleSchema = z
  .string()
  .trim()
  .min(1, "Problem title is required")
  .max(150, "Title must not exceed 150 characters");

/**
 * Studio UI Test Case Schema.
 */
export const testCaseSchema = z.object({
  id: z.string().min(1, "Testcase ID is required"),
  title: z.string().optional(),
  input: z.string().default(""),
  inputTable: z.string().optional(),
  output: z.string().optional(),
  explanation: z.string().optional(),
  imageUrl: z.string().optional(),
  imageFileId: z.string().optional(),
});
export type TestCaseValues = z.infer<typeof testCaseSchema>;

/**
 * Form Schema for Problem Details (Step 1 of Problem Studio).
 * Used by CodingProblemForm and useAppForm.
 */
export const codingProblemSchema = z
  .object({
    title: titleSchema,
    slug: slugSchema,
    problemType: problemTypeSchema,
    difficulty: difficultySchema,
    languages: z
      .array(z.string().min(1, "Language ID cannot be blank"))
      .min(1, "At least one supported language is required"),
    description: z.string().trim().min(1, "Problem statement is required"),
    testCases: z
      .array(testCaseSchema)
      .min(1, "At least one testcase is required"),
    topics: z
      .array(z.string().trim().min(1, "Topic tag cannot be blank"))
      .min(1, "At least one topic tag is required"),
    hints: z
      .array(z.string().trim().min(1, "Hint cannot be blank"))
      .default([]),
  })
  .superRefine((data, ctx) => {
    if (data.problemType === "DATABASE") {
      if (data.languages.length !== 1) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message:
            "A database problem must support exactly one database language.",
          path: ["languages"],
        });
      } else if (
        DATABASE_LANGUAGE_IDS.size > 0 &&
        !DATABASE_LANGUAGE_IDS.has(data.languages[0])
      ) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: `Language '${data.languages[0]}' is not a database language for a DATABASE problem.`,
          path: ["languages"],
        });
      }
    } else if (data.problemType === "GENERIC") {
      const invalidDbLangs = data.languages.filter((id) =>
        DATABASE_LANGUAGE_IDS.has(id)
      );
      if (invalidDbLangs.length > 0) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: `Database language '${invalidDbLangs[0]}' cannot be used for a GENERIC problem.`,
          path: ["languages"],
        });
      }
    }
  });

export type CodingProblemValues = z.infer<typeof codingProblemSchema>;

/**
 * Default values for initializing problem studio.
 */
export const defaultCodingProblemValues: CodingProblemValues = {
  title: "",
  slug: "",
  problemType: "GENERIC",
  difficulty: "EASY",
  languages: ["javascript-node-20", "python-3.12", "java-21"],
  description: "",
  testCases: [
    {
      id: "case-1",
      title: "Case 1",
      input: "",
    },
  ],
  topics: [],
  hints: [],
};

/**
 * Backend Testcase Request schema (matching TestcaseRequest.java).
 */
export const testcaseRequestSchema = z.object({
  stdin: z.string().optional().nullable(),
  dbInit: z.string().optional().nullable(),
});
export type TestcaseRequest = z.infer<typeof testcaseRequestSchema>;

/**
 * Per-language execution constraints schema (matching ExecutionConstraintRequest.java).
 */
export const executionConstraintSchema = z.object({
  timeLimitMs: z
    .number()
    .positive("Time limit must be greater than zero")
    .max(60000, "Time limit cannot exceed 60000 ms")
    .optional(),
  memoryLimitKb: z
    .number()
    .positive("Memory limit must be greater than zero")
    .max(1048576, "Memory limit cannot exceed 1048576 KB")
    .optional(),
});
export type ExecutionConstraint = z.infer<typeof executionConstraintSchema>;

/**
 * Complete Create Coding Problem API Request Schema (matching CreateCodingProblemRequest.java).
 * Validates the payload submitted to POST /api/coding-problem.
 */
export const createCodingProblemSchema = z
  .object({
    title: titleSchema,
    slug: slugSchema,
    problemType: problemTypeSchema,
    difficulty: difficultySchema,
    supportedLanguageIds: z
      .array(z.string().min(1, "Language ID cannot be blank"))
      .min(1, "At least one supported language is required"),
    statement: z.string().trim().min(1, "Problem statement cannot be empty"),
    topics: z
      .array(z.string().min(1, "Topic cannot be blank"))
      .min(1, "At least one topic is required"),
    hints: z
      .array(z.string().min(1, "Hint cannot be blank"))
      .optional()
      .default([]),
    starterCode: z.record(z.string().min(1), z.string()).optional().default({}),
    referenceSolutionLanguage: z
      .string()
      .min(1, "Reference solution language is required"),
    referenceSolution: z.string().min(1, "Reference solution cannot be empty"),
    requireVerificationScript: z.boolean().default(false),
    verificationScriptLanguage: z.string().optional().nullable(),
    verificationScript: z.string().optional().nullable(),
    sampleTestcases: z
      .array(testcaseRequestSchema)
      .min(1, "At least one sample testcase is required")
      .max(20, "Sample testcases must not exceed 20"),
    hiddenTestcases: z
      .array(testcaseRequestSchema)
      .min(1, "At least one hidden testcase is required")
      .max(20, "Hidden testcases must not exceed 20"),
    executionConstraints: z
      .record(z.string().min(1), executionConstraintSchema)
      .optional()
      .default({}),
  })
  .superRefine((data, ctx) => {
    // 1. DATABASE problems must have exactly one language
    if (data.problemType === "DATABASE") {
      if (data.supportedLanguageIds.length !== 1) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message:
            "A DATABASE problem must support exactly one database language.",
          path: ["supportedLanguageIds"],
        });
      }
    }

    // 2. Reference solution language must be in supportedLanguageIds
    if (!data.supportedLanguageIds.includes(data.referenceSolutionLanguage)) {
      ctx.addIssue({
        code: "custom",
        message: `Reference solution language '${data.referenceSolutionLanguage}' must be included in supportedLanguageIds.`,
        path: ["referenceSolutionLanguage"],
      });
    }

    // 3. Verification script validations
    if (data.requireVerificationScript) {
      if (
        !data.verificationScriptLanguage ||
        !data.verificationScriptLanguage.trim()
      ) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message:
            "Verification script language is required when verification script is enabled.",
          path: ["verificationScriptLanguage"],
        });
      }
      if (!data.verificationScript || !data.verificationScript.trim()) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message:
            "Verification script source code is required when verification script is enabled.",
          path: ["verificationScript"],
        });
      }
      if (
        data.problemType === "DATABASE" &&
        data.verificationScriptLanguage &&
        data.verificationScriptLanguage.toLowerCase() !==
          data.referenceSolutionLanguage.toLowerCase()
      ) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: `Verification script language '${data.verificationScriptLanguage}' must match reference solution language '${data.referenceSolutionLanguage}' for a DATABASE problem.`,
          path: ["verificationScriptLanguage"],
        });
      }
    }
  });

export type CreateCodingProblemRequest = z.infer<
  typeof createCodingProblemSchema
>;

/**
 * Schema for Step 1: Statement Step.
 * Clean vertical layout containing:
 * - title
 * - slug
 * - difficulty
 * - problemType (Generic vs Database)
 * - supportedLanguageIds (supported languages)
 * - statement
 * - topics
 * - hints
 */
export const statementStepSchema = z
  .object({
    title: titleSchema,
    slug: slugSchema,
    problemType: problemTypeSchema,
    difficulty: difficultySchema,
    supportedLanguageIds: z
      .array(z.string().min(1, "Language ID cannot be blank"))
      .min(1, "At least one supported language is required"),
    statement: z.string().trim().min(1, "Problem statement is required"),
    topics: z
      .array(z.string().trim().min(1, "Topic tag cannot be blank"))
      .min(1, "At least one topic tag is required"),
    hints: z.array(z.string().trim().min(1, "Hint cannot be blank")),
  })
  .superRefine((data, ctx) => {
    if (data.problemType === "DATABASE") {
      if (data.supportedLanguageIds.length !== 1) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message:
            "A database problem must support exactly one database language.",
          path: ["supportedLanguageIds"],
        });
      } else if (
        DATABASE_LANGUAGE_IDS.size > 0 &&
        !DATABASE_LANGUAGE_IDS.has(data.supportedLanguageIds[0])
      ) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: `Language '${data.supportedLanguageIds[0]}' is not a database language for a DATABASE problem.`,
          path: ["supportedLanguageIds"],
        });
      }
    } else if (data.problemType === "GENERIC") {
      const invalidDbLangs = data.supportedLanguageIds.filter((id) =>
        DATABASE_LANGUAGE_IDS.has(id)
      );
      if (invalidDbLangs.length > 0) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: `Database language '${invalidDbLangs[0]}' cannot be used for a GENERIC problem.`,
          path: ["supportedLanguageIds"],
        });
      }
    }
  });

export type StatementStepValues = z.infer<typeof statementStepSchema>;

/**
 * Default values for initializing Step 1: Statement Step.
 */
export const defaultStatementStepValues: StatementStepValues = {
  title: "",
  slug: "",
  problemType: "GENERIC",
  difficulty: "EASY",
  supportedLanguageIds: ["javascript-node-20", "python-3.12", "java-21"],
  statement: "",
  topics: [],
  hints: [],
};

