import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import axios from "axios"
import { userService } from "@/services/user-service"
import type {
  CreateUserPayload,
  UpdateUserPayload,
  UserFilterParams,
  UserItem,
} from "@/types/auth"
import type { PaginatedResponse } from "@/services/coding-problem-service"
import { toast } from "@/components/ui/toast"

function extractErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: string } | undefined
    if (data?.message) return data.message
  }
  if (error instanceof Error) {
    return error.message
  }
  return fallback
}

export const userKeys = {
  all: ["users"] as const,
  lists: () => [...userKeys.all, "list"] as const,
  list: (filters: UserFilterParams) =>
    [...userKeys.lists(), filters] as const,
  details: () => [...userKeys.all, "detail"] as const,
  detail: (id: string) => [...userKeys.details(), id] as const,
}

/**
 * Hook to retrieve paginated and filtered users.
 */
export function useUsers(filters: UserFilterParams = {}) {
  return useQuery<PaginatedResponse<UserItem>>({
    queryKey: userKeys.list(filters),
    queryFn: () => userService.getUsers(filters),
    placeholderData: (previousData) => previousData,
  })
}

/**
 * Hook to retrieve a single user by ID.
 */
export function useUser(id?: string) {
  return useQuery<UserItem>({
    queryKey: userKeys.detail(id || ""),
    queryFn: () => userService.getUser(id!),
    enabled: Boolean(id && id.trim().length > 0),
  })
}

/**
 * Hook to create a new user.
 */
export function useCreateUser() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: CreateUserPayload) => userService.createUser(payload),
    onSuccess: (created) => {
      toast.add({
        title: "User Created",
        description: `User "${created.name}" (${created.role}) has been created successfully.`,
        type: "success",
      })
      queryClient.invalidateQueries({ queryKey: userKeys.lists() })
    },
    onError: (error: unknown) => {
      toast.add({
        title: "Failed to Create User",
        description: extractErrorMessage(error, "Could not create user."),
        type: "error",
      })
    },
  })
}

/**
 * Hook to update an existing user.
 */
export function useUpdateUser() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UpdateUserPayload }) =>
      userService.updateUser(id, payload),
    onSuccess: (updated) => {
      toast.add({
        title: "User Updated",
        description: `User "${updated.name}" has been updated successfully.`,
        type: "success",
      })
      queryClient.invalidateQueries({ queryKey: userKeys.lists() })
      queryClient.invalidateQueries({ queryKey: userKeys.detail(updated.id) })
    },
    onError: (error: unknown) => {
      toast.add({
        title: "Failed to Update User",
        description: extractErrorMessage(error, "Could not update user."),
        type: "error",
      })
    },
  })
}

/**
 * Hook to delete an existing user.
 */
export function useDeleteUser() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => userService.deleteUser(id),
    onSuccess: () => {
      toast.add({
        title: "User Deleted",
        description: "User has been deleted successfully.",
        type: "success",
      })
      queryClient.invalidateQueries({ queryKey: userKeys.lists() })
    },
    onError: (error: unknown) => {
      toast.add({
        title: "Failed to Delete User",
        description: extractErrorMessage(error, "Could not delete user."),
        type: "error",
      })
    },
  })
}
