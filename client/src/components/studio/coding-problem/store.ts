import { create } from "zustand";
import {
  type StatementStepValues,
  defaultStatementStepValues,
} from "./schemas";
import {
  LANGUAGE_REGISTRY,
  getDefaultExecutionConstraints,
  getLanguageById,
} from "@/config/languages";
import {
  codingProblemService,
  type TestcaseGeneratorRunResponse,
  type CodingProblem,
} from "@/services/coding-problem-service";
import {
  DEFAULT_TESTCASE_DELIMITER,
  parseGeneratedTestcases,
} from "@/lib/testcase-parser";
import { toast } from "@/components/ui/toast";
import axios from "axios";
import type { TestCase } from "./types";

export type EditorMode = "starter" | "reference" | "verification";

export type DatabaseValidationMode =
  "ORDER_INSENSITIVE" | "ORDER_SENSITIVE" | "CUSTOM_QUERY";

export interface SampleTestCaseItem {
  id: string;
  title: string;
  stdin: string;
  dbInit: string;
}

export interface ExecutionConstraintValues {
  timeLimitMs?: number;
  memoryLimitKb?: number;
}

export interface TestResultItem {
  testCaseId: string;
  caseIndex: number;
  status:
    | "SUCCESS"
    | "PASSED"
    | "FAILED"
    | "RUNTIME_ERROR"
    | "COMPILATION_ERROR"
    | "TIME_LIMIT_EXCEEDED"
    | "MEMORY_LIMIT_EXCEEDED";
  runtimeMs?: number;
  memoryMb?: number;
  exitCode?: number;
  input?: string;
  actualOutput?: string;
  expectedOutput?: string;
  stderr?: string;
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
  resetValues: () => void;
  setIsSaving: (isSaving: boolean) => void;

  // Editor mode & language selection
  setActiveEditorMode: (mode: EditorMode) => void;
  setActiveCodeLanguage: (langId: string) => void;

  // Starter Code
  setStarterCode: (langId: string, code: string) => void;

  // Reference Solution
  setReferenceSolutionLanguage: (langId: string) => void;
  setReferenceSolution: (code: string) => void;
  resetReferenceSolutionToStarter: (langId: string) => void;

  // Verification Script
  setRequireVerificationScript: (require: boolean) => void;
  setVerificationScriptLanguage: (langId: string) => void;
  setVerificationScript: (code: string) => void;

  // Sample Testcases
  setSampleTestcases: (testcases: SampleTestCaseItem[]) => void;
  addSampleTestcase: () => void;
  updateSampleTestcase: (
    index: number,
    partial: Partial<SampleTestCaseItem>
  ) => void;
  removeSampleTestcase: (index: number) => void;
  duplicateSampleTestcase: (index: number) => void;
  moveSampleTestcase: (fromIndex: number, direction: -1 | 1) => void;

  // Hidden Testcases
  setHiddenTestcases: (testcases: SampleTestCaseItem[]) => void;
  addHiddenTestcase: () => void;
  updateHiddenTestcase: (
    index: number,
    partial: Partial<SampleTestCaseItem>
  ) => void;
  removeHiddenTestcase: (index: number) => void;
  duplicateHiddenTestcase: (index: number) => void;
  setActiveHiddenTestcaseIndex: (index: number) => void;
  setActiveHiddenTab: (tab: "testcases" | "results") => void;
  setIsTestingHidden: (isTesting: boolean) => void;
  setHiddenTestResults: (results: TestResultItem[] | null) => void;
  clearHiddenTestResults: () => void;
  runHiddenTestcases: () => Promise<void>;
  addBatchHiddenTestcases: (
    newCases: string[],
    problemType: "GENERIC" | "DATABASE"
  ) => { added: number; warning?: string };

  // Generator Workspace State & Actions
  setGeneratorLanguage: (lang: string) => void;
  setGeneratorScript: (code: string) => void;
  setGeneratorStdin: (stdin: string) => void;
  setGeneratorDelimiter: (delim: string) => void;
  setActiveGeneratorTab: (tab: "script" | "stdin") => void;
  runGenerator: () => Promise<void>;
  clearGeneratorResult: () => void;
  addGeneratedTestcasesToHidden: () => { added: number; error?: string };

  // Execution Constraints
  setExecutionConstraint: (
    langId: string,
    constraint: ExecutionConstraintValues
  ) => void;
  resetExecutionConstraints: (langId?: string) => void;

  // Testcase & Result UI state
  setActiveTestcaseIndex: (index: number) => void;
  setActiveTestcaseTab: (tab: "testcases" | "results") => void;
  setIsTesting: (isTesting: boolean) => void;
  setTestResults: (results: TestResultItem[] | null) => void;
  setExecutionLogs: (logs: string) => void;
  clearTestResults: () => void;
  runSampleTestcases: () => Promise<void>;

  // Image tracking & problem ID
  trackUploadedImage: (fileId: string) => void;
  removeTrackedImage: (fileId: string) => void;
  setCreatedProblemId: (id: string) => void;
  setStatus: (status: string) => void;
  setUniqueSlug: (uniqueSlug: boolean | null) => void;

  // Studio mode & Hydration
  setMode: (mode: "create" | "edit", problemId?: string | null) => void;
  hydrateProblem: (problem: CodingProblem) => void;

  // Legacy compatibility
  setSolutionValidator: (langId: string, code: string) => void;
  setDatabaseSolution: (query: string) => void;
  setDatabaseValidationMode: (mode: DatabaseValidationMode) => void;
  setDatabaseCustomValidator: (query: string) => void;
}

