import { createFileRoute } from "@tanstack/react-router"
import { AssessmentStudio } from "@/components/studio/assessment/assessment-studio"

export const Route = createFileRoute("/admin/assessments/studio/new")({
  component: AssessmentNewPage,
})

function AssessmentNewPage() {
  return <AssessmentStudio />
}

