/**
 * Standard testcase delimiter contract agreed across frontend and backend.
 */
export const DEFAULT_TESTCASE_DELIMITER = "---STACKED_TESTCASE_DELIMITER---";

export interface ParseTestcasesResult {
  success: boolean;
  testcases: string[];
  error?: string;
  totalParsed: number;
}

/**
 * Parses generator standard output into discrete testcase inputs based on a delimiter.
 *
 * Requirements:
 * - Preserves the exact content of each testcase, including meaningful whitespace, indentation, and newlines.
 * - Does NOT call trim() or strip() on testcase contents.
 * - Handles trailing delimiters gracefully without creating empty phantom testcases.
 * - Detects missing delimiter, empty testcase segments, and malformed outputs.
 */
export function parseGeneratedTestcases(
  stdout: string | null | undefined,
  delimiter: string = DEFAULT_TESTCASE_DELIMITER
): ParseTestcasesResult {
  if (stdout === null || stdout === undefined || stdout.length === 0) {
    return {
      success: false,
      testcases: [],
      error:
        "Generator standard output is empty. Ensure your script prints output.",
      totalParsed: 0,
    };
  }

  const activeDelimiter = delimiter?.trim()
    ? delimiter
    : DEFAULT_TESTCASE_DELIMITER;

  // Validate that the delimiter is present in stdout
  if (!stdout.includes(activeDelimiter)) {
    return {
      success: false,
      testcases: [],
      error: `Delimiter "${activeDelimiter}" was not found in generator output. Ensure your generator prints the delimiter between testcases.`,
      totalParsed: 0,
    };
  }

  // Split on delimiter boundary.
  // When a script prints a delimiter on its own line (e.g. `tc1\n---DELIMITER---\ntc2\n`),
  // we match `(?:\r?\n)?` before and after the delimiter so that the newline separating
  // the delimiter line from the testcase is consumed without altering inner content.
  const escapedDelimiter = activeDelimiter.replace(
    /[.*+?^${}()|[\]\\]/g,
    "\\$&"
  );
  const regex = new RegExp(`(?:\\r?\\n)?${escapedDelimiter}(?:\\r?\\n)?`, "g");

  const rawSegments = stdout.split(regex);

  // If there was a trailing delimiter at the very end of stdout (e.g. `tc1\n---DELIMITER---\n`),
  // rawSegments will have a trailing empty string (""). Discard only this trailing empty slice.
  if (rawSegments.length > 0 && rawSegments[rawSegments.length - 1] === "") {
    rawSegments.pop();
  }

  if (rawSegments.length === 0) {
    return {
      success: false,
      testcases: [],
      error: "No testcase content found between delimiters.",
      totalParsed: 0,
    };
  }

  // Check for empty testcase segments (e.g. two consecutive delimiters)
  for (let i = 0; i < rawSegments.length; i++) {
    if (rawSegments[i].length === 0) {
      return {
        success: false,
        testcases: [],
        error: `Testcase #${i + 1} is empty (consecutive delimiters or empty segment detected).`,
        totalParsed: 0,
      };
    }
  }

  return {
    success: true,
    testcases: rawSegments,
    totalParsed: rawSegments.length,
  };
}
