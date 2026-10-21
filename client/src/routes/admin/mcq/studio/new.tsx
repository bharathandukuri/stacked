import { createFileRoute } from "@tanstack/react-router"
import McqStudio from "@/components/studio/mcq/mcq-studio"

export const Route = createFileRoute("/admin/mcq/studio/new")({
  component: McqNewPage,
})

function McqNewPage() {
  return <McqStudio mode="create" />
}

