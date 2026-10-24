import API from "@/lib/api-client";
import type { ApiResponse } from "@/types/api";

export interface SupportedLanguage {
  id: string;
  name: string;
  type: "GENERIC" | "DATABASE" | "COMPILED" | "INTERPRETED";
  fileExtension: string;
  defaultFileName: string;
  defaultTimeLimitMs: number;
  defaultMemoryLimitKb: number;
}

export interface SlugAvailabilityResponse {
  slug: string;
  available: boolean;
}

export interface TestCaseData {
  id?: string;
  stdin?: string | null;
  dbInit?: string | null;
  output?: string | null;
}

export interface CodingProblem {
  id: string;
  title: string;
  slug: string;
  status: "PUBLISHED" | "UNPUBLISHED" | "DRAFT";
  problemType: "GENERIC" | "DATABASE";
  difficulty: "EASY" | "MEDIUM" | "HARD";
  supportedLanguageIds: string[];
  statement: string;
  topics: string[];
  hints: string[];
  starterCode: Record<string, string>;
  referenceSolutionLanguage: string;
  referenceSolution: string;
  requireVerificationScript: boolean;
  verificationScriptLanguage?: string | null;
  verificationScript?: string | null;
  sampleTestCases: TestCaseData[];
  hiddenTestCases: TestCaseData[];
  executionConstraints?: Record<
    string,
    { timeLimitMs?: number; memoryLimitKb?: number }
  >;
  generatorLanguage?: string | null;
  generatorScript?: string | null;
  generatorStdin?: string | null;
  generatorDelimiter?: string | null;
  imageFileIds?: string[];
  createdAt?: string;
  updatedAt?: string;
}

export interface CreatedCodingProblemResponse {
  id: string;
  title: string;
  slug: string;
  status: string;
}

