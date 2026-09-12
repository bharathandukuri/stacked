import CodingProblemStudio from "@/components/studio/coding-problem/studio"
import { createFileRoute } from "@tanstack/react-router"

export const Route = createFileRoute("/admin/coding-problem/studio/new")({
  component: CodingProblemNewPage,
})

function CodingProblemNewPage() {
  return <CodingProblemStudio />
}
