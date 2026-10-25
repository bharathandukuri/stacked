import { createFileRoute } from "@tanstack/react-router"
import { z } from "zod"
import McqStudio from "@/components/studio/mcq/mcq-studio"

const mcqNewSearchSchema = z.object({
  topic: z.string().optional(),
})

export const Route = createFileRoute("/admin/mcq/studio/new")({
  validateSearch: mcqNewSearchSchema,
  component: McqNewPage,
})

function McqNewPage() {
  const { topic } = Route.useSearch()
  return <McqStudio mode="create" initialTopic={topic} />
}