export interface CodingProblemSummary {
  id: string;
  title: string;
  slug: string;
  status: "PUBLISHED" | "UNPUBLISHED" | "DRAFT";
  problemType: "GENERIC" | "DATABASE";
  difficulty: "EASY" | "MEDIUM" | "HARD";
  supportedLanguageIds: string[];
  topics?: string[];
  sampleTestCaseCount: number;
  hiddenTestCaseCount: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface CodingProblemFilterParams {
  search?: string;
  status?: "PUBLISHED" | "UNPUBLISHED" | "DRAFT" | "ALL";
  problemType?: "GENERIC" | "DATABASE" | "ALL";
  difficulty?: "EASY" | "MEDIUM" | "HARD" | "ALL";
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: "ASC" | "DESC";
}

export interface PaginatedResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  isFirst: boolean;
  isLast: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface RunPreviewTestcaseItem {
  id?: string;
  stdin?: string | null;
  dbInit?: string | null;
}

export interface RunPreviewPayload {
  problemType: "GENERIC" | "DATABASE";
  referenceSolutionLanguage: string;
  referenceSolution: string;
  requireVerificationScript: boolean;
  verificationScriptLanguage?: string | null;
  verificationScript?: string | null;
  testcases: RunPreviewTestcaseItem[];
  executionConstraints?: Record<
    string,
    { timeLimitMs?: number; memoryLimitKb?: number }
  >;
}

export interface PreviewTestcaseResult {
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
  stdout?: string;
  stderr?: string;
  exitCode?: number;
  runtimeMs?: number;
  memoryMb?: number;
  expectedOutput?: string;
  logs?: string[];
  error?: string;
}

export interface PreviewRunResponse {
  overallStatus: string;
  totalExecutionTimeMs: number;
  totalTestcases: number;
  passedTestcases: number;
  failedTestcases: number;
  logs: string;
  results: PreviewTestcaseResult[];
}

export interface RunTestcaseGeneratorPayload {
  language: string;
  script: string;
  stdin?: string | null;
  constraints?: {
    timeLimitMs?: number;
    memoryLimitKb?: number;
  };
}

export interface TestcaseGeneratorRunResponse {
  status:
    | "SUCCESS"
    | "COMPILATION_ERROR"
    | "RUNTIME_ERROR"
    | "TIME_LIMIT_EXCEEDED"
    | "MEMORY_LIMIT_EXCEEDED";
  stdout?: string;
  stderr?: string;
  exitCode?: number;
  exitSignal?: number;
  executionTimeMs?: number;
  memoryUsageKb?: number;
  error?: string;
}

export const codingProblemService = {
  /**
   * @deprecated Supported language details are stored directly in frontend configuration (`@/config/languages`).
   */
  async getSupportedLanguages(
    problemType?: "GENERIC" | "DATABASE"
  ): Promise<SupportedLanguage[]> {
    const url = problemType
      ? `/admin/coding-problem/supported-languages?problemType=${problemType}`
      : "/admin/coding-problem/supported-languages";
    const res = await API.get<ApiResponse<SupportedLanguage[]>>(url);
    return res.data.data;
  },

  /**
   * Check if a problem slug is available. Optionally exclude an existing problem ID when editing.
   */
  async checkSlug(slug: string, excludeId?: string): Promise<boolean> {
    const query = new URLSearchParams({ slug });
    if (excludeId && excludeId.trim()) {
      query.set("excludeId", excludeId.trim());
    }
    const res = await API.get<ApiResponse<SlugAvailabilityResponse>>(
      `/admin/coding-problem/check-slug?${query.toString()}`
    );
    return Boolean(res.data.data?.available);
  },

  /**
   * Check if a coding problem is currently in use.
   */
  async isInUse(idOrSlug: string): Promise<boolean> {
    const res = await API.get<ApiResponse<{ inUse: boolean }>>(
      `/admin/coding-problem/${encodeURIComponent(idOrSlug)}/in-use`
    );
    return Boolean(res.data.data?.inUse);
  },

  /**
   * Fetch complete coding problem details by ID or Slug.
   */
  async getCodingProblem(idOrSlug: string): Promise<CodingProblem> {
    const res = await API.get<ApiResponse<CodingProblem>>(
      `/admin/coding-problem/${encodeURIComponent(idOrSlug)}`
    );
    return res.data.data;
  },

  /**
   * Create a new coding problem.
   */
  async createCodingProblem(
    payload: unknown
  ): Promise<CreatedCodingProblemResponse> {
    const res = await API.post<ApiResponse<CreatedCodingProblemResponse>>(
      "/admin/coding-problem",
      payload
    );
    return res.data.data;
  },

  /**
   * Update an existing coding problem.
   */
  async updateCodingProblem(
    idOrSlug: string,
    payload: unknown
  ): Promise<CodingProblem> {
    const res = await API.put<ApiResponse<CodingProblem>>(
      `/admin/coding-problem/${encodeURIComponent(idOrSlug)}`,
      payload
    );
    return res.data.data;
  },

  /**
   * Delete an existing coding problem.
   */
  async deleteCodingProblem(idOrSlug: string): Promise<void> {
    await API.delete(`/admin/coding-problem/${encodeURIComponent(idOrSlug)}`);
  },

  /**
   * Run sample testcases in preview/sandbox execution.
   */
  async runPreview(payload: RunPreviewPayload): Promise<PreviewRunResponse> {
    const res = await API.post<ApiResponse<PreviewRunResponse>>(
      "/admin/coding-problem/run-preview",
      payload
    );
    return res.data.data;
  },

  /**
   * Execute testcase generator script.
   */
  async runGenerator(
    payload: RunTestcaseGeneratorPayload
  ): Promise<TestcaseGeneratorRunResponse> {
    const res = await API.post<ApiResponse<TestcaseGeneratorRunResponse>>(
      "/admin/coding-problem/run-generator",
      payload
    );
    return res.data.data;
  },

  /**
   * Fetch paginated list of coding problems with optional filtering.
   */
  async getCodingProblems(
    params?: CodingProblemFilterParams
  ): Promise<PaginatedResponse<CodingProblemSummary>> {
    const query = new URLSearchParams();
    if (params?.search && params.search.trim()) {
      query.set("search", params.search.trim());
    }
    if (params?.status && params.status !== "ALL") {
      query.set("status", params.status);
    }
    if (params?.problemType && params.problemType !== "ALL") {
      query.set("problemType", params.problemType);
    }
    if (params?.difficulty && params.difficulty !== "ALL") {
      query.set("difficulty", params.difficulty);
    }
    if (params?.page !== undefined) {
      query.set("page", String(params.page));
    }
    if (params?.size !== undefined) {
      query.set("size", String(params.size));
    }
    if (params?.sortBy) {
      query.set("sortBy", params.sortBy);
    }
    if (params?.sortDirection) {
      query.set("sortDirection", params.sortDirection);
    }

    const qs = query.toString();
    const url = qs ? `/admin/coding-problem?${qs}` : "/admin/coding-problem";
    const res =
      await API.get<ApiResponse<PaginatedResponse<CodingProblemSummary>>>(url);
    return res.data.data;
  },

  /**
   * Update coding problem status (PUBLISHED, UNPUBLISHED, or DRAFT).
   */
  async updateStatus(
    idOrSlug: string,
    status: "PUBLISHED" | "UNPUBLISHED" | "DRAFT"
  ): Promise<CodingProblemSummary> {
    const res = await API.patch<ApiResponse<CodingProblemSummary>>(
      `/admin/coding-problem/${encodeURIComponent(idOrSlug)}/status`,
      { status }
    );
    return res.data.data;
  },

  /**
   * Publish a problem.
   */
  async publishProblem(idOrSlug: string): Promise<CodingProblemSummary> {
    const res = await API.patch<ApiResponse<CodingProblemSummary>>(
      `/admin/coding-problem/${encodeURIComponent(idOrSlug)}/publish`
    );
    return res.data.data;
  },

  /**
   * Unpublish a problem.
   */
  async unpublishProblem(idOrSlug: string): Promise<CodingProblemSummary> {
    const res = await API.patch<ApiResponse<CodingProblemSummary>>(
      `/admin/coding-problem/${encodeURIComponent(idOrSlug)}/unpublish`
    );
    return res.data.data;
  },
};

export interface BuildProblemPayloadParams {
  values: {
    title: string;
    slug: string;
    problemType: "GENERIC" | "DATABASE";
    difficulty: "EASY" | "MEDIUM" | "HARD";
    supportedLanguageIds: string[];
    statement: string;
    topics: string[];
    hints?: string[];
  };
  starterCodes: Record<string, string>;
  referenceSolutionLanguage: string;
  referenceSolution: string;
  requireVerificationScript: boolean;
  verificationScriptLanguage?: string | null;
  verificationScript?: string | null;
  sampleTestcases: Array<{ stdin?: string | null; dbInit?: string | null }>;
  hiddenTestcases: Array<{ stdin?: string | null; dbInit?: string | null }>;
  executionConstraints?: Record<
    string,
    { timeLimitMs?: number; memoryLimitKb?: number }
  >;
  generatorLanguage?: string | null;
  generatorScript?: string | null;
  generatorStdin?: string | null;
  generatorDelimiter?: string | null;
  imageFileIds?: string[];
}

export function buildCreateCodingProblemRequest(
  params: BuildProblemPayloadParams
) {
  const isDatabase = params.values.problemType === "DATABASE";

  // Starter codes: for generic include all supported languages; for database include db language if present
  const starterCode: Record<string, string> = {};
  if (isDatabase) {
    const dbLang = params.values.supportedLanguageIds[0];
    if (dbLang && params.starterCodes[dbLang]) {
      starterCode[dbLang] = params.starterCodes[dbLang];
    }
  } else {
    for (const langId of params.values.supportedLanguageIds) {
      if (params.starterCodes[langId]) {
        starterCode[langId] = params.starterCodes[langId];
      }
    }
  }

  // Format sample testcases
  const formattedSampleTestcases = params.sampleTestcases.map((tc) => ({
    stdin: isDatabase ? null : (tc.stdin ?? ""),
    dbInit: isDatabase ? (tc.dbInit ?? "") : null,
  }));

  // Format hidden testcases (fallback to sample testcases if empty)
  const sourceHidden =
    params.hiddenTestcases && params.hiddenTestcases.length > 0
      ? params.hiddenTestcases
      : params.sampleTestcases;

  const formattedHiddenTestcases = sourceHidden.map((tc) => ({
    stdin: isDatabase ? null : (tc.stdin ?? ""),
    dbInit: isDatabase ? (tc.dbInit ?? "") : null,
  }));

  // Format execution constraints (filter only supported languages & verifier language if present)
  const executionConstraints: Record<
    string,
    { timeLimitMs?: number; memoryLimitKb?: number }
  > = {};
  if (params.executionConstraints) {
    for (const [langId, limits] of Object.entries(
      params.executionConstraints
    )) {
      const isSupported = params.values.supportedLanguageIds.includes(langId);
      const isVerLang =
        params.requireVerificationScript &&
        langId === params.verificationScriptLanguage;
      if (isSupported || isVerLang) {
        if (limits.timeLimitMs || limits.memoryLimitKb) {
          executionConstraints[langId] = {
            timeLimitMs: limits.timeLimitMs ? Number(limits.timeLimitMs) : undefined,
            memoryLimitKb: limits.memoryLimitKb
              ? Number(limits.memoryLimitKb)
              : undefined,
          };
        }
      }
    }
  }

  // Extract file IDs strictly from active markdown content (statement & hints).
  // Removed/deleted images are omitted so the backend can safely clean them up.
  const fileIdMatches = new Set<string>();
  const fileRegex = /(?:\/api\/files\/|\/files\/)([a-zA-Z0-9_-]{8,64})(?:\/content)?/g;
  if (params.values.statement) {
    let match: RegExpExecArray | null;
    while ((match = fileRegex.exec(params.values.statement)) !== null) {
      if (match[1]) fileIdMatches.add(match[1]);
    }
  }
  if (Array.isArray(params.values.hints)) {
    for (const hint of params.values.hints) {
      if (hint) {
        let match: RegExpExecArray | null;
        while ((match = fileRegex.exec(hint)) !== null) {
          if (match[1]) fileIdMatches.add(match[1]);
        }
      }
    }
  }

  return {
    title: params.values.title.trim(),
    slug: params.values.slug.trim(),
    problemType: params.values.problemType,
    difficulty: params.values.difficulty,
    supportedLanguageIds: params.values.supportedLanguageIds,
    statement: params.values.statement.trim(),
    topics: params.values.topics,
    hints: (params.values.hints || []).filter((h) => h.trim().length > 0),
    starterCode,
    referenceSolutionLanguage: params.referenceSolutionLanguage,
    referenceSolution: params.referenceSolution.trim(),
    requireVerificationScript: params.requireVerificationScript,
    verificationScriptLanguage: params.requireVerificationScript
      ? params.verificationScriptLanguage || null
      : null,
    verificationScript: params.requireVerificationScript
      ? params.verificationScript?.trim() || null
      : null,
    sampleTestcases: formattedSampleTestcases,
    hiddenTestcases: formattedHiddenTestcases,
    executionConstraints,
    generatorLanguage: params.generatorLanguage?.trim() || null,
    generatorScript: params.generatorScript?.trim() || null,
    generatorStdin: params.generatorStdin?.trim() || null,
    generatorDelimiter: params.generatorDelimiter?.trim() || null,
    imageFileIds: Array.from(fileIdMatches),
  };
}

export const buildUpdateCodingProblemRequest = buildCreateCodingProblemRequest;
