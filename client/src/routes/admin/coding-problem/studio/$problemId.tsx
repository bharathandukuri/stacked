import CodingProblemStudio from "@/components/studio/coding-problem/studio"
import { createFileRoute } from "@tanstack/react-router"

export const Route = createFileRoute(
  "/admin/coding-problem/studio/$problemId"
)({
  component: CodingProblemEditPage,
})

function CodingProblemEditPage() {
  const { problemId } = Route.useParams()
  return <CodingProblemStudio mode="edit" problemId={problemId} />
}

