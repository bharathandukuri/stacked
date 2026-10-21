import { createFileRoute } from "@tanstack/react-router"
import McqStudio from "@/components/studio/mcq/mcq-studio"

export const Route = createFileRoute("/admin/mcq/studio/$questionId")({
  component: McqEditPage,
})

function McqEditPage() {
  const { questionId } = Route.useParams()
  return <McqStudio mode="edit" questionId={questionId} />
}

