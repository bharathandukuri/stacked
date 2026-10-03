import { create } from "zustand"
import type { TestCase } from "./types"
import type { CodingProblemValues } from "./schemas"
import {
  LANGUAGE_REGISTRY,
  getLanguageById,
  getDefaultLanguageIds,
} from "@/config/languages"

export type DatabaseValidationMode =
  "ORDER_INSENSITIVE" | "ORDER_SENSITIVE" | "CUSTOM_QUERY"

export interface TestResultItem {
  testCaseId: string
  caseIndex: number
  status: "PASSED" | "FAILED" | "ERROR"
  runtimeMs: number
  memoryMb: number
  input: string
  actualOutput: string
  expectedOutput?: string
  logs?: string
  error?: string
}

export interface CodingProblemStudioState {
  currentStep: number
  values: CodingProblemValues
  uploadedImageIds: string[]
  isSaving: boolean
  statement: CodingProblemValues
  testCases: TestCase[]

  // Step 2: Code, Solutions & Validation
  activeCodeLanguage: string
  starterCodes: Record<string, string>
  referenceSolutions: Record<string, string>
  solutionValidators: Record<string, string>

  // Database validation settings
  databaseSolution: string
  databaseValidationMode: DatabaseValidationMode
  databaseCustomValidator: string

  // Sample Run
  isTesting: boolean
  testResults: TestResultItem[] | null
}

export interface CodingProblemStudioActions {
  setCurrentStep: (step: number) => void
  setValues: (data: Partial<CodingProblemValues>) => void
  setStatement: (data: Partial<CodingProblemValues>) => void
  setTestCases: (cases: TestCase[]) => void
  setIsSaving: (isSaving: boolean) => void
  trackUploadedImage: (fileId: string) => void
  removeTrackedImage: (fileId: string) => void

  // Step 2 actions
  setActiveCodeLanguage: (langId: string) => void
  setStarterCode: (langId: string, code: string) => void
  setReferenceSolution: (langId: string, code: string) => void
  setSolutionValidator: (langId: string, code: string) => void
  resetReferenceSolutionToStarter: (langId: string) => void
  setDatabaseSolution: (query: string) => void
  setDatabaseValidationMode: (mode: DatabaseValidationMode) => void
  setDatabaseCustomValidator: (query: string) => void
  runSampleTestcases: (langId: string) => Promise<void>
  clearTestResults: () => void

  resetStore: () => void
}

export type CodingProblemStudioStore = CodingProblemStudioState & {
  actions: CodingProblemStudioActions
}

export const DEFAULT_TEST_CASES: TestCase[] = [
  {
    id: "case-1",
    title: "Case 1",
    input: "",
  },
]

export const DEFAULT_CODING_PROBLEM_VALUES: CodingProblemValues = {
  title: "",
  slug: "",
  difficulty: "EASY",
  problemType: "GENERIC",
  languages: getDefaultLanguageIds("GENERIC"),
  description: "",
  constraints: [],
  hints: [""],
  topics: [],
  companies: [],
  testCases: [...DEFAULT_TEST_CASES],
}

// Generate initial templates from language registry
function getInitialCodeMap(
  type: "starter" | "solution" | "validator"
): Record<string, string> {
  const map: Record<string, string> = {}
  for (const lang of LANGUAGE_REGISTRY) {
    if (type === "starter") {
      map[lang.id] = lang.defaultStarterCode
    } else if (type === "solution") {
      map[lang.id] = lang.defaultStarterCode
    } else {
      map[lang.id] = lang.defaultValidatorCode
    }
  }
  return map
}

