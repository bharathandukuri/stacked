import type * as monaco from "monaco-editor"

/**
 * Handles Enter key press for intelligent Markdown list and blockquote continuation:
 * 1. Unordered lists: `- `, `* `, `+ ` -> continues with the same bullet and indent
 * 2. Ordered lists: `1. `, `2. ` -> continues with incremented number and same indent
 * 3. Task lists: `- [ ] `, `- [x] ` -> continues with `- [ ] ` (unchecked)
 * 4. Blockquotes: `> ` -> continues with `> `
 * 5. Empty list item / blockquote: pressing Enter clears the marker to cleanly exit the list
 */
export function handleMarkdownListEnter(
  editor: monaco.editor.IStandaloneCodeEditor
): boolean {
  const model = editor.getModel()
  const selection = editor.getSelection()
  if (!model || !selection) return false

  // Only handle when there is no multi-character selection
  if (!selection.isEmpty()) return false

  const lineNumber = selection.startLineNumber
  const column = selection.startColumn
  const lineContent = model.getLineContent(lineNumber)

  // Part of line before cursor
  const textBeforeCursor = lineContent.substring(0, column - 1)

  // 1. Check if current line is an EMPTY list item (e.g. "- ", "* ", "+ ", "1. ", "1) ", "- [ ] ", "- [x] ")
  const emptyListMatch = /^(\s*)([-*+](\s+\[[ xX]\])?|\d+[.)])\s*$/.exec(
    lineContent
  )
  if (emptyListMatch) {
    const indent = emptyListMatch[1]
    // If indented by 2+ spaces, outdent by 2 spaces instead of fully exiting
    const replacement = indent.length >= 2 ? indent.slice(2) : ""

    editor.pushUndoStop()
    editor.executeEdits("markdown-list-exit", [
      {
        range: {
          startLineNumber: lineNumber,
          startColumn: 1,
          endLineNumber: lineNumber,
          endColumn: lineContent.length + 1,
        },
        text: replacement,
        forceMoveMarkers: true,
      },
    ])
    editor.setPosition({
      lineNumber,
      column: replacement.length + 1,
    })
    editor.pushUndoStop()
    return true
  }

  // 2. Check if current line is an EMPTY blockquote (e.g. "> ", "> > ")
  const emptyBlockquoteMatch = /^(\s*>)+\s*$/.exec(lineContent)
  if (emptyBlockquoteMatch) {
    editor.pushUndoStop()
    editor.executeEdits("markdown-blockquote-exit", [
      {
        range: {
          startLineNumber: lineNumber,
          startColumn: 1,
          endLineNumber: lineNumber,
          endColumn: lineContent.length + 1,
        },
        text: "",
        forceMoveMarkers: true,
      },
    ])
    editor.setPosition({
      lineNumber,
      column: 1,
    })
    editor.pushUndoStop()
    return true
  }

  // 3. Task list item: e.g. "- [ ] item" or "- [x] item"
  const taskListMatch = /^(\s*)([-*+])\s+\[[ xX]\]\s+(.*)$/.exec(
    textBeforeCursor
  )
  if (taskListMatch) {
    const indent = taskListMatch[1]
    const bullet = taskListMatch[2]
    const nextMarker = `${indent}${bullet} [ ] `
    insertContinuation(editor, lineNumber, column, nextMarker)
    return true
  }

  // 4. Unordered bullet list: e.g. "- item", "* item", "+ item"
  const unorderedMatch = /^(\s*)([-*+])\s+(.*)$/.exec(textBeforeCursor)
  if (unorderedMatch) {
    const indent = unorderedMatch[1]
    const bullet = unorderedMatch[2]
    const nextMarker = `${indent}${bullet} `
    insertContinuation(editor, lineNumber, column, nextMarker)
    return true
  }

  // 5. Ordered numbered list: e.g. "1. item", "2) item"
  const orderedMatch = /^(\s*)(\d+)([.)])\s+(.*)$/.exec(textBeforeCursor)
  if (orderedMatch) {
    const indent = orderedMatch[1]
    const currentNum = parseInt(orderedMatch[2], 10)
    const delimiter = orderedMatch[3]
    const nextNum = currentNum + 1
    const nextMarker = `${indent}${nextNum}${delimiter} `
    insertContinuation(editor, lineNumber, column, nextMarker)
    return true
  }

  // 6. Blockquote: e.g. "> quote"
  const blockquoteMatch = /^(\s*>+\s*)(.*)$/.exec(textBeforeCursor)
  if (blockquoteMatch) {
    const marker = blockquoteMatch[1]
    insertContinuation(editor, lineNumber, column, marker)
    return true
  }

  return false
}

/**
 * Handles Tab and Shift+Tab key press on list items to indent/outdent
 */
export function handleMarkdownListTab(
  editor: monaco.editor.IStandaloneCodeEditor,
  outdent: boolean
): boolean {
  const model = editor.getModel()
  const selection = editor.getSelection()
  if (!model || !selection) return false

  const lineNumber = selection.startLineNumber
  const lineContent = model.getLineContent(lineNumber)

  // Check if current line is a list item
  const listMatch = /^(\s*)([-*+](\s+\[[ xX]\])?|\d+[.)])\s+/.exec(lineContent)
  if (!listMatch) return false

  const currentIndent = listMatch[1]
  const column = selection.startColumn

  if (outdent) {
    if (currentIndent.length < 2) return false
    const spacesToRemove = currentIndent.startsWith("  ") ? 2 : 1
    editor.pushUndoStop()
    editor.executeEdits("markdown-list-outdent", [
      {
        range: {
          startLineNumber: lineNumber,
          startColumn: 1,
          endLineNumber: lineNumber,
          endColumn: spacesToRemove + 1,
        },
        text: "",
        forceMoveMarkers: true,
      },
    ])
    editor.setPosition({
      lineNumber,
      column: Math.max(1, column - spacesToRemove),
    })
    editor.pushUndoStop()
    return true
  } else {
    // Indent by 2 spaces
    editor.pushUndoStop()
    editor.executeEdits("markdown-list-indent", [
      {
        range: {
          startLineNumber: lineNumber,
          startColumn: 1,
          endLineNumber: lineNumber,
          endColumn: 1,
        },
        text: "  ",
        forceMoveMarkers: true,
      },
    ])
    editor.setPosition({
      lineNumber,
      column: column + 2,
    })
    editor.pushUndoStop()
    return true
  }
}

function insertContinuation(
  editor: monaco.editor.IStandaloneCodeEditor,
  lineNumber: number,
  column: number,
  nextMarker: string
) {
  editor.pushUndoStop()
  editor.executeEdits("markdown-list-continue", [
    {
      range: {
        startLineNumber: lineNumber,
        startColumn: column,
        endLineNumber: lineNumber,
        endColumn: column,
      },
      text: `\n${nextMarker}`,
      forceMoveMarkers: true,
    },
  ])
  editor.setPosition({
    lineNumber: lineNumber + 1,
    column: nextMarker.length + 1,
  })
  editor.pushUndoStop()
}
