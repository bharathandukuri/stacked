import { create } from "zustand";
import {
  type StatementStepValues,
  defaultStatementStepValues,
} from "./schemas";
import { LANGUAGE_REGISTRY } from "@/config/languages";
import type { TestCase } from "./types";

export type DatabaseValidationMode =
  | "ORDER_INSENSITIVE"
  | "ORDER_SENSITIVE"
  | "CUSTOM_QUERY";

export interface TestResultItem {
  testCaseId: string;
  caseIndex: number;
  status: "PASSED" | "FAILED" | "RUNTIME_ERROR";
  runtimeMs?: number;
  memoryMb?: number;
  input?: string;
  actualOutput?: string;
  expectedOutput?: string;
  logs?: string;
  error?: string;
}

export interface CodingProblemValuesType extends StatementStepValues {
  languages: string[];
  description: string;
  problemType: "GENERIC" | "DATABASE";
  testCases: TestCase[];
}

export interface CodingProblemStudioActions {
  setCurrentStep: (step: number) => void;
  setValues: (partial: Partial<CodingProblemValuesType>) => void;
  setTestCases: (cases: TestCase[]) => void;
  resetStore: () => void;
  setIsSaving: (isSaving: boolean) => void;
  setActiveCodeLanguage: (langId: string) => void;
  setStarterCode: (langId: string, code: string) => void;
  setReferenceSolution: (langId: string, code: string) => void;
  setSolutionValidator: (langId: string, code: string) => void;
  resetReferenceSolutionToStarter: (langId: string) => void;
  setDatabaseSolution: (query: string) => void;
  setDatabaseValidationMode: (mode: DatabaseValidationMode) => void;
  setDatabaseCustomValidator: (query: string) => void;
  clearTestResults: () => void;
  runSampleTestcases: (langId: string) => Promise<void>;
  trackUploadedImage: (fileId: string) => void;
  removeTrackedImage: (fileId: string) => void;
  setCreatedProblemId: (id: string) => void;
  setStatus: (status: string) => void;
  setUniqueSlug: (uniqueSlug: boolean | null) => void;
}

export interface CodingProblemStoreType {
  currentStep: number;
  uniqueSlug: boolean | null;
  values: CodingProblemValuesType;
  setCurrentStep: (step: number) => void;
  setValues: (values: Partial<CodingProblemValuesType>) => void;
  setUniqueSlug: (uniqueSlug: boolean | null) => void;
  resetValues: () => void;

  // Step 2 & Studio state (preserved for multi-step studio)
  activeCodeLanguage: string;
  starterCodes: Record<string, string>;
  referenceSolutions: Record<string, string>;
  solutionValidators: Record<string, string>;
  databaseSolution: string;
  databaseValidationMode: DatabaseValidationMode;
  databaseCustomValidator: string;
  isTesting: boolean;
  testResults: TestResultItem[] | null;
  isSaving: boolean;
  uploadedImageIds: string[];
  actions: CodingProblemStudioActions;
}

export const DEFAULT_CODING_PROBLEM_VALUES: CodingProblemValuesType = {
  ...defaultStatementStepValues,
  description: defaultStatementStepValues.statement,
  languages: defaultStatementStepValues.supportedLanguageIds,
  problemType: "GENERIC",
  testCases: [
    {
      id: "case-1",
      title: "Case 1",
      input: "",
    },
  ],
};

function getInitialCodeMap(
  type: "starter" | "solution" | "validator"
): Record<string, string> {
  const map: Record<string, string> = {};
  for (const lang of LANGUAGE_REGISTRY) {
    if (type === "starter" || type === "solution") {
      map[lang.id] = lang.defaultStarterCode;
    } else {
      map[lang.id] = lang.defaultValidatorCode;
    }
  }
  return map;
}

export const useCodingProblemStore = create<CodingProblemStoreType>((set) => {
  const setCurrentStep = (step: number) => set({ currentStep: step });

  const setValues = (v: Partial<CodingProblemValuesType>) =>
    set((state) => {
      const nextValues = {
        ...state.values,
        ...v,
      };
      if (v.supportedLanguageIds && !v.languages) {
        nextValues.languages = v.supportedLanguageIds;
      }
      if (v.statement && !v.description) {
        nextValues.description = v.statement;
      }
      if (v.description && !v.statement) {
        nextValues.statement = v.description;
      }
      return { values: nextValues };
    });

  const setUniqueSlug = (uniqueSlug: boolean | null) => set({ uniqueSlug });

  const resetValues = () =>
    set({
      currentStep: 1,
      uniqueSlug: null,
      values: { ...DEFAULT_CODING_PROBLEM_VALUES },
    });

  const actions: CodingProblemStudioActions = {
    setCurrentStep,
    setValues,
    setUniqueSlug,
    setTestCases: (cases) =>
      set((state) => ({
        values: { ...state.values, testCases: cases },
      })),
    resetStore: resetValues,
    setIsSaving: (isSaving) => set({ isSaving }),
    setActiveCodeLanguage: (activeCodeLanguage) => set({ activeCodeLanguage }),
    setStarterCode: (langId, code) =>
      set((state) => ({
        starterCodes: {
          ...state.starterCodes,
          [langId]: code,
        },
      })),
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
          [langId]: state.starterCodes[langId] || "",
        },
      })),
    setDatabaseSolution: (databaseSolution) => set({ databaseSolution }),
    setDatabaseValidationMode: (databaseValidationMode) =>
      set({ databaseValidationMode }),
    setDatabaseCustomValidator: (databaseCustomValidator) =>
      set({ databaseCustomValidator }),
    clearTestResults: () => set({ testResults: null }),
    runSampleTestcases: async () => {},
    trackUploadedImage: (fileId) =>
      set((state) => ({
        uploadedImageIds: state.uploadedImageIds.includes(fileId)
          ? state.uploadedImageIds
          : [...state.uploadedImageIds, fileId],
      })),
    removeTrackedImage: (fileId) =>
      set((state) => ({
        uploadedImageIds: state.uploadedImageIds.filter((id) => id !== fileId),
      })),
    setCreatedProblemId: () => {},
    setStatus: () => {},
  };

  return {
    currentStep: 1,
    uniqueSlug: null,
    values: { ...DEFAULT_CODING_PROBLEM_VALUES },
    setCurrentStep,
    setValues,
    setUniqueSlug,
    resetValues,

    activeCodeLanguage: "javascript-node-20",
    starterCodes: getInitialCodeMap("starter"),
    referenceSolutions: getInitialCodeMap("solution"),
    solutionValidators: getInitialCodeMap("validator"),
    databaseSolution: `-- Reference SQL query\nSELECT\n    *\nFROM\n    Person;\n`,
    databaseValidationMode: "ORDER_INSENSITIVE",
    databaseCustomValidator: "",
    isTesting: false,
    testResults: null,
    isSaving: false,
    uploadedImageIds: [],
    actions,
  };
});
