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

export interface CreatedCodingProblemResponse {
  id: string;
  title: string;
  slug: string;
  status: string;
}

export const codingProblemService = {
  /**
   * Fetch all supported languages, optionally filtered by problem type (GENERIC or DATABASE).
   */
  async getSupportedLanguages(
    problemType?: "GENERIC" | "DATABASE"
  ): Promise<SupportedLanguage[]> {
    const url = problemType
      ? `/coding-problem/supported-languages?problemType=${problemType}`
      : "/coding-problem/supported-languages";
    const res = await API.get<ApiResponse<SupportedLanguage[]>>(url);
    return res.data.data;
  },

  /**
   * Check if a problem slug is available.
   */
  async checkSlug(slug: string): Promise<boolean> {
    const res = await API.get<ApiResponse<SlugAvailabilityResponse>>(
      `/coding-problem/check-slug?slug=${encodeURIComponent(slug)}`
    );
    return Boolean(res.data.data?.available);
  },

  /**
   * Create a new coding problem.
   */
  async createCodingProblem(
    payload: unknown
  ): Promise<CreatedCodingProblemResponse> {
    const res = await API.post<ApiResponse<CreatedCodingProblemResponse>>(
      "/coding-problem",
      payload
    );
    return res.data.data;
  },
};

export function buildCreateCodingProblemRequest(values: unknown) {
  return values;
}

