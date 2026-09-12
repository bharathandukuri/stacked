import { SampleTestcasesSection } from "../components/sample-testcases-section"
import type { ProblemParameter, TestCase } from "../types"

export interface SampleTestcasesProps {
  parameters?: ProblemParameter[]
  testCases?: TestCase[]
  onTestCasesChange?: (testCases: TestCase[]) => void
  className?: string
  readOnly?: boolean
}

export function SampleTestcases({
  parameters = [],
  testCases = [],
  onTestCasesChange,
  className,
  readOnly,
}: SampleTestcasesProps) {
  return (
    <SampleTestcasesSection
      problemType="GENERIC"
      parameters={parameters}
      testCases={testCases}
      onChange={onTestCasesChange || (() => {})}
      className={className}
      readOnly={readOnly}
    />
  )
}

export default SampleTestcases
