import type * as monaco from "monaco-editor"

/**
 * Converts a TanStack Hotkey string (e.g. "Mod+B", "Mod+Alt+1", "Alt+Shift+H")
 * to a Monaco Editor keybinding bitmask number.
 */
export function parseTanstackHotkeyToMonaco(
  monacoInstance: typeof monaco,
  hotkey: string
): number | null {
  if (!hotkey) return null

  const parts = hotkey.split("+")
  let modBits = 0
  let keyPart = ""

  for (const part of parts) {
    const p = part.trim().toLowerCase()
    if (p === "mod" || p === "ctrl" || p === "cmd" || p === "meta") {
      modBits |= monacoInstance.KeyMod.CtrlCmd
    } else if (p === "alt" || p === "option") {
      modBits |= monacoInstance.KeyMod.Alt
    } else if (p === "shift") {
      modBits |= monacoInstance.KeyMod.Shift
    } else {
      keyPart = part.trim()
    }
  }

  if (!keyPart) return null

  // Digits: 0-9
  if (/^[0-9]$/.test(keyPart)) {
    const digitCode =
      monacoInstance.KeyCode[
        `Digit${keyPart}` as keyof typeof monacoInstance.KeyCode
      ]
    return digitCode !== undefined ? modBits | digitCode : null
  }

  // Letters: A-Z
  if (/^[a-zA-Z]$/.test(keyPart)) {
    const letterCode =
      monacoInstance.KeyCode[
        `Key${keyPart.toUpperCase()}` as keyof typeof monacoInstance.KeyCode
      ]
    return letterCode !== undefined ? modBits | letterCode : null
  }

  // Common symbols and punctuation
  switch (keyPart) {
    case ",":
      return modBits | monacoInstance.KeyCode.Comma
    case ".":
      return modBits | monacoInstance.KeyCode.Period
    case "-":
      return modBits | monacoInstance.KeyCode.Minus
    case "=":
      return modBits | monacoInstance.KeyCode.Equal
    case "/":
      return modBits | monacoInstance.KeyCode.Slash
    case "`":
      return modBits | monacoInstance.KeyCode.Backquote
    case ";":
      return modBits | monacoInstance.KeyCode.Semicolon
    case "'":
      return modBits | monacoInstance.KeyCode.Quote
    default:
      return null
  }
}
