import API from "@/lib/api-client"
import type { ApiResponse } from "@/types/api"
import type { FileMetadata } from "@/types/file"

export const fileService = {
  /**
   * Upload a file to temporary storage.
   */
  async uploadTemporary(file: File): Promise<FileMetadata> {
    const formData = new FormData()
    formData.append("file", file)

    const response = await API.post<ApiResponse<FileMetadata>>(
      "/files/temporary",
      formData,
      {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      }
    )
    return response.data.data
  },

  /**
   * Upload a file directly to permanent storage.
   */
  async uploadPermanent(file: File): Promise<FileMetadata> {
    const formData = new FormData()
    formData.append("file", file)

    const response = await API.post<ApiResponse<FileMetadata>>(
      "/files/permanent",
      formData,
      {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      }
    )
    return response.data.data
  },

  /**
   * Move a temporary file to permanent storage.
   */
  async makePermanent(fileId: string): Promise<FileMetadata> {
    const response = await API.post<ApiResponse<FileMetadata>>(
      `/files/${encodeURIComponent(fileId)}/make-permanent`
    )
    return response.data.data
  },

  /**
   * Retrieve metadata for a file.
   */
  async getMetadata(fileId: string): Promise<FileMetadata> {
    const response = await API.get<ApiResponse<FileMetadata>>(
      `/files/${encodeURIComponent(fileId)}`
    )
    return response.data.data
  },

  /**
   * Download or retrieve raw file content as a Blob.
   */
  async getFileBlob(fileId: string): Promise<Blob> {
    const response = await API.get<Blob>(
      `/files/${encodeURIComponent(fileId)}/content`,
      {
        responseType: "blob",
      }
    )
    return response.data
  },

  /**
   * Permanently delete a file. Caller must be the file owner.
   */
  async deleteFile(fileId: string): Promise<void> {
    await API.delete<ApiResponse<void>>(`/files/${encodeURIComponent(fileId)}`)
  },
}