export interface CodingProblemStoreType {
  mode: "create" | "edit";
  editingProblemId: string | null;
  currentStep: number;
  uniqueSlug: boolean | null;
  values: CodingProblemValuesType;
  setCurrentStep: (step: number) => void;
  setValues: (values: Partial<CodingProblemValuesType>) => void;
  setUniqueSlug: (uniqueSlug: boolean | null) => void;
  resetValues: () => void;

  // Step 2 & Studio Workspace state
  activeEditorMode: EditorMode;
  activeCodeLanguage: string;
  starterCodes: Record<string, string>;

  // Reference Solution (Single for generic or database)
  referenceSolutionLanguage: string;
  referenceSolution: string;

  // Verification Script (Optional toggle)
  requireVerificationScript: boolean;
  verificationScriptLanguage: string;
  verificationScript: string;

  // Sample Testcases
  sampleTestcases: SampleTestCaseItem[];

  // Hidden Testcases
  hiddenTestcases: SampleTestCaseItem[];
  activeHiddenTestcaseIndex: number;
  activeHiddenTab: "testcases" | "results";
  isTestingHidden: boolean;
  hiddenTestResults: TestResultItem[] | null;
  hiddenExecutionLogs: string;

  // Generator Workspace
  generatorLanguage: string;
  generatorScript: string;
  generatorStdin: string;
  generatorDelimiter: string;
  activeGeneratorTab: "script" | "stdin";
  isGenerating: boolean;
  generatorResult: TestcaseGeneratorRunResponse | null;
  generatorAddedBatchSignature: string | null;

  // Execution Constraints per language
  executionConstraints: Record<string, ExecutionConstraintValues>;

  // Testcase & Result tabs
  activeTestcaseIndex: number;
  activeTestcaseTab: "testcases" | "results";
  isTesting: boolean;
  testResults: TestResultItem[] | null;
  executionLogs: string;

  // Persistence status
  isSaving: boolean;
  createdProblemId: string | null;
  status: string;
  uploadedImageIds: string[];

  // Legacy fields
  referenceSolutions: Record<string, string>;
  solutionValidators: Record<string, string>;
  databaseSolution: string;
  databaseValidationMode: DatabaseValidationMode;
  databaseCustomValidator: string;

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

export const DEFAULT_SAMPLE_TESTCASES: SampleTestCaseItem[] = [
  {
    id: "case-1",
    title: "Case 1",
    stdin: "",
    dbInit: "",
  },
];

export const DEFAULT_HIDDEN_TESTCASES: SampleTestCaseItem[] = [
  {
    id: "hidden-1",
    title: "Case 1",
    stdin: "",
    dbInit: "",
  },
];

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

function getInitialDefaultConstraints(): Record<
  string,
  ExecutionConstraintValues
> {
  const map: Record<string, ExecutionConstraintValues> = {};
  for (const lang of LANGUAGE_REGISTRY) {
    map[lang.id] = getDefaultExecutionConstraints(lang.id);
  }
  return map;
}

export const useCodingProblemStore = create<CodingProblemStoreType>(
  (set, get) => {
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

        // Ensure activeCodeLanguage and referenceSolutionLanguage are valid
        let nextActiveLang = state.activeCodeLanguage;
        let nextRefLang = state.referenceSolutionLanguage;
        let nextVerLang = state.verificationScriptLanguage;

        const langs =
          nextValues.supportedLanguageIds || nextValues.languages || [];
        if (langs.length > 0) {
          if (!langs.includes(nextActiveLang)) {
            nextActiveLang = langs[0];
          }
          if (!langs.includes(nextRefLang)) {
            nextRefLang = langs[0];
          }
          if (!langs.includes(nextVerLang)) {
            nextVerLang = langs[0];
          }
        }

        return {
          values: nextValues,
          activeCodeLanguage: nextActiveLang,
          referenceSolutionLanguage: nextRefLang,
          verificationScriptLanguage: nextVerLang,
        };
      });

    const setUniqueSlug = (uniqueSlug: boolean | null) => set({ uniqueSlug });

    const resetValues = () =>
      set({
        mode: "create",
        editingProblemId: null,
        currentStep: 1,
        uniqueSlug: null,
        values: { ...DEFAULT_CODING_PROBLEM_VALUES },
        activeEditorMode: "starter",
        activeCodeLanguage: "javascript-node-20",
        starterCodes: getInitialCodeMap("starter"),
        referenceSolutionLanguage: "javascript-node-20",
        referenceSolution:
          getLanguageById("javascript-node-20")?.defaultStarterCode || "",
        requireVerificationScript: false,
        verificationScriptLanguage: "javascript-node-20",
        verificationScript:
          getLanguageById("javascript-node-20")?.defaultValidatorCode || "",
        sampleTestcases: [...DEFAULT_SAMPLE_TESTCASES],
        hiddenTestcases: [...DEFAULT_HIDDEN_TESTCASES],
        activeHiddenTestcaseIndex: 0,
        activeHiddenTab: "testcases",
        isTestingHidden: false,
        hiddenTestResults: null,
        hiddenExecutionLogs: "",
        generatorLanguage: "python-3.12",
        generatorScript: "",
        generatorStdin: "",
        generatorDelimiter: DEFAULT_TESTCASE_DELIMITER,
        activeGeneratorTab: "script",
        isGenerating: false,
        generatorResult: null,
        generatorAddedBatchSignature: null,
        executionConstraints: getInitialDefaultConstraints(),
        activeTestcaseIndex: 0,
        activeTestcaseTab: "testcases",
        isTesting: false,
        testResults: null,
        executionLogs: "",
        isSaving: false,
        createdProblemId: null,
        status: "UNPUBLISHED",
        uploadedImageIds: [],
      });

