import { createFileRoute } from "@tanstack/react-router"
import { QuestionBankStudio } from "@/components/studio/question-bank/question-bank-studio"

export const Route = createFileRoute("/admin/questions/studio/$bankId")({
  component: QuestionBankEditPage,
})

function QuestionBankEditPage() {
  const { bankId } = Route.useParams()
  return <QuestionBankStudio mode="edit" bankId={bankId} />
}