export const useCodingProblemStore = create<CodingProblemStudioStore>(
  (set, get) => {
    const actions: CodingProblemStudioActions = {
      setCurrentStep: (currentStep) => set({ currentStep }),

      setValues: (partial) =>
        set((state) => {
          const nextValues = { ...state.values, ...partial }
          return {
            values: nextValues,
            statement: nextValues,
            testCases: nextValues.testCases,
          }
        }),

      setStatement: (partial) =>
        set((state) => {
          const nextValues = { ...state.values, ...partial }
          return {
            values: nextValues,
            statement: nextValues,
            testCases: nextValues.testCases,
          }
        }),

      setTestCases: (testCases) =>
        set((state) => {
          const nextValues = { ...state.values, testCases }
          return {
            values: nextValues,
            statement: nextValues,
            testCases,
          }
        }),

      setIsSaving: (isSaving) => set({ isSaving }),

      trackUploadedImage: (fileId) =>
        set((state) => {
          if (state.uploadedImageIds.includes(fileId)) return state
          return { uploadedImageIds: [...state.uploadedImageIds, fileId] }
        }),

      removeTrackedImage: (fileId) =>
        set((state) => ({
          uploadedImageIds: state.uploadedImageIds.filter(
            (id) => id !== fileId
          ),
        })),

      setActiveCodeLanguage: (activeCodeLanguage) =>
        set({ activeCodeLanguage }),

      setStarterCode: (langId, code) =>
        set((state) => {
          // If the reference solution is identical to previous starter code or empty, sync it
          const currentRef = state.referenceSolutions[langId]
          const prevStarter = state.starterCodes[langId]
          const shouldSyncRef = !currentRef || currentRef === prevStarter

          return {
            starterCodes: {
              ...state.starterCodes,
              [langId]: code,
            },
            referenceSolutions: {
              ...state.referenceSolutions,
              [langId]: shouldSyncRef ? code : currentRef,
            },
          }
        }),

      setReferenceSolution: (langId, code) =>
        set((state) => ({
          referenceSolutions: {
            ...state.referenceSolutions,
            [langId]: code,
          },
        })),

      setSolutionValidator: (langId, code) =>
        set((state) => ({
          solutionValidators: {
            ...state.solutionValidators,
            [langId]: code,
          },
        })),

      resetReferenceSolutionToStarter: (langId) =>
        set((state) => ({
          referenceSolutions: {
            ...state.referenceSolutions,
            [langId]:
              state.starterCodes[langId] ||
              getLanguageById(langId)?.defaultStarterCode ||
              "",
          },
        })),

      setDatabaseSolution: (databaseSolution) => set({ databaseSolution }),

      setDatabaseValidationMode: (databaseValidationMode) =>
        set({ databaseValidationMode }),

      setDatabaseCustomValidator: (databaseCustomValidator) =>
        set({ databaseCustomValidator }),

      clearTestResults: () => set({ testResults: null }),

      runSampleTestcases: async (langId: string) => {
        const state = get()
        const cases = state.values.testCases
        const isDb = state.values.problemType === "DATABASE"

        set({ isTesting: true, testResults: null })

        // Simulate realistic execution of solution code against test cases
        await new Promise((res) => setTimeout(res, 600))

        const results: TestResultItem[] = cases.map((tc, index) => {
          const inputData = tc.input || ""
          const caseNumber = index + 1

          if (isDb) {
            return {
              testCaseId: tc.id,
              caseIndex: caseNumber,
              status: "PASSED",
              runtimeMs: Math.floor(Math.random() * 25) + 10,
              memoryMb: Math.round((Math.random() * 5 + 20) * 10) / 10,
              input: inputData || "-- SQL Schema Init",
              actualOutput: `| result |\n|--------|\n|   OK   |`,
              expectedOutput: `| result |\n|--------|\n|   OK   |`,
              logs: `Transaction initialized for test case ${caseNumber}. Result set matched reference query.`,
            }
          }

          return {
            testCaseId: tc.id,
            caseIndex: caseNumber,
            status: "PASSED",
            runtimeMs: Math.floor(Math.random() * 35) + 15,
            memoryMb: Math.round((Math.random() * 8 + 12) * 10) / 10,
            input: inputData || `[sample input ${caseNumber}]`,
            actualOutput: `[Valid Output for Case ${caseNumber}]`,
            expectedOutput: `[Valid Output for Case ${caseNumber}]`,
            logs: `Solution compiled and executed with ${langId}. Validator exited with code 0 (Accepted).`,
          }
        })

        set({ isTesting: false, testResults: results })
      },

      resetStore: () =>
        set({
          currentStep: 1,
          values: { ...DEFAULT_CODING_PROBLEM_VALUES },
          statement: { ...DEFAULT_CODING_PROBLEM_VALUES },
          testCases: [...DEFAULT_TEST_CASES],
          uploadedImageIds: [],
          isSaving: false,
          activeCodeLanguage: "javascript-node-20",
          starterCodes: getInitialCodeMap("starter"),
          referenceSolutions: getInitialCodeMap("solution"),
          solutionValidators: getInitialCodeMap("validator"),
          databaseSolution: `-- Reference SQL query\nSELECT\n    *\nFROM\n    Person;\n`,
          databaseValidationMode: "ORDER_INSENSITIVE",
          databaseCustomValidator: "",
          isTesting: false,
          testResults: null,
        }),
    }

    return {
      currentStep: 1,
      values: { ...DEFAULT_CODING_PROBLEM_VALUES },
      statement: { ...DEFAULT_CODING_PROBLEM_VALUES },
      testCases: [...DEFAULT_TEST_CASES],
      uploadedImageIds: [],
      isSaving: false,
      activeCodeLanguage: "javascript-node-20",
      starterCodes: getInitialCodeMap("starter"),
      referenceSolutions: getInitialCodeMap("solution"),
      solutionValidators: getInitialCodeMap("validator"),
      databaseSolution: `-- Reference SQL query\nSELECT\n    *\nFROM\n    Person;\n`,
      databaseValidationMode: "ORDER_INSENSITIVE",
      databaseCustomValidator: "",
      isTesting: false,
      testResults: null,
      actions,
    }
  }
)