    const hydrateProblem = (problem: CodingProblem) => {
      const isDb = problem.problemType === "DATABASE";
      const supportedLangs =
        problem.supportedLanguageIds && problem.supportedLanguageIds.length > 0
          ? problem.supportedLanguageIds
          : isDb
            ? ["postgresql-16"]
            : ["javascript-node-20", "python-3.12", "java-21"];

      // Merge starterCode with default templates
      const starterCodes = {
        ...getInitialCodeMap("starter"),
        ...(problem.starterCode || {}),
      };

      // Reference solution language & solution
      const refLang =
        problem.referenceSolutionLanguage ||
        supportedLangs[0] ||
        "javascript-node-20";
      const refSolution =
        problem.referenceSolution ||
        getLanguageById(refLang)?.defaultStarterCode ||
        "";

      // Verification script
      const reqVer = Boolean(problem.requireVerificationScript);
      const verLang =
        problem.verificationScriptLanguage ||
        refLang ||
        "javascript-node-20";
      const verScript =
        problem.verificationScript ||
        getLanguageById(verLang)?.defaultValidatorCode ||
        "";

      // Sample testcases
      const sampleTestcases: SampleTestCaseItem[] =
        problem.sampleTestCases && problem.sampleTestCases.length > 0
          ? problem.sampleTestCases.map((tc, idx) => ({
              id: tc.id || `sample-${Date.now()}-${idx + 1}`,
              title: `Case ${idx + 1}`,
              stdin: tc.stdin ?? "",
              dbInit: tc.dbInit ?? "",
            }))
          : [...DEFAULT_SAMPLE_TESTCASES];

      // Hidden testcases
      const hiddenTestcases: SampleTestCaseItem[] =
        problem.hiddenTestCases && problem.hiddenTestCases.length > 0
          ? problem.hiddenTestCases.map((tc, idx) => ({
              id: tc.id || `hidden-${Date.now()}-${idx + 1}`,
              title: `Case ${idx + 1}`,
              stdin: tc.stdin ?? "",
              dbInit: tc.dbInit ?? "",
            }))
          : [...DEFAULT_HIDDEN_TESTCASES];

      // Execution constraints
      const defaultConstraints = getInitialDefaultConstraints();
      const executionConstraints: Record<string, ExecutionConstraintValues> = {
        ...defaultConstraints,
      };
      if (problem.executionConstraints) {
        for (const [langId, limits] of Object.entries(problem.executionConstraints)) {
          if (limits) {
            executionConstraints[langId] = {
              timeLimitMs: limits.timeLimitMs,
              memoryLimitKb: limits.memoryLimitKb,
            };
          }
        }
      }

      // Values
      const values: CodingProblemValuesType = {
        title: problem.title || "",
        slug: problem.slug || "",
        problemType: problem.problemType || "GENERIC",
        difficulty: problem.difficulty || "EASY",
        supportedLanguageIds: supportedLangs,
        languages: supportedLangs,
        statement: problem.statement || "",
        description: problem.statement || "",
        topics: problem.topics || [],
        hints: problem.hints || [],
        testCases: sampleTestcases.map((stc) => ({
          id: stc.id,
          title: stc.title,
          input: isDb ? stc.dbInit : stc.stdin,
        })),
      };

      set({
        mode: "edit",
        editingProblemId: problem.id,
        createdProblemId: problem.id,
        status: problem.status || "UNPUBLISHED",
        currentStep: 1,
        uniqueSlug: true,
        values,
        activeEditorMode: "starter",
        activeCodeLanguage: supportedLangs[0] || "javascript-node-20",
        starterCodes,
        referenceSolutionLanguage: refLang,
        referenceSolution: refSolution,
        requireVerificationScript: reqVer,
        verificationScriptLanguage: verLang,
        verificationScript: verScript,
        sampleTestcases,
        hiddenTestcases,
        activeHiddenTestcaseIndex: 0,
        activeHiddenTab: "testcases",
        isTestingHidden: false,
        hiddenTestResults: null,
        hiddenExecutionLogs: "",
        generatorLanguage: problem.generatorLanguage || "python-3.12",
        generatorScript: problem.generatorScript || "",
        generatorStdin: problem.generatorStdin || "",
        generatorDelimiter: problem.generatorDelimiter || DEFAULT_TESTCASE_DELIMITER,
        activeGeneratorTab: "script",
        isGenerating: false,
        generatorResult: null,
        generatorAddedBatchSignature: null,
        executionConstraints,
        activeTestcaseIndex: 0,
        activeTestcaseTab: "testcases",
        isTesting: false,
        testResults: null,
        executionLogs: "",
        isSaving: false,
        uploadedImageIds: problem.imageFileIds || [],
      });
    };

