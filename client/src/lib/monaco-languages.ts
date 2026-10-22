import { getLanguageById } from "../config/languages.ts"

export function resolveMonacoLanguage(langId?: string): string {
  const norm = (langId || "").toLowerCase().trim()
  if (norm.startsWith("java") && !norm.startsWith("javascript")) return "java"
  if (norm.startsWith("python") || norm === "py") return "python"
  if (norm.startsWith("cpp") || norm.startsWith("c++")) return "cpp"
  if (norm === "c" || norm.startsWith("c-")) return "c"
  if (norm.startsWith("javascript") || norm.startsWith("js") || norm.startsWith("node")) return "javascript"
  if (norm.startsWith("sql") || norm.startsWith("postgre") || norm.startsWith("mysql") || norm.startsWith("sqlite")) return "sql"
  if (norm.startsWith("mongo")) return "javascript"
  const cfg = getLanguageById(norm)
  return cfg?.monacoLanguage || "plaintext"
}

export function resolveStarterCode(starterCodeMap?: Record<string, string>, langId?: string): string {
  if (!starterCodeMap || !langId) return ""
  if (starterCodeMap[langId]) return starterCodeMap[langId]
  const lower = langId.toLowerCase().trim()
  for (const [key, code] of Object.entries(starterCodeMap)) {
    if (key.toLowerCase().trim() === lower) return code
  }
  for (const [key, code] of Object.entries(starterCodeMap)) {
    const k = key.toLowerCase().trim()
    if (lower.startsWith(k) || k.startsWith(lower)) return code
  }
  return ""
}
