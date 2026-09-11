export type FileStorageType = "TEMPORARY" | "PERMANENT"

export interface FileMetadata {
  id: string
  originalFileName: string
  contentType: string
  size: number
  storageType: FileStorageType
  ownerId: string
  createdAt: string
  expiresAt: string | null
}

export type FileResponse = FileMetadata
