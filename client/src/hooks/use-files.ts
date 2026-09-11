import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { fileService } from "@/services/file-service"
import type { FileMetadata } from "@/types/file"

export const fileKeys = {
  all: ["files"] as const,
  detail: (id: string) => ["files", id] as const,
}

/**
 * Hook to retrieve metadata for a file.
 */
export function useFileMetadata(fileId: string) {
  return useQuery({
    queryKey: fileKeys.detail(fileId),
    queryFn: () => fileService.getMetadata(fileId),
    enabled: Boolean(fileId),
    staleTime: 5 * 60 * 1000,
  })
}

/**
 * Hook to upload a file to temporary storage.
 */
export function useUploadTemporaryFile() {
  return useMutation({
    mutationFn: (file: File) => fileService.uploadTemporary(file),
  })
}

/**
 * Hook to upload a file directly to permanent storage.
 */
export function useUploadPermanentFile() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (file: File) => fileService.uploadPermanent(file),
    onSuccess: (data: FileMetadata) => {
      queryClient.setQueryData(fileKeys.detail(data.id), data)
      queryClient.invalidateQueries({ queryKey: fileKeys.all })
    },
  })
}

/**
 * Hook to promote an existing temporary file to permanent storage.
 */
export function useMakeFilePermanent() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (fileId: string) => fileService.makePermanent(fileId),
    onSuccess: (data: FileMetadata) => {
      queryClient.setQueryData(fileKeys.detail(data.id), data)
      queryClient.invalidateQueries({ queryKey: fileKeys.all })
    },
  })
}

/**
 * Hook to delete a file. Ownership is enforced on the server.
 */
export function useDeleteFile() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (fileId: string) => fileService.deleteFile(fileId),
    onSuccess: (_, fileId: string) => {
      queryClient.removeQueries({ queryKey: fileKeys.detail(fileId) })
      queryClient.invalidateQueries({ queryKey: fileKeys.all })
    },
  })
}