    const actions: CodingProblemStudioActions = {
      setMode: (mode, problemId = null) =>
        set({ mode, editingProblemId: problemId }),
      hydrateProblem,
      setCurrentStep,
      setValues,
      setUniqueSlug,
      resetStore: resetValues,
      resetValues,
      setIsSaving: (isSaving) => set({ isSaving }),

      // Editor Mode
      setActiveEditorMode: (activeEditorMode) => set({ activeEditorMode }),
      setActiveCodeLanguage: (activeCodeLanguage) =>
        set({ activeCodeLanguage }),

      // Starter Code
      setStarterCode: (langId, code) =>
        set((state) => ({
          starterCodes: {
            ...state.starterCodes,
            [langId]: code,
          },
        })),

      // Reference Solution
      setReferenceSolutionLanguage: (referenceSolutionLanguage) =>
        set((state) => {
          // When language changes, if referenceSolution is empty or default, switch template
          const currentCode = state.referenceSolution.trim();
          const prevDefault =
            getLanguageById(
              state.referenceSolutionLanguage
            )?.defaultStarterCode?.trim() || "";
          const shouldReplace = !currentCode || currentCode === prevDefault;
          const newTemplate = shouldReplace
            ? getLanguageById(referenceSolutionLanguage)?.defaultStarterCode ||
              ""
            : state.referenceSolution;

          return {
            referenceSolutionLanguage,
            referenceSolution: newTemplate,
          };
        }),

      setReferenceSolution: (referenceSolution) => set({ referenceSolution }),

      resetReferenceSolutionToStarter: (langId) =>
        set((state) => ({
          referenceSolution:
            state.starterCodes[langId] ||
            getLanguageById(langId)?.defaultStarterCode ||
            "",
        })),

      // Verification Script
      setRequireVerificationScript: (requireVerificationScript) =>
        set((state) => {
          const script =
            state.verificationScript.trim() ||
            getLanguageById(state.verificationScriptLanguage)
              ?.defaultValidatorCode ||
            "";
          return {
            requireVerificationScript,
            verificationScript: script,
          };
        }),

      setVerificationScriptLanguage: (verificationScriptLanguage) =>
        set((state) => {
          const currentCode = state.verificationScript.trim();
          const prevDefault =
            getLanguageById(
              state.verificationScriptLanguage
            )?.defaultValidatorCode?.trim() || "";
          const shouldReplace = !currentCode || currentCode === prevDefault;
          const newTemplate = shouldReplace
            ? getLanguageById(verificationScriptLanguage)
                ?.defaultValidatorCode || ""
            : state.verificationScript;

          return {
            verificationScriptLanguage,
            verificationScript: newTemplate,
          };
        }),

      setVerificationScript: (verificationScript) =>
        set({ verificationScript }),

      // Sample Testcases
      setSampleTestcases: (sampleTestcases) =>
        set((state) => ({
          sampleTestcases,
          values: {
            ...state.values,
            testCases: sampleTestcases.map((stc) => ({
              id: stc.id,
              title: stc.title,
              input:
                state.values.problemType === "DATABASE"
                  ? stc.dbInit
                  : stc.stdin,
            })),
          },
        })),

      addSampleTestcase: () =>
        set((state) => {
          const nextNumber = state.sampleTestcases.length + 1;
          const newCase: SampleTestCaseItem = {
            id: `case-${Date.now()}`,
            title: `Case ${nextNumber}`,
            stdin: "",
            dbInit: "",
          };
          const updated = [...state.sampleTestcases, newCase];
          return {
            sampleTestcases: updated,
            activeTestcaseIndex: updated.length - 1,
            values: {
              ...state.values,
              testCases: updated.map((stc) => ({
                id: stc.id,
                title: stc.title,
                input:
                  state.values.problemType === "DATABASE"
                    ? stc.dbInit
                    : stc.stdin,
              })),
            },
          };
        }),

      updateSampleTestcase: (index, partial) =>
        set((state) => {
          const updated = [...state.sampleTestcases];
          const existing = updated[index];
          if (!existing) return state;

          updated[index] = { ...existing, ...partial };
          return {
            sampleTestcases: updated,
            values: {
              ...state.values,
              testCases: updated.map((stc) => ({
                id: stc.id,
                title: stc.title,
                input:
                  state.values.problemType === "DATABASE"
                    ? stc.dbInit
                    : stc.stdin,
              })),
            },
          };
        }),

      removeSampleTestcase: (index) =>
        set((state) => {
          if (state.sampleTestcases.length <= 1) return state;
          const filtered = state.sampleTestcases.filter((_, i) => i !== index);
          const reindexed = filtered.map((tc, idx) => ({
            ...tc,
            title: `Case ${idx + 1}`,
          }));
          const nextIndex = Math.max(
            0,
            Math.min(state.activeTestcaseIndex, reindexed.length - 1)
          );
          return {
            sampleTestcases: reindexed,
            activeTestcaseIndex: nextIndex,
            values: {
              ...state.values,
              testCases: reindexed.map((stc) => ({
                id: stc.id,
                title: stc.title,
                input:
                  state.values.problemType === "DATABASE"
                    ? stc.dbInit
                    : stc.stdin,
              })),
            },
          };
        }),

      duplicateSampleTestcase: (index) =>
        set((state) => {
          const target = state.sampleTestcases[index];
          if (!target) return state;

          const duplicated: SampleTestCaseItem = {
            ...target,
            id: `case-${Date.now()}`,
          };
          const updated = [...state.sampleTestcases, duplicated];
          const reindexed = updated.map((tc, idx) => ({
            ...tc,
            title: `Case ${idx + 1}`,
          }));
          return {
            sampleTestcases: reindexed,
            activeTestcaseIndex: reindexed.length - 1,
            values: {
              ...state.values,
              testCases: reindexed.map((stc) => ({
                id: stc.id,
                title: stc.title,
                input:
                  state.values.problemType === "DATABASE"
                    ? stc.dbInit
                    : stc.stdin,
              })),
            },
          };
        }),

      moveSampleTestcase: (fromIndex, direction) =>
        set((state) => {
          const toIndex = fromIndex + direction;
          if (toIndex < 0 || toIndex >= state.sampleTestcases.length)
            return state;

          const updated = [...state.sampleTestcases];
          const item = updated[fromIndex];
          updated[fromIndex] = updated[toIndex];
          updated[toIndex] = item;

          const reindexed = updated.map((tc, idx) => ({
            ...tc,
            title: `Case ${idx + 1}`,
          }));
          return {
            sampleTestcases: reindexed,
            activeTestcaseIndex: toIndex,
            values: {
              ...state.values,
              testCases: reindexed.map((stc) => ({
                id: stc.id,
                title: stc.title,
                input:
                  state.values.problemType === "DATABASE"
                    ? stc.dbInit
                    : stc.stdin,
              })),
            },
          };
        }),

      // Hidden Testcases
      setHiddenTestcases: (hiddenTestcases) => set({ hiddenTestcases }),

      addHiddenTestcase: () =>
        set((state) => {
          if (state.hiddenTestcases.length >= 20) {
            toast.add({
              title: "Limit Reached",
              description: "Maximum 20 hidden testcases allowed.",
              type: "error",
            });
            return state;
          }
          const nextNumber = state.hiddenTestcases.length + 1;
          const newCase: SampleTestCaseItem = {
            id: `hidden-${Date.now()}`,
            title: `Case ${nextNumber}`,
            stdin: "",
            dbInit: "",
          };
          const updated = [...state.hiddenTestcases, newCase];
          return {
            hiddenTestcases: updated,
            activeHiddenTestcaseIndex: updated.length - 1,
          };
        }),

      updateHiddenTestcase: (index, partial) =>
        set((state) => {
          const updated = [...state.hiddenTestcases];
          const existing = updated[index];
          if (!existing) return state;
          updated[index] = { ...existing, ...partial };
          return { hiddenTestcases: updated };
        }),

      removeHiddenTestcase: (index) =>
        set((state) => {
          if (state.hiddenTestcases.length <= 1) {
            toast.add({
              title: "Cannot Delete",
              description: "At least one hidden testcase is required.",
              type: "error",
            });
            return state;
          }
          const filtered = state.hiddenTestcases.filter((_, i) => i !== index);
          const reindexed = filtered.map((tc, idx) => ({
            ...tc,
            title: `Case ${idx + 1}`,
          }));
          const nextIndex = Math.max(
            0,
            Math.min(state.activeHiddenTestcaseIndex, reindexed.length - 1)
          );
          return {
            hiddenTestcases: reindexed,
            activeHiddenTestcaseIndex: nextIndex,
          };
        }),

      duplicateHiddenTestcase: (index) =>
        set((state) => {
          if (state.hiddenTestcases.length >= 20) {
            toast.add({
              title: "Limit Reached",
              description: "Maximum 20 hidden testcases allowed.",
              type: "error",
            });
            return state;
          }
          const target = state.hiddenTestcases[index];
          if (!target) return state;
          const duplicated: SampleTestCaseItem = {
            ...target,
            id: `hidden-${Date.now()}`,
          };
          const updated = [...state.hiddenTestcases, duplicated];
          const reindexed = updated.map((tc, idx) => ({
            ...tc,
            title: `Case ${idx + 1}`,
          }));
          return {
            hiddenTestcases: reindexed,
            activeHiddenTestcaseIndex: reindexed.length - 1,
          };
        }),

      setActiveHiddenTestcaseIndex: (activeHiddenTestcaseIndex) =>
        set({ activeHiddenTestcaseIndex }),

      setActiveHiddenTab: (activeHiddenTab) => set({ activeHiddenTab }),
      setIsTestingHidden: (isTestingHidden) => set({ isTestingHidden }),
      setHiddenTestResults: (hiddenTestResults) => set({ hiddenTestResults }),
      clearHiddenTestResults: () =>
        set({ hiddenTestResults: null, hiddenExecutionLogs: "" }),

      runHiddenTestcases: async () => {
        const state = get();
        const {
          values,
          activeEditorMode,
          activeCodeLanguage,
          starterCodes,
          referenceSolution,
          referenceSolutionLanguage,
          requireVerificationScript,
          verificationScriptLanguage,
          verificationScript,
          hiddenTestcases,
          executionConstraints,
        } = state;

        set({ isTestingHidden: true });

        const isStarterMode = activeEditorMode === "starter";
        const codeLanguage = isStarterMode
          ? activeCodeLanguage
          : referenceSolutionLanguage;
        const codeToRun = isStarterMode
          ? starterCodes[activeCodeLanguage] || ""
          : referenceSolution;

        const isDb = values.problemType === "DATABASE";

        // Check if code is empty before sending
        if (!codeToRun || !codeToRun.trim()) {
          const emptyResults: TestResultItem[] = hiddenTestcases.map(
            (tc, idx) => ({
              testCaseId: tc.id,
              caseIndex: idx + 1,
              status: "COMPILATION_ERROR",
              exitCode: 1,
              input: isDb ? tc.dbInit : tc.stdin,
              actualOutput: "",
              stderr: `${isStarterMode ? "Starter code" : "Reference solution"} is empty.`,
              logs: "[Error] No executable source code provided.",
            })
          );

          set({
            isTestingHidden: false,
            hiddenTestResults: emptyResults,
            hiddenExecutionLogs:
              "[Error] Executable source code is empty. Please provide implementation before running.",
            activeHiddenTab: "results",
          });
          return;
        }

        // Format execution limits for the payload
        const limitsPayload: Record<
          string,
          { timeLimitMs?: number; memoryLimitKb?: number }
        > = {};
        for (const [langId, limits] of Object.entries(executionConstraints)) {
          if (limits.timeLimitMs || limits.memoryLimitKb) {
            limitsPayload[langId] = {
              timeLimitMs: limits.timeLimitMs
                ? Number(limits.timeLimitMs)
                : undefined,
              memoryLimitKb: limits.memoryLimitKb
                ? Number(limits.memoryLimitKb)
                : undefined,
            };
          }
        }

        const payload = {
          problemType: values.problemType,
          referenceSolutionLanguage: codeLanguage,
          referenceSolution: codeToRun.trim(),
          requireVerificationScript: isStarterMode
            ? false
            : requireVerificationScript,
          verificationScriptLanguage: isStarterMode
            ? undefined
            : verificationScriptLanguage,
          verificationScript: isStarterMode
            ? undefined
            : verificationScript?.trim(),
          testcases: hiddenTestcases.map((tc) => ({
            id: tc.id,
            stdin: isDb ? null : (tc.stdin ?? ""),
            dbInit: isDb ? (tc.dbInit ?? "") : null,
          })),
          executionConstraints: limitsPayload,
        };

        try {
          const previewResult = await codingProblemService.runPreview(payload);

          const results: TestResultItem[] = (previewResult.results || []).map(
            (r, idx) => ({
              testCaseId:
                r.testCaseId || hiddenTestcases[idx]?.id || `case-${idx + 1}`,
              caseIndex: r.caseIndex ?? idx + 1,
              status: (r.status as TestResultItem["status"]) || "FAILED",
              runtimeMs: r.runtimeMs,
              memoryMb: r.memoryMb,
              exitCode:
                r.exitCode !== undefined ? Number(r.exitCode) : undefined,
              input: isDb
                ? hiddenTestcases[idx]?.dbInit
                : hiddenTestcases[idx]?.stdin,
              actualOutput: r.stdout || "",
              expectedOutput: r.expectedOutput,
              stderr: r.stderr,
              logs: r.logs ? r.logs.join("\n") : undefined,
              error: r.error,
            })
          );

          set({
            isTestingHidden: false,
            hiddenTestResults: results,
            hiddenExecutionLogs: previewResult.logs || "",
            activeHiddenTab: "results",
          });
        } catch (err: unknown) {
          let errorMsg = "Failed to execute hidden testcases in sandbox.";
          if (axios.isAxiosError(err) && err.response?.data?.message) {
            errorMsg = err.response.data.message;
          } else if (err instanceof Error) {
            errorMsg = err.message;
          }

          const fallbackResults: TestResultItem[] = hiddenTestcases.map(
            (tc, idx) => ({
              testCaseId: tc.id,
              caseIndex: idx + 1,
              status: "RUNTIME_ERROR",
              exitCode: 1,
              input: isDb ? tc.dbInit : tc.stdin,
              actualOutput: "",
              stderr: errorMsg,
              error: errorMsg,
            })
          );

          set({
            isTestingHidden: false,
            hiddenTestResults: fallbackResults,
            hiddenExecutionLogs: `[Fatal Sandbox Error] ${errorMsg}`,
            activeHiddenTab: "results",
          });
        }
      },

      addBatchHiddenTestcases: (newCases, problemType) => {
        const state = get();
        const current = state.hiddenTestcases;
        const isEmptyInitial =
          current.length === 1 &&
          (!current[0].stdin || current[0].stdin.trim() === "") &&
          (!current[0].dbInit || current[0].dbInit.trim() === "");

        const existingCases = isEmptyInitial ? [] : [...current];
        const availableSlots = 20 - existingCases.length;
        if (availableSlots <= 0) {
          return {
            added: 0,
            warning:
              "Hidden testcases limit reached (maximum 20 testcases allowed).",
          };
        }

        const casesToAdd = newCases.slice(0, availableSlots);
        const formattedNewCases: SampleTestCaseItem[] = casesToAdd.map(
          (content, idx) => ({
            id: `hidden-${Date.now()}-${idx + 1}`,
            title: `Case ${existingCases.length + idx + 1}`,
            stdin: problemType === "DATABASE" ? "" : content,
            dbInit: problemType === "DATABASE" ? content : "",
          })
        );

        const updated = [...existingCases, ...formattedNewCases];
        const reindexed = updated.map((tc, idx) => ({
          ...tc,
          title: `Case ${idx + 1}`,
        }));

        set({
          hiddenTestcases: reindexed,
          activeHiddenTestcaseIndex: Math.max(0, reindexed.length - 1),
        });

        const warning =
          newCases.length > availableSlots
            ? `Added ${casesToAdd.length} testcases. ${newCases.length - availableSlots} testcases omitted due to the 20-testcase limit.`
            : undefined;

        return { added: casesToAdd.length, warning };
      },

      // Generator Actions
      setGeneratorLanguage: (generatorLanguage) =>
        set({ generatorLanguage, generatorAddedBatchSignature: null }),

      setGeneratorScript: (generatorScript) =>
        set({ generatorScript, generatorAddedBatchSignature: null }),

      setGeneratorStdin: (generatorStdin) =>
        set({ generatorStdin, generatorAddedBatchSignature: null }),

      setGeneratorDelimiter: (generatorDelimiter) =>
        set({ generatorDelimiter, generatorAddedBatchSignature: null }),

      setActiveGeneratorTab: (activeGeneratorTab) =>
        set({ activeGeneratorTab }),

      clearGeneratorResult: () =>
        set({ generatorResult: null, generatorAddedBatchSignature: null }),

      runGenerator: async () => {
        const state = get();
        const {
          generatorLanguage,
          generatorScript,
          generatorStdin,
          executionConstraints,
        } = state;
        if (!generatorScript || generatorScript.trim() === "") {
          toast.add({
            title: "Missing Script",
            description: "Please enter a testcase generator script.",
            type: "error",
          });
          return;
        }

        set({
          isGenerating: true,
          generatorResult: null,
          generatorAddedBatchSignature: null,
        });

        try {
          const constraints = executionConstraints[generatorLanguage];
          const result = await codingProblemService.runGenerator({
            language: generatorLanguage,
            script: generatorScript,
            stdin: generatorStdin || "",
            constraints: constraints
              ? {
                  timeLimitMs: constraints.timeLimitMs,
                  memoryLimitKb: constraints.memoryLimitKb,
                }
              : undefined,
          });

          set({ isGenerating: false, generatorResult: result });
        } catch (err: unknown) {
          const errorMsg =
            err instanceof Error
              ? err.message
              : "Failed to execute generator script. Please check your network and script.";
          set({
            isGenerating: false,
            generatorResult: {
              status: "RUNTIME_ERROR",
              error: errorMsg,
              stderr: errorMsg,
            },
          });
          toast.add({
            title: "Generator Execution Failed",
            description: errorMsg,
            type: "error",
          });
        }
      },

      addGeneratedTestcasesToHidden: () => {
        const state = get();
        const {
          generatorResult,
          generatorDelimiter,
          generatorAddedBatchSignature,
          values,
        } = state;
        if (
          !generatorResult ||
          generatorResult.status !== "SUCCESS" ||
          !generatorResult.stdout
        ) {
          return {
            added: 0,
            error: "No successful generator output available.",
          };
        }

        const parseResult = parseGeneratedTestcases(
          generatorResult.stdout,
          generatorDelimiter
        );
        if (!parseResult.success) {
          return {
            added: 0,
            error: parseResult.error || "Failed to parse testcases.",
          };
        }

        const batchSignature = JSON.stringify(parseResult.testcases);
        if (generatorAddedBatchSignature === batchSignature) {
          return {
            added: 0,
            error:
              "This generated batch has already been added to hidden testcases.",
          };
        }

        const result = state.actions.addBatchHiddenTestcases(
          parseResult.testcases,
          values.problemType
        );

        if (result.added > 0) {
          set({ generatorAddedBatchSignature: batchSignature });
        }

        return { added: result.added, error: result.warning };
      },

      // Execution Constraints
      setExecutionConstraint: (langId, constraint) =>
        set((state) => ({
          executionConstraints: {
            ...state.executionConstraints,
            [langId]: {
              ...state.executionConstraints[langId],
              ...constraint,
            },
          },
        })),

      resetExecutionConstraints: (langId) =>
        set((state) => {
          if (langId) {
            return {
              executionConstraints: {
                ...state.executionConstraints,
                [langId]: getDefaultExecutionConstraints(langId),
              },
            };
          }
          return {
            executionConstraints: getInitialDefaultConstraints(),
          };
        }),

      // Testcase UI tabs and execution state
      setActiveTestcaseIndex: (activeTestcaseIndex) =>
        set({ activeTestcaseIndex }),
      setActiveTestcaseTab: (activeTestcaseTab) => set({ activeTestcaseTab }),
      setIsTesting: (isTesting) => set({ isTesting }),
      setTestResults: (testResults) => set({ testResults }),
      setExecutionLogs: (executionLogs) => set({ executionLogs }),
      clearTestResults: () => set({ testResults: null, executionLogs: "" }),

      runSampleTestcases: async () => {
        const state = get();
        const {
          values,
          activeEditorMode,
          activeCodeLanguage,
          starterCodes,
          referenceSolution,
          referenceSolutionLanguage,
          requireVerificationScript,
          verificationScriptLanguage,
          verificationScript,
          sampleTestcases,
          executionConstraints,
        } = state;

        set({ isTesting: true });

        const isStarterMode = activeEditorMode === "starter";
        const codeLanguage = isStarterMode
          ? activeCodeLanguage
          : referenceSolutionLanguage;
        const codeToRun = isStarterMode
          ? starterCodes[activeCodeLanguage] || ""
          : referenceSolution;

        const isDb = values.problemType === "DATABASE";

        // Check if code is empty before sending
        if (!codeToRun || !codeToRun.trim()) {
          const emptyResults: TestResultItem[] = sampleTestcases.map(
            (tc, idx) => ({
              testCaseId: tc.id,
              caseIndex: idx + 1,
              status: "COMPILATION_ERROR",
              exitCode: 1,
              input: isDb ? tc.dbInit : tc.stdin,
              actualOutput: "",
              stderr: `${isStarterMode ? "Starter code" : "Reference solution"} is empty.`,
              logs: "[Error] No executable source code provided.",
            })
          );

          set({
            isTesting: false,
            testResults: emptyResults,
            executionLogs:
              "[Error] Executable source code is empty. Please provide implementation before running.",
            activeTestcaseTab: "results",
          });
          return;
        }

        // Format execution limits for the payload
        const limitsPayload: Record<
          string,
          { timeLimitMs?: number; memoryLimitKb?: number }
        > = {};
        for (const [langId, limits] of Object.entries(executionConstraints)) {
          if (limits.timeLimitMs || limits.memoryLimitKb) {
            limitsPayload[langId] = {
              timeLimitMs: limits.timeLimitMs
                ? Number(limits.timeLimitMs)
                : undefined,
              memoryLimitKb: limits.memoryLimitKb
                ? Number(limits.memoryLimitKb)
                : undefined,
            };
          }
        }

        const payload = {
          problemType: values.problemType,
          referenceSolutionLanguage: codeLanguage,
          referenceSolution: codeToRun.trim(),
          requireVerificationScript: isStarterMode
            ? false
            : requireVerificationScript,
          verificationScriptLanguage: isStarterMode
            ? undefined
            : verificationScriptLanguage,
          verificationScript: isStarterMode
            ? undefined
            : verificationScript?.trim(),
          testcases: sampleTestcases.map((tc) => ({
            id: tc.id,
            stdin: isDb ? null : (tc.stdin ?? ""),
            dbInit: isDb ? (tc.dbInit ?? "") : null,
          })),
          executionConstraints: limitsPayload,
        };

        try {
          const previewResult = await codingProblemService.runPreview(payload);

          const results: TestResultItem[] = (previewResult.results || []).map(
            (r, idx) => ({
              testCaseId:
                r.testCaseId || sampleTestcases[idx]?.id || `case-${idx + 1}`,
              caseIndex: r.caseIndex ?? idx + 1,
              status: (r.status as TestResultItem["status"]) || "FAILED",
              runtimeMs: r.runtimeMs,
              memoryMb: r.memoryMb,
              exitCode:
                r.exitCode !== undefined ? Number(r.exitCode) : undefined,
              input: isDb
                ? sampleTestcases[idx]?.dbInit
                : sampleTestcases[idx]?.stdin,
              actualOutput: r.stdout || "",
              expectedOutput: r.expectedOutput,
              stderr: r.stderr,
              logs: r.logs ? r.logs.join("\n") : undefined,
              error: r.error,
            })
          );

          // NOTE: As requested by the user, run outputs are strictly transient for creator preview.
          // They are NOT stored in persistent problem values, because the judge automatically initializes
          // expected outputs during problem creation/submission.
          set({
            isTesting: false,
            testResults: results,
            executionLogs: previewResult.logs || "",
            activeTestcaseTab: "results",
          });
        } catch (err: unknown) {
          let errorMsg = "Failed to execute testcases in sandbox.";
          if (axios.isAxiosError(err) && err.response?.data?.message) {
            errorMsg = err.response.data.message;
          } else if (err instanceof Error) {
            errorMsg = err.message;
          }

          const fallbackResults: TestResultItem[] = sampleTestcases.map(
            (tc, idx) => ({
              testCaseId: tc.id,
              caseIndex: idx + 1,
              status: "RUNTIME_ERROR",
              input: isDb ? tc.dbInit : tc.stdin,
              actualOutput: "",
              stderr: errorMsg,
              logs: `[Error] ${errorMsg}`,
              error: errorMsg,
            })
          );

          set({
            isTesting: false,
            testResults: fallbackResults,
            executionLogs: `[Sandbox Execution Error]\n${errorMsg}`,
            activeTestcaseTab: "results",
          });
        }
      },

      // Legacy actions
      setTestCases: (cases) =>
        set((state) => ({
          values: { ...state.values, testCases: cases },
        })),
      trackUploadedImage: (fileId) =>
        set((state) => ({
          uploadedImageIds: state.uploadedImageIds.includes(fileId)
            ? state.uploadedImageIds
            : [...state.uploadedImageIds, fileId],
        })),
      removeTrackedImage: (fileId) =>
        set((state) => ({
          uploadedImageIds: state.uploadedImageIds.filter(
            (id) => id !== fileId
          ),
        })),
      setCreatedProblemId: (createdProblemId) => set({ createdProblemId }),
      setStatus: (status) => set({ status }),

      setSolutionValidator: (langId, code) =>
        set((state) => ({
          solutionValidators: {
            ...state.solutionValidators,
            [langId]: code,
          },
        })),
      setDatabaseSolution: (databaseSolution) => set({ databaseSolution }),
      setDatabaseValidationMode: (databaseValidationMode) =>
        set({ databaseValidationMode }),
      setDatabaseCustomValidator: (databaseCustomValidator) =>
        set({ databaseCustomValidator }),
    };

