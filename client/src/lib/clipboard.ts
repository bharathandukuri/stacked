/**
 * Copies text to the system clipboard across modern browsers,
 * secure contexts (HTTPS/localhost), and non-secure contexts (e.g. HTTP on remote server IP).
 *
 * Browsers disable `navigator.clipboard` in non-secure HTTP contexts.
 * This utility falls back to `document.execCommand('copy')` using an offscreen textarea
 * so copy operations always work seamlessly in development, staging, and HTTP environments.
 *
 * @param text The string to copy to the clipboard.
 * @returns Promise<boolean> Resolves to true if successfully copied, false otherwise.
 */
export async function copyToClipboard(text: string): Promise<boolean> {
  if (!text) {
    return false;
  }

  // 1. Try modern asynchronous Clipboard API if available (HTTPS or localhost)
  if (
    typeof navigator !== "undefined" &&
    navigator.clipboard &&
    typeof navigator.clipboard.writeText === "function"
  ) {
    try {
      await navigator.clipboard.writeText(text);
      return true;
    } catch (err) {
      // Permission denied or writeText failed; proceed to fallback
      console.warn("navigator.clipboard.writeText failed, trying fallback:", err);
    }
  }

  // 2. Fallback for non-secure HTTP contexts (e.g. AWS EC2 IP over HTTP)
  if (typeof document !== "undefined") {
    try {
      const textArea = document.createElement("textarea");
      textArea.value = text;
      // Ensure element is off-screen and invisible to avoid UI disruption or scroll jumping
      textArea.setAttribute("readonly", "");
      textArea.style.position = "fixed";
      textArea.style.top = "-9999px";
      textArea.style.left = "-9999px";
      textArea.style.opacity = "0";
      textArea.style.pointerEvents = "none";
      document.body.appendChild(textArea);

      textArea.focus();
      textArea.select();
      // For mobile devices: set selection range
      textArea.setSelectionRange(0, text.length);

      const successful = document.execCommand("copy");
      document.body.removeChild(textArea);
      return successful;
    } catch (fallbackErr) {
      console.error("document.execCommand('copy') failed:", fallbackErr);
      return false;
    }
  }

  return false;
}

