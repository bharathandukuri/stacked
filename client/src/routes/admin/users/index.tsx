import { useEffect, useState, useDeferredValue } from "react"
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import {
  Users,
  Plus,
  Search,
  RotateCcw,
  AlertCircle,
  Loader2,
  MoreVertical,
  Pencil,
  Trash2,
  ShieldCheck,
  User as UserIcon,
  Eye,
  EyeOff,
  UserPlus,
} from "lucide-react"

import { AdminSidebar } from "@/components/admin/sidebar"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select"
import { toast } from "@/components/ui/toast"
import {
  useAdmin,
  useIsAuthenticated,
  useIsInitialized,
} from "@/stores/auth-store"
import { initializeAuthSession } from "@/hooks/use-auth"
import {
  useUsers,
  useCreateUser,
  useUpdateUser,
  useDeleteUser,
} from "@/hooks/use-users"
import type { Role, UserItem } from "@/types/auth"

export const Route = createFileRoute("/admin/users/")({
  component: AdminUsersPage,
})

function AdminUsersPage() {
  const navigate = useNavigate()
  const currentAdmin = useAdmin()
  const isAuthenticated = useIsAuthenticated()
  const isInitialized = useIsInitialized()

  // Ensure session initialization
  useEffect(() => {
    if (!isInitialized) {
      initializeAuthSession()
    }
  }, [isInitialized])

  // Redirect to login if unauthenticated
  useEffect(() => {
    if (isInitialized && !isAuthenticated) {
      navigate({ to: "/admin/login" })
    }
  }, [isInitialized, isAuthenticated, navigate])

  // Filters & Pagination State
  const [searchInput, setSearchInput] = useState("")
  const deferredSearch = useDeferredValue(searchInput)
  const [roleFilter, setRoleFilter] = useState<Role | "">("")
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)

  const handleSearchChange = (val: string) => {
    setSearchInput(val)
    setPage(0)
  }

  const handleRoleFilterChange = (role: Role | "") => {
    setRoleFilter(role)
    setPage(0)
  }

  // Query Hook
  const { data: usersData, isLoading, isFetching, isError, error } = useUsers({
    search: deferredSearch,
    role: roleFilter,
    page,
    size,
    sortBy: "createdAt",
    sortDirection: "DESC",
  })

  // Mutations
  const createMutation = useCreateUser()
  const updateMutation = useUpdateUser()
  const deleteMutation = useDeleteUser()

  // Dialog States
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const [userToEdit, setUserToEdit] = useState<UserItem | null>(null)
  const [userToDelete, setUserToDelete] = useState<UserItem | null>(null)

  // Create Form State
  const [createName, setCreateName] = useState("")
  const [createEmail, setCreateEmail] = useState("")
  const [createPassword, setCreatePassword] = useState("")
  const [createRole, setCreateRole] = useState<Role>("USER")
  const [showCreatePassword, setShowCreatePassword] = useState(false)

  // Edit Form State
  const [editName, setEditName] = useState("")
  const [editEmail, setEditEmail] = useState("")
  const [editRole, setEditRole] = useState<Role>("USER")
  const [editPassword, setEditPassword] = useState("")
  const [showEditPassword, setShowEditPassword] = useState(false)

  // Reset Create Form
  const resetCreateForm = () => {
    setCreateName("")
    setCreateEmail("")
    setCreatePassword("")
    setCreateRole("USER")
    setShowCreatePassword(false)
  }

  // Open Edit Dialog
  const handleOpenEdit = (user: UserItem) => {
    setUserToEdit(user)
    setEditName(user.name)
    setEditEmail(user.email)
    setEditRole(user.role)
    setEditPassword("")
    setShowEditPassword(false)
  }

  // Handle Create Submit
  const handleCreateSubmit = (e: React.FormEvent) => {
    e.preventDefault()

    if (!createName.trim()) {
      toast.add({ title: "Validation Error", description: "Name is required.", type: "error" })
      return
    }
    if (!createEmail.trim() || !createEmail.includes("@")) {
      toast.add({ title: "Validation Error", description: "Valid email address is required.", type: "error" })
      return
    }
    if (!createPassword || createPassword.length < 6) {
      toast.add({ title: "Validation Error", description: "Password must be at least 6 characters.", type: "error" })
      return
    }

    createMutation.mutate(
      {
        name: createName.trim(),
        email: createEmail.trim().toLowerCase(),
        password: createPassword,
        role: createRole,
      },
      {
        onSuccess: () => {
          setIsCreateOpen(false)
          resetCreateForm()
        },
      }
    )
  }

  // Handle Update Submit
  const handleUpdateSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!userToEdit) return

    if (!editName.trim()) {
      toast.add({ title: "Validation Error", description: "Name is required.", type: "error" })
      return
    }
    if (!editEmail.trim() || !editEmail.includes("@")) {
      toast.add({ title: "Validation Error", description: "Valid email address is required.", type: "error" })
      return
    }

    if (editPassword && editPassword.length < 6) {
      toast.add({ title: "Validation Error", description: "New password must be at least 6 characters.", type: "error" })
      return
    }

    updateMutation.mutate(
      {
        id: userToEdit.id,
        payload: {
          name: editName.trim(),
          email: editEmail.trim().toLowerCase(),
          role: editRole,
          password: editPassword.trim() || undefined,
        },
      },
      {
        onSuccess: () => {
          setUserToEdit(null)
        },
      }
    )
  }

  // Handle Delete Confirmation
  const handleDeleteConfirm = () => {
    if (!userToDelete) return

    if (isSelfUser(userToDelete)) {
      toast.add({
        title: "Action Prohibited",
        description: "You cannot delete your own admin account.",
        type: "error",
      })
      setUserToDelete(null)
      return
    }

    deleteMutation.mutate(userToDelete.id, {
      onSuccess: () => {
        setUserToDelete(null)
      },
    })
  }

  // Helper to determine if target user is currently logged-in admin
  const isSelfUser = (user: UserItem): boolean => {
    if (!currentAdmin) return false
    return (
      user.id === currentAdmin.id ||
      user.email.toLowerCase() === currentAdmin.email.toLowerCase()
    )
  }

  const users = usersData?.items || []
  const totalElements = usersData?.totalElements || 0
  const totalPages = usersData?.totalPages || 0
  const hasActiveFilters = Boolean(searchInput || roleFilter)

  const handleResetFilters = () => {
    setSearchInput("")
    setRoleFilter("")
    setPage(0)
  }

  return (
    <div className="flex h-screen w-full overflow-hidden bg-background">
      <AdminSidebar currentPath="/admin/users" />

      <main className="flex flex-1 flex-col overflow-y-auto">
        {/* Header */}
        <header className="sticky top-0 z-10 flex h-16 items-center justify-between border-b border-border bg-card/70 px-8 backdrop-blur-md">
          <div className="flex items-center gap-3">
            <div className="flex size-9 items-center justify-center rounded-lg border border-primary/20 bg-primary/10 text-primary">
              <Users className="size-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-lg font-bold tracking-tight text-foreground">
                  User Management
                </h1>
                <Badge variant="secondary" className="px-1.5 py-0 text-xs">
                  {totalElements}
                </Badge>
              </div>
              <p className="text-xs text-muted-foreground">
                Manage administrators and user accounts with role-based access control.
              </p>
            </div>
          </div>

          <Button
            onClick={() => {
              resetCreateForm()
              setIsCreateOpen(true)
            }}
            className="gap-2 shadow-sm"
          >
            <UserPlus className="size-4" />
            <span>Add User</span>
          </Button>
        </header>

        {/* Filters Bar */}
        <div className="border-b border-border bg-muted/20 px-8 py-4">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            {/* Search Input */}
            <div className="relative flex-1 sm:max-w-md">
              <Search className="absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder="Search by name or email..."
                value={searchInput}
                onChange={(e) => handleSearchChange(e.target.value)}
                className="pl-9 text-sm"
              />
            </div>

            {/* Filter Dropdowns */}
            <div className="flex flex-wrap items-center gap-2">
              <NativeSelect
                value={roleFilter}
                onChange={(e) => handleRoleFilterChange(e.target.value as Role | "")}
                className="w-36 text-xs"
              >
                <NativeSelectOption value="">All Roles</NativeSelectOption>
                <NativeSelectOption value="ADMIN">ADMIN</NativeSelectOption>
                <NativeSelectOption value="USER">USER</NativeSelectOption>
              </NativeSelect>

              {hasActiveFilters && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={handleResetFilters}
                  className="gap-1.5 text-xs text-muted-foreground hover:text-foreground"
                >
                  <RotateCcw className="size-3.5" />
                  <span>Reset</span>
                </Button>
              )}
            </div>
          </div>
        </div>

        {/* Content Area */}
        <div className="flex-1 p-8">
          {isLoading ? (
            <div className="flex h-64 flex-col items-center justify-center gap-3 text-muted-foreground">
              <Loader2 className="size-8 animate-spin text-primary" />
              <p className="text-sm font-medium">Loading users...</p>
            </div>
          ) : isError ? (
            <div className="flex h-64 flex-col items-center justify-center gap-3 text-destructive">
              <AlertCircle className="size-8" />
              <p className="text-sm font-medium">
                Failed to load users: {error instanceof Error ? error.message : "Unknown error"}
              </p>
              <Button variant="outline" size="sm" onClick={() => window.location.reload()}>
                Retry
              </Button>
            </div>
          ) : users.length === 0 ? (
            <div className="flex h-64 flex-col items-center justify-center gap-3 rounded-xl border border-dashed border-border bg-card/40 p-8 text-center">
              <div className="flex size-12 items-center justify-center rounded-full bg-muted text-muted-foreground">
                <Users className="size-6" />
              </div>
              <h3 className="text-base font-semibold text-foreground">No users found</h3>
              <p className="max-w-sm text-xs text-muted-foreground">
                {hasActiveFilters
                  ? "No users match your active filters. Try adjusting or clearing your search."
                  : "No users have been created yet. Click 'Add User' to register a new user."}
              </p>
              {hasActiveFilters ? (
                <Button variant="outline" size="sm" onClick={handleResetFilters}>
                  Clear Filters
                </Button>
              ) : (
                <Button size="sm" onClick={() => setIsCreateOpen(true)} className="gap-2">
                  <Plus className="size-4" />
                  <span>Create First User</span>
                </Button>
              )}
            </div>
          ) : (
            <div className="rounded-xl border border-border bg-card shadow-sm">
              <Table>
                <TableHeader>
                  <TableRow className="hover:bg-transparent">
                    <TableHead className="w-[340px]">User</TableHead>
                    <TableHead className="w-[160px]">Role</TableHead>
                    <TableHead className="w-[200px]">Created</TableHead>
                    <TableHead className="w-[80px] text-right">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {users.map((user) => {
                    const isSelf = isSelfUser(user)

                    return (
                      <TableRow key={user.id} className="transition-colors">
                        {/* User Column */}
                        <TableCell>
                          <div className="flex items-center gap-3">
                            <div className="flex size-9 shrink-0 items-center justify-center rounded-full border border-border bg-muted/60 text-xs font-semibold text-foreground">
                              {user.name.slice(0, 2).toUpperCase()}
                            </div>
                            <div className="flex flex-col overflow-hidden">
                              <div className="flex items-center gap-2">
                                <span className="truncate text-sm font-semibold text-foreground">
                                  {user.name}
                                </span>
                                {isSelf && (
                                  <Badge
                                    variant="outline"
                                    className="border-primary/40 bg-primary/10 px-1 py-0 text-[10px] font-semibold text-primary"
                                  >
                                    You
                                  </Badge>
                                )}
                              </div>
                              <span className="truncate text-xs text-muted-foreground">
                                {user.email}
                              </span>
                            </div>
                          </div>
                        </TableCell>

                        {/* Role Column */}
                        <TableCell>
                          {user.role === "ADMIN" ? (
                            <Badge
                              variant="secondary"
                              className="inline-flex items-center gap-1.5 border border-emerald-500/30 bg-emerald-500/10 font-semibold text-emerald-600 dark:text-emerald-400"
                            >
                              <ShieldCheck className="size-3.5" />
                              <span>ADMIN</span>
                            </Badge>
                          ) : (
                            <Badge
                              variant="outline"
                              className="inline-flex items-center gap-1.5 border-blue-500/30 bg-blue-500/10 font-semibold text-blue-600 dark:text-blue-400"
                            >
                              <UserIcon className="size-3.5" />
                              <span>USER</span>
                            </Badge>
                          )}
                        </TableCell>

                        {/* Created At */}
                        <TableCell className="text-xs text-muted-foreground">
                          {user.createdAt
                            ? new Date(user.createdAt).toLocaleDateString(undefined, {
                                year: "numeric",
                                month: "short",
                                day: "numeric",
                                hour: "2-digit",
                                minute: "2-digit",
                              })
                            : "—"}
                        </TableCell>

                        {/* Actions */}
                        <TableCell className="text-right">
                          <DropdownMenu>
                            <DropdownMenuTrigger
                              render={
                                <Button
                                  variant="ghost"
                                  size="icon"
                                  className="size-8 text-muted-foreground hover:text-foreground"
                                  aria-label="Open user actions"
                                >
                                  <MoreVertical className="size-4" />
                                </Button>
                              }
                            />
                            <DropdownMenuContent align="end" className="w-44">
                              <DropdownMenuItem
                                onClick={() => handleOpenEdit(user)}
                                className="gap-2 cursor-pointer"
                              >
                                <Pencil className="size-3.5 text-muted-foreground" />
                                <span>Edit User</span>
                              </DropdownMenuItem>

                              <DropdownMenuSeparator />

                              <DropdownMenuItem
                                onClick={() => {
                                  if (!isSelf) {
                                    setUserToDelete(user)
                                  }
                                }}
                                disabled={isSelf}
                                className={`gap-2 cursor-pointer ${
                                  isSelf
                                    ? "opacity-50 cursor-not-allowed"
                                    : "text-destructive focus:text-destructive"
                                }`}
                              >
                                <Trash2 className="size-3.5" />
                                <span>
                                  {isSelf ? "Cannot Delete Self" : "Delete User"}
                                </span>
                              </DropdownMenuItem>
                            </DropdownMenuContent>
                          </DropdownMenu>
                        </TableCell>
                      </TableRow>
                    )
                  })}
                </TableBody>
              </Table>

              {/* Pagination Footer */}
              <div className="flex flex-col items-center justify-between gap-3 border-t border-border px-6 py-4 sm:flex-row">
                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                  <span>Rows per page:</span>
                  <NativeSelect
                    value={size}
                    onChange={(e) => setSize(Number(e.target.value))}
                    className="h-8 w-18 text-xs"
                  >
                    <NativeSelectOption value="10">10</NativeSelectOption>
                    <NativeSelectOption value="20">20</NativeSelectOption>
                    <NativeSelectOption value="50">50</NativeSelectOption>
                  </NativeSelect>
                  <span className="ml-2">
                    Showing {page * size + 1}–
                    {Math.min((page + 1) * size, totalElements)} of {totalElements}
                  </span>
                </div>

                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page === 0 || isFetching}
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    className="h-8 text-xs"
                  >
                    Previous
                  </Button>
                  <span className="px-2 text-xs font-medium text-muted-foreground">
                    Page {page + 1} of {Math.max(1, totalPages)}
                  </span>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page >= totalPages - 1 || isFetching}
                    onClick={() => setPage((p) => p + 1)}
                    className="h-8 text-xs"
                  >
                    Next
                  </Button>
                </div>
              </div>
            </div>
          )}
        </div>
      </main>

      {/* =========================================================================
          Create User Dialog
      ========================================================================== */}
      <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
        <DialogContent className="sm:max-w-[460px]">
          <form onSubmit={handleCreateSubmit}>
            <DialogHeader>
              <DialogTitle className="flex items-center gap-2">
                <UserPlus className="size-5 text-primary" />
                <span>Add New User</span>
              </DialogTitle>
              <DialogDescription>
                Provide the full name, email, password, and system role for the new user.
              </DialogDescription>
            </DialogHeader>

            <div className="grid gap-4 py-4">
              <div className="grid gap-1.5">
                <Label htmlFor="create-name" className="text-xs font-semibold">
                  Full Name <span className="text-destructive">*</span>
                </Label>
                <Input
                  id="create-name"
                  placeholder="e.g. Jane Doe"
                  value={createName}
                  onChange={(e) => setCreateName(e.target.value)}
                  required
                />
              </div>

              <div className="grid gap-1.5">
                <Label htmlFor="create-email" className="text-xs font-semibold">
                  Email Address <span className="text-destructive">*</span>
                </Label>
                <Input
                  id="create-email"
                  type="email"
                  placeholder="e.g. jane@example.com"
                  value={createEmail}
                  onChange={(e) => setCreateEmail(e.target.value)}
                  required
                />
              </div>

              <div className="grid gap-1.5">
                <Label htmlFor="create-password" className="text-xs font-semibold">
                  Password <span className="text-destructive">*</span>
                </Label>
                <div className="relative">
                  <Input
                    id="create-password"
                    type={showCreatePassword ? "text" : "password"}
                    placeholder="Minimum 6 characters"
                    value={createPassword}
                    onChange={(e) => setCreatePassword(e.target.value)}
                    required
                    className="pr-10"
                  />
                  <button
                    type="button"
                    onClick={() => setShowCreatePassword(!showCreatePassword)}
                    className="absolute top-1/2 right-3 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                    tabIndex={-1}
                  >
                    {showCreatePassword ? (
                      <EyeOff className="size-4" />
                    ) : (
                      <Eye className="size-4" />
                    )}
                  </button>
                </div>
              </div>

              <div className="grid gap-1.5">
                <Label htmlFor="create-role" className="text-xs font-semibold">
                  Role <span className="text-destructive">*</span>
                </Label>
                <NativeSelect
                  id="create-role"
                  value={createRole}
                  onChange={(e) => setCreateRole(e.target.value as Role)}
                  className="w-full text-sm"
                >
                  <NativeSelectOption value="USER">
                    USER (Candidate & Assessment Access)
                  </NativeSelectOption>
                  <NativeSelectOption value="ADMIN">
                    ADMIN (Full Administrative Management)
                  </NativeSelectOption>
                </NativeSelect>
              </div>
            </div>

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => setIsCreateOpen(false)}
                disabled={createMutation.isPending}
              >
                Cancel
              </Button>
              <Button type="submit" disabled={createMutation.isPending} className="gap-2">
                {createMutation.isPending && (
                  <Loader2 className="size-4 animate-spin" />
                )}
                <span>Create User</span>
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* =========================================================================
          Edit User Dialog
      ========================================================================== */}
      <Dialog open={Boolean(userToEdit)} onOpenChange={(open) => !open && setUserToEdit(null)}>
        <DialogContent className="sm:max-w-[460px]">
          <form onSubmit={handleUpdateSubmit}>
            <DialogHeader>
              <DialogTitle className="flex items-center gap-2">
                <Pencil className="size-5 text-primary" />
                <span>Edit User</span>
              </DialogTitle>
              <DialogDescription>
                Update user name, email, role, or reset their password.
              </DialogDescription>
            </DialogHeader>

            <div className="grid gap-4 py-4">
              <div className="grid gap-1.5">
                <Label htmlFor="edit-name" className="text-xs font-semibold">
                  Full Name <span className="text-destructive">*</span>
                </Label>
                <Input
                  id="edit-name"
                  value={editName}
                  onChange={(e) => setEditName(e.target.value)}
                  required
                />
              </div>

              <div className="grid gap-1.5">
                <Label htmlFor="edit-email" className="text-xs font-semibold">
                  Email Address <span className="text-destructive">*</span>
                </Label>
                <Input
                  id="edit-email"
                  type="email"
                  value={editEmail}
                  onChange={(e) => setEditEmail(e.target.value)}
                  required
                />
              </div>

              <div className="grid gap-1.5">
                <Label htmlFor="edit-role" className="text-xs font-semibold">
                  Role <span className="text-destructive">*</span>
                </Label>
                <NativeSelect
                  id="edit-role"
                  value={editRole}
                  onChange={(e) => setEditRole(e.target.value as Role)}
                  disabled={userToEdit ? isSelfUser(userToEdit) : false}
                  className="w-full text-sm"
                >
                  <NativeSelectOption value="USER">
                    USER (Candidate & Assessment Access)
                  </NativeSelectOption>
                  <NativeSelectOption value="ADMIN">
                    ADMIN (Full Administrative Management)
                  </NativeSelectOption>
                </NativeSelect>
                {userToEdit && isSelfUser(userToEdit) && (
                  <p className="text-[11px] text-muted-foreground">
                    You cannot change your own admin role.
                  </p>
                )}
              </div>

              <div className="grid gap-1.5">
                <Label htmlFor="edit-password" className="text-xs font-semibold">
                  New Password <span className="text-xs font-normal text-muted-foreground">(Optional)</span>
                </Label>
                <div className="relative">
                  <Input
                    id="edit-password"
                    type={showEditPassword ? "text" : "password"}
                    placeholder="Leave blank to keep current password"
                    value={editPassword}
                    onChange={(e) => setEditPassword(e.target.value)}
                    className="pr-10"
                  />
                  <button
                    type="button"
                    onClick={() => setShowEditPassword(!showEditPassword)}
                    className="absolute top-1/2 right-3 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                    tabIndex={-1}
                  >
                    {showEditPassword ? (
                      <EyeOff className="size-4" />
                    ) : (
                      <Eye className="size-4" />
                    )}
                  </button>
                </div>
              </div>
            </div>

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => setUserToEdit(null)}
                disabled={updateMutation.isPending}
              >
                Cancel
              </Button>
              <Button type="submit" disabled={updateMutation.isPending} className="gap-2">
                {updateMutation.isPending && (
                  <Loader2 className="size-4 animate-spin" />
                )}
                <span>Save Changes</span>
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* =========================================================================
          Delete User Confirmation Dialog
      ========================================================================== */}
      <AlertDialog
        open={Boolean(userToDelete)}
        onOpenChange={(open) => !open && setUserToDelete(null)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle className="text-destructive flex items-center gap-2">
              <Trash2 className="size-5" />
              <span>Delete User Account</span>
            </AlertDialogTitle>
            <AlertDialogDescription className="space-y-2">
              <p>
                Are you sure you want to permanently delete user{" "}
                <strong className="text-foreground">{userToDelete?.name}</strong> (
                {userToDelete?.email})?
              </p>
              <p className="text-xs text-muted-foreground">
                This action cannot be undone. All active sessions and data associated with this user
                will be permanently removed.
              </p>
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={deleteMutation.isPending}>
              Cancel
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={(e) => {
                e.preventDefault()
                handleDeleteConfirm()
              }}
              disabled={deleteMutation.isPending}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90 gap-2"
            >
              {deleteMutation.isPending && (
                <Loader2 className="size-4 animate-spin" />
              )}
              <span>Delete User</span>
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
