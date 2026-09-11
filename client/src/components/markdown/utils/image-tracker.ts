import { fileService } from "@/services/file-service"
import type { FileResponse } from "@/types/file"
import type { UploadedImage, MarkdownEditorValue } from "../types"

// Regex to capture file IDs from /api/files/{id}/content or absolute backend URLs
const FILE_ID_REGEX = /\/api\/files\/([a-zA-Z0-9_-]{8,64})\/content/g

/**
 * Extracts all file IDs embedded in the markdown text matching the File Module API pattern.
 */
export function extractFileIdsFromMarkdown(content: string): string[] {
  if (!content) return []
  const matches = new Set<string>()
  let match: RegExpExecArray | null
  const regex = new RegExp(FILE_ID_REGEX)

  while ((match = regex.exec(content)) !== null) {
    if (match[1]) {
      matches.add(match[1])
    }
  }

  return Array.from(matches)
}

/**
 * Categorizes session images into those actively referenced in markdown text vs unused.
 */
export function syncImageReferences(
  content: string,
  images: UploadedImage[]
): {
  referencedImages: UploadedImage[]
  unusedImages: UploadedImage[]
  referencedCount: number
  unusedCount: number
} {
  const activeFileIds = new Set(extractFileIdsFromMarkdown(content))

  const referencedImages = images.filter((img) => activeFileIds.has(img.fileId))
  const unusedImages = images.filter((img) => !activeFileIds.has(img.fileId))

  return {
    referencedImages,
    unusedImages,
    referencedCount: referencedImages.length,
    unusedCount: unusedImages.length,
  }
}

/**
 * Promotes all temporary images currently referenced in markdown content to permanent storage.
 * Optionally deletes unused session images from temporary storage immediately.
 */
export async function finalizeMarkdownImages(
  editorValue: MarkdownEditorValue,
  options: { deleteUnused?: boolean } = {}
): Promise<{
  updatedValue: MarkdownEditorValue
  promotedFileIds: string[]
  deletedFileIds: string[]
}> {
  const { content, uploadedImages } = editorValue
  const { referencedImages, unusedImages } = syncImageReferences(
    content,
    uploadedImages
  )

  const promotedFileIds: string[] = []
  const deletedFileIds: string[] = []

  // 1. Promote temporary images that are referenced in content to permanent storage
  const updatedImages: UploadedImage[] = await Promise.all(
    uploadedImages.map(async (img) => {
      const isReferenced = referencedImages.some(
        (ref) => ref.fileId === img.fileId
      )

      if (isReferenced && !img.isPermanent) {
        try {
          await fileService.makePermanent(img.fileId)
          promotedFileIds.push(img.fileId)
          return { ...img, isPermanent: true }
        } catch (err) {
          console.error(
            `Failed to promote image [${img.fileId}] to permanent:`,
            err
          )
          return img
        }
      }

      return img
    })
  )

  // 2. Discover any additional file IDs in content that weren't tracked in session uploadedImages
  const allReferencedFileIds = extractFileIdsFromMarkdown(content)
  for (const fileId of allReferencedFileIds) {
    const alreadyTracked = updatedImages.some((img) => img.fileId === fileId)
    if (!alreadyTracked) {
      try {
        await fileService.makePermanent(fileId)
        promotedFileIds.push(fileId)
        updatedImages.push({
          fileId,
          url: fileService.getFileContentUrl(fileId),
          originalFileName: `image-${fileId}`,
          size: 0,
          contentType: "image/png",
          uploadedAt: new Date().toISOString(),
          isPermanent: true,
        })
      } catch (err) {
        console.warn(`Could not promote detected image [${fileId}]:`, err)
      }
    }
  }

  // 3. Optionally purge unused temporary files immediately
  if (options.deleteUnused && unusedImages.length > 0) {
    for (const unused of unusedImages) {
      if (!unused.isPermanent) {
        try {
          await fileService.deleteFile(unused.fileId)
          deletedFileIds.push(unused.fileId)
        } catch (err) {
          console.warn(
            `Failed to clean up unused temporary image [${unused.fileId}]:`,
            err
          )
        }
      }
    }
  }

  // Remove deleted files from tracked state
  const finalImages = updatedImages.filter(
    (img) => !deletedFileIds.includes(img.fileId)
  )

  return {
    updatedValue: {
      content,
      uploadedImages: finalImages,
    },
    promotedFileIds,
    deletedFileIds,
  }
}

/**
 * Maps a backend FileResponse into an UploadedImage session item.
 */
export function createUploadedImageFromFileResponse(
  fileResponse: FileResponse
): UploadedImage {
  return {
    fileId: fileResponse.id,
    url: fileService.getFileContentUrl(fileResponse.id),
    originalFileName: fileResponse.originalFileName,
    size: fileResponse.size,
    contentType: fileResponse.contentType,
    uploadedAt: fileResponse.createdAt,
    isPermanent: fileResponse.storageType === "PERMANENT",
  }
}
