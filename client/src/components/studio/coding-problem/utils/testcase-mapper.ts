import type { ProblemParameter, TestCase } from "../types"

/**
 * Maps the raw input text (where each parameter occupies exactly one line in order)
 * into a structured dictionary of parameter names to string values.
 *
 * Example:
 * Line 1: [2,7,11,15]  -> parameters[0].name
 * Line 2: 9            -> parameters[1].name
 */
export function mapSourceToUi(
  source: string,
  parameters: ProblemParameter[]
): { inputs: Record<string, string>; expectedOutput?: string } {
  const inputs: Record<string, string> = {}

  if (!source || !source.trim()) {
    parameters.forEach((p) => {
      inputs[p.name] = ""
    })
    return { inputs }
  }

  const trimmed = source.trim()

  // 1. JSON parsing fallback
  if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
    try {
      const parsed = JSON.parse(trimmed)
      if (parsed && typeof parsed === "object") {
        const rawInputs = parsed.inputs ?? parsed
        parameters.forEach((p) => {
          const val = rawInputs[p.name]
          inputs[p.name] =
            val !== undefined
              ? typeof val === "object"
                ? JSON.stringify(val)
                : String(val)
              : ""
        })

        const expectedOutput =
          parsed.expectedOutput !== undefined
            ? typeof parsed.expectedOutput === "object"
              ? JSON.stringify(parsed.expectedOutput)
              : String(parsed.expectedOutput)
            : parsed.output !== undefined
              ? typeof parsed.output === "object"
                ? JSON.stringify(parsed.output)
                : String(parsed.output)
              : undefined

        return { inputs, expectedOutput }
      }
    } catch {
      // Fall through to line-based parsing
    }
  }

  // 2. Line-based parsing: Each line corresponds to one parameter in order
  const rawLines = trimmed
    .split("\n")
    .map((l) => l.trim())
    .filter((l) => l.length > 0 && !l.startsWith("//") && !l.startsWith("#"))

  let expectedOutput: string | undefined = undefined

  // Clean lines: strip legacy "paramName =" if present
  const cleanedLines: string[] = rawLines
    .map((line) => {
      // If line has output = ..., extract expectedOutput
      const lower = line.toLowerCase()
      if (lower.startsWith("output =") || lower.startsWith("output:")) {
        expectedOutput = line
          .slice(
            line.indexOf("=") !== -1
              ? line.indexOf("=") + 1
              : line.indexOf(":") + 1
          )
          .trim()
        return ""
      }

      const eqIdx = line.indexOf("=")
      if (eqIdx !== -1) {
        const prefix = line.slice(0, eqIdx).trim()
        // Check if prefix matches a parameter name
        if (parameters.some((p) => p.name === prefix)) {
          return line.slice(eqIdx + 1).trim()
        }
      }
      return line
    })
    .filter(Boolean)

  parameters.forEach((param, idx) => {
    // Flatten any unintentional inner newlines to enforce exactly one line per parameter
    const val = cleanedLines[idx] ?? ""
    inputs[param.name] = val.replace(/\r?\n+/g, " ").trim()
  })

  // If there's an extra line after all parameters and no explicit output was found, it's expectedOutput
  if (expectedOutput === undefined && cleanedLines.length > parameters.length) {
    expectedOutput = cleanedLines[parameters.length]
  }

  return { inputs, expectedOutput }
}

/**
 * Maps structured parameter values back to the canonical single-line-per-parameter format.
 * No parameter names or prefixes are added.
 *
 * Example:
 * [2,7,11,15]
 * 9
 */
export function mapUiToSource(
  inputs: Record<string, string>,
  parameters: ProblemParameter[]
): string {
  const lines: string[] = []

  parameters.forEach((param) => {
    // Ensure the parameter value is strictly on a single line
    const val = (inputs[param.name] ?? "").replace(/\r?\n+/g, " ").trim()
    lines.push(val)
  })

  return lines.join("\n")
}

/**
 * Formats a TestCase into the canonical single-line-per-parameter string representation.
 */
export function formatTestCaseSource(
  testCase: TestCase,
  parameters: ProblemParameter[]
): string {
  return mapUiToSource(testCase.inputs, parameters)
}
