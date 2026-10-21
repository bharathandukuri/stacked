import { createFileRoute, redirect } from "@tanstack/react-router"
import { LoginPage } from "../login"

export const Route = createFileRoute("/admin/login")({
  beforeLoad: () => {
    throw redirect({ to: "/login" })
  },
  component: LoginPage,
})
