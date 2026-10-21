import { createFileRoute } from "@tanstack/react-router"
import { QuestionBankStudio } from "@/components/studio/question-bank/question-bank-studio"

export const Route = createFileRoute("/admin/questions/studio/new")({
  component: QuestionBankNewPage,
})

function QuestionBankNewPage() {
  return <QuestionBankStudio mode="create" />
}

