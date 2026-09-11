export interface MarkdownPreviewProps {
  /** Raw markdown content string to render */
  content?: string
  /** Alias for content for backwards compatibility */
  value?: string
  /** Optional container CSS class */
  className?: string
  /** Message displayed when content is empty */
  emptyMessage?: string
  /** Whether to allow safe HTML tags via rehype-raw (default: true) */
  allowHtml?: boolean
  /** Whether to show a container border (default: false) */
  bordered?: boolean
}
