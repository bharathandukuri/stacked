import { useState } from "react"
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import { Lock, Mail, AlertCircle, Loader2 } from "lucide-react"

import FormText from "@/components/form/form-text"
import { Logo } from "@/components/logo"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { useAppForm } from "@/hooks/form/create-form-hooks"
import { useLogin } from "@/hooks/use-auth"
import { adminLoginSchema, emailSchema, passwordSchema } from "@/schemas/auth"
import { toast } from "@/components/ui/toast"
import type { ApiError } from "@/types/api"

export const Route = createFileRoute("/admin/login")({
  component: AdminLoginPage,
})

export function AdminLoginPage() {
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const loginMutation = useLogin()
  const navigate = useNavigate()

  const form = useAppForm({
    defaultValues: {
      email: "",
      password: "",
    },
    validators: {
      onChange: adminLoginSchema,
    },
    onSubmit: async ({ value }) => {
      setErrorMessage(null)
      try {
        const res = await loginMutation.mutateAsync({
          email: value.email,
          password: value.password,
        })
        toast.add({
          title: "Welcome Back",
          description: `Signed in as ${res?.admin?.name || "Administrator"}.`,
          type: "success",
        })
        await navigate({ to: "/admin" })
      } catch (err: unknown) {
        const apiError = err as ApiError
        const message =
          apiError?.message ||
          "Invalid credentials. Please verify and try again."
        setErrorMessage(message)
        toast.add({
          title: "Authentication Failed",
          description: message,
          type: "error",
        })
      }
    },
  })

  return (
    <main className="flex min-h-screen items-center justify-center bg-muted/30 p-4">
      <Card className="w-full max-w-md border-border/70 shadow-sm">
        <CardHeader className="space-y-1.5 pb-6 text-center">
          <div className="mx-auto mb-2 flex size-12 items-center justify-center rounded-xl border bg-card shadow-xs">
            <Logo size={28} />
          </div>
          <CardTitle className="text-2xl font-semibold tracking-tight">
            Admin Sign In
          </CardTitle>
          <CardDescription className="text-sm text-muted-foreground">
            Enter your credentials to access the portal
          </CardDescription>
        </CardHeader>

        <CardContent className="space-y-4">
          {errorMessage && (
            <Alert variant="destructive" className="py-2.5">
              <AlertCircle className="size-4" />
              <AlertDescription className="text-sm">
                {errorMessage}
              </AlertDescription>
            </Alert>
          )}

          <form
            onSubmit={(e) => {
              e.preventDefault()
              e.stopPropagation()
              form.handleSubmit()
            }}
            className="space-y-4"
          >
            <form.AppField
              name="email"
              validators={{
                onChange: emailSchema,
              }}
              children={() => (
                <FormText
                  label="Email"
                  placeholder="admin@stacked.com"
                  type="email"
                  icon={Mail}
                  required
                />
              )}
            />

            <form.AppField
              name="password"
              validators={{
                onChange: passwordSchema,
              }}
              children={() => (
                <FormText
                  label="Password"
                  placeholder="••••••••"
                  type="password"
                  icon={Lock}
                  required
                />
              )}
            />

            <form.Subscribe
              selector={(state) => [state.canSubmit, state.isSubmitting]}
              children={([canSubmit, isSubmitting]) => {
                const isLoading = isSubmitting || loginMutation.isPending

                return (
                  <Button
                    type="submit"
                    size="lg"
                    disabled={!canSubmit || isLoading}
                    className="w-full font-medium"
                  >
                    {isLoading ? (
                      <>
                        <Loader2 className="mr-2 size-4 animate-spin" />
                        Signing in...
                      </>
                    ) : (
                      "Sign In"
                    )}
                  </Button>
                )
              }}
            />
          </form>
        </CardContent>
      </Card>
    </main>
  )
}