    return {
      mode: "create",
      editingProblemId: null,
      currentStep: 1,
      uniqueSlug: null,
      values: { ...DEFAULT_CODING_PROBLEM_VALUES },
      setCurrentStep,
      setValues,
      setUniqueSlug,
      resetValues,

      activeEditorMode: "starter",
      activeCodeLanguage: "javascript-node-20",
      starterCodes: getInitialCodeMap("starter"),

      referenceSolutionLanguage: "javascript-node-20",
      referenceSolution:
        getLanguageById("javascript-node-20")?.defaultStarterCode || "",

      requireVerificationScript: false,
      verificationScriptLanguage: "javascript-node-20",
      verificationScript:
        getLanguageById("javascript-node-20")?.defaultValidatorCode || "",

      sampleTestcases: [...DEFAULT_SAMPLE_TESTCASES],
      hiddenTestcases: [...DEFAULT_HIDDEN_TESTCASES],
      activeHiddenTestcaseIndex: 0,
      activeHiddenTab: "testcases",
      isTestingHidden: false,
      hiddenTestResults: null,
      hiddenExecutionLogs: "",

      generatorLanguage: "python-3.12",
      generatorScript: "",
      generatorStdin: "",
      generatorDelimiter: DEFAULT_TESTCASE_DELIMITER,
      activeGeneratorTab: "script",
      isGenerating: false,
      generatorResult: null,
      generatorAddedBatchSignature: null,

      executionConstraints: getInitialDefaultConstraints(),

      activeTestcaseIndex: 0,
      activeTestcaseTab: "testcases",
      isTesting: false,
      testResults: null,
      executionLogs: "",

      isSaving: false,
      createdProblemId: null,
      status: "UNPUBLISHED",
      uploadedImageIds: [],

      // Legacy fields
      referenceSolutions: getInitialCodeMap("solution"),
      solutionValidators: getInitialCodeMap("validator"),
      databaseSolution: `-- File: solution.sql\n-- Reference SQL query\nSELECT\n    *\nFROM\n    Person;\n`,
      databaseValidationMode: "ORDER_INSENSITIVE",
      databaseCustomValidator: "",

      actions,
    };
  }
);
