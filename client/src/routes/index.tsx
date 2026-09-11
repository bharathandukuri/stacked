import { createFileRoute } from "@tanstack/react-router"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { CheckCircle2 } from "lucide-react"
import { Logo } from "@/components/logo"

export const Route = createFileRoute("/")({
  component: HomeComponent,
})

function HomeComponent() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center bg-background p-6 text-foreground">
      <Card className="w-full max-w-md shadow-lg">
        <CardHeader className="space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Logo size={40} />
              <CardTitle className="text-2xl font-bold tracking-tight">
                Stacked
              </CardTitle>
            </div>
            <Badge
              variant="outline"
              className="gap-1 border-emerald-500 text-emerald-500"
            >
              <CheckCircle2 className="h-3.5 w-3.5" /> Ready
            </Badge>
          </div>
          <CardDescription>
            Spring Boot 4, Redis, MongoDB, TanStack Router, TanStack Query, and
            shadcn UI.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <p className="text-sm text-muted-foreground">
            Backend infrastructure and modern React frontend initialized and
            ready for development.
          </p>
          <Button className="w-full">Get Started</Button>
        </CardContent>
      </Card>
    </main>
  )
}
