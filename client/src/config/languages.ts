/**
 * Language Configuration
 *
 * Lightweight client configuration for supported programming languages and compilers.
 * Language entries are flattened with descriptive names like "Java (OpenJDK 21)".
 */

export type ProblemCategory = "GENERIC" | "DATABASE"

export interface LanguageConfig {
  id: string
  name: string
  category: ProblemCategory
  monacoLanguage: string
  defaultExtension: string
  defaultStarterCode: string
  defaultValidatorCode: string
}

export const LANGUAGE_REGISTRY: readonly LanguageConfig[] = [
  // --- Generic Algorithmic Languages ---
  {
    id: "javascript-node-20",
    name: "JavaScript (Node.js 20)",
    category: "GENERIC",
    monacoLanguage: "javascript",
    defaultExtension: ".js",
    defaultStarterCode: `/**\n * @param {string} input\n * @return {any}\n */\nfunction solve(input) {\n    // Write your solution here\n    \n}\n`,
    defaultValidatorCode: `// Solution Validator (reads input and actual output separated by delimiter)\nconst fs = require('fs');\n\nconst raw = fs.readFileSync(0, 'utf-8');\nconst [input, actualOutput] = raw.split('\\n---OUTPUT---\\n');\n\n// Assert validity (exit with 0 on pass, non-zero on fail)\nif (actualOutput === undefined || actualOutput.trim() === '') {\n    process.exit(1);\n}\nprocess.exit(0);\n`,
  },
  {
    id: "typescript-node-20",
    name: "TypeScript (Node.js 20)",
    category: "GENERIC",
    monacoLanguage: "typescript",
    defaultExtension: ".ts",
    defaultStarterCode: `function solve(input: string): any {\n    // Write your solution here\n    \n}\n`,
    defaultValidatorCode: `import * as fs from 'fs';\n\nconst raw = fs.readFileSync(0, 'utf-8');\nconst [input, actualOutput] = raw.split('\\n---OUTPUT---\\n');\n\nif (actualOutput === undefined || actualOutput.trim() === '') {\n    process.exit(1);\n}\nprocess.exit(0);\n`,
  },
  {
    id: "python-3.12",
    name: "Python (CPython 3.12)",
    category: "GENERIC",
    monacoLanguage: "python",
    defaultExtension: ".py",
    defaultStarterCode: `class Solution:\n    def solve(self, input_data: str):\n        # Write your solution here\n        pass\n`,
    defaultValidatorCode: `import sys\n\nraw = sys.stdin.read()\nparts = raw.split('\\n---OUTPUT---\\n')\ninput_data = parts[0]\nactual_output = parts[1] if len(parts) > 1 else ''\n\n# Assert validity (exit with 0 on pass, non-zero on fail)\nif not actual_output.strip():\n    sys.exit(1)\nsys.exit(0)\n`,
  },
  {
    id: "python-pypy",
    name: "Python (PyPy 3.10)",
    category: "GENERIC",
    monacoLanguage: "python",
    defaultExtension: ".py",
    defaultStarterCode: `class Solution:\n    def solve(self, input_data: str):\n        # Write your solution here\n        pass\n`,
    defaultValidatorCode: `import sys\n\nraw = sys.stdin.read()\nparts = raw.split('\\n---OUTPUT---\\n')\ninput_data = parts[0]\nactual_output = parts[1] if len(parts) > 1 else ''\n\nif not actual_output.strip():\n    sys.exit(1)\nsys.exit(0)\n`,
  },
  {
    id: "java-21",
    name: "Java (OpenJDK 21)",
    category: "GENERIC",
    monacoLanguage: "java",
    defaultExtension: ".java",
    defaultStarterCode: `class Solution {\n    public Object solve(String input) {\n        // Write your solution here\n        return null;\n    }\n}\n`,
    defaultValidatorCode: `import java.util.Scanner;\n\npublic class Validator {\n    public static void main(String[] args) {\n        Scanner scanner = new Scanner(System.in);\n        StringBuilder sb = new StringBuilder();\n        while (scanner.hasNextLine()) {\n            sb.append(scanner.nextLine()).append("\\n");\n        }\n        String raw = sb.toString();\n        String[] parts = raw.split("\\n---OUTPUT---\\n");\n        if (parts.length < 2 || parts[1].trim().isEmpty()) {\n            System.exit(1);\n        }\n        System.exit(0);\n    }\n}\n`,
  },
  {
    id: "java-17",
    name: "Java (OpenJDK 17)",
    category: "GENERIC",
    monacoLanguage: "java",
    defaultExtension: ".java",
    defaultStarterCode: `class Solution {\n    public Object solve(String input) {\n        // Write your solution here\n        return null;\n    }\n}\n`,
    defaultValidatorCode: `import java.util.Scanner;\n\npublic class Validator {\n    public static void main(String[] args) {\n        Scanner scanner = new Scanner(System.in);\n        StringBuilder sb = new StringBuilder();\n        while (scanner.hasNextLine()) {\n            sb.append(scanner.nextLine()).append("\\n");\n        }\n        String raw = sb.toString();\n        String[] parts = raw.split("\\n---OUTPUT---\\n");\n        if (parts.length < 2 || parts[1].trim().isEmpty()) {\n            System.exit(1);\n        }\n        System.exit(0);\n    }\n}\n`,
  },
  {
    id: "cpp-23",
    name: "C++ (GCC 13.2)",
    category: "GENERIC",
    monacoLanguage: "cpp",
    defaultExtension: ".cpp",
    defaultStarterCode: `#include <iostream>\n#include <string>\n\nclass Solution {\npublic:\n    void solve(const std::string& input) {\n        // Write your solution here\n    }\n};\n`,
    defaultValidatorCode: `#include <iostream>\n#include <string>\n\nint main() {\n    std::string line, raw;\n    while (std::getline(std::cin, line)) {\n        raw += line + "\\n";\n    }\n    std::string delimiter = "\\n---OUTPUT---\\n";\n    size_t pos = raw.find(delimiter);\n    if (pos == std::string::npos) {\n        return 1;\n    }\n    return 0;\n}\n`,
  },
  {
    id: "cpp-clang-17",
    name: "C++ (Clang 17)",
    category: "GENERIC",
    monacoLanguage: "cpp",
    defaultExtension: ".cpp",
    defaultStarterCode: `#include <iostream>\n#include <string>\n\nclass Solution {\npublic:\n    void solve(const std::string& input) {\n        // Write your solution here\n    }\n};\n`,
    defaultValidatorCode: `#include <iostream>\n#include <string>\n\nint main() {\n    std::string line, raw;\n    while (std::getline(std::cin, line)) {\n        raw += line + "\\n";\n    }\n    std::string delimiter = "\\n---OUTPUT---\\n";\n    size_t pos = raw.find(delimiter);\n    if (pos == std::string::npos) {\n        return 1;\n    }\n    return 0;\n}\n`,
  },
  {
    id: "c-17",
    name: "C (GCC 13.2)",
    category: "GENERIC",
    monacoLanguage: "c",
    defaultExtension: ".c",
    defaultStarterCode: `#include <stdio.h>\n#include <stdlib.h>\n\nvoid solve(const char* input) {\n    // Write your solution here\n}\n`,
    defaultValidatorCode: `#include <stdio.h>\n#include <string.h>\n\nint main() {\n    return 0;\n}\n`,
  },
  {
    id: "csharp-dotnet-8",
    name: "C# (.NET 8.0)",
    category: "GENERIC",
    monacoLanguage: "csharp",
    defaultExtension: ".cs",
    defaultStarterCode: `public class Solution {\n    public void Solve(string input) {\n        // Write your solution here\n    }\n}\n`,
    defaultValidatorCode: `using System;\n\npublic class Validator {\n    public static int Main() {\n        string input = Console.In.ReadToEnd();\n        string[] parts = input.Split("\\n---OUTPUT---\\n");\n        if (parts.Length < 2 || string.IsNullOrWhiteSpace(parts[1])) return 1;\n        return 0;\n    }\n}\n`,
  },
  {
    id: "go-1.22",
    name: "Go (1.22)",
    category: "GENERIC",
    monacoLanguage: "go",
    defaultExtension: ".go",
    defaultStarterCode: `package main\n\nfunc solve(input string) {\n    // Write your solution here\n}\n`,
    defaultValidatorCode: `package main\n\nimport (\n    "io"\n    "os"\n    "strings"\n)\n\nfunc main() {\n    bytes, _ := io.ReadAll(os.Stdin)\n    parts := strings.Split(string(bytes), "\\n---OUTPUT---\\n")\n    if len(parts) < 2 || strings.TrimSpace(parts[1]) == "" {\n        os.Exit(1)\n    }\n    os.Exit(0)\n}\n`,
  },
  {
    id: "rust-1.76",
    name: "Rust (1.76)",
    category: "GENERIC",
    monacoLanguage: "rust",
    defaultExtension: ".rs",
    defaultStarterCode: `impl Solution {\n    pub fn solve(input: String) {\n        // Write your solution here\n    }\n}\n`,
    defaultValidatorCode: `use std::io::{self, Read};\n\nfn main() {\n    let mut buffer = String::new();\n    io::stdin().read_to_string(&mut buffer).unwrap();\n    let parts: Vec<&str> = buffer.split("\\n---OUTPUT---\\n").collect();\n    if parts.len() < 2 || parts[1].trim().is_empty() {\n        std::process::exit(1);\n    }\n    std::process::exit(0);\n}\n`,
  },

  // --- Database SQL Engines ---
  {
    id: "mysql-8.0",
    name: "MySQL (8.0)",
    category: "DATABASE",
    monacoLanguage: "sql",
    defaultExtension: ".sql",
    defaultStarterCode: "",
    defaultValidatorCode: `-- Reference SQL query for MySQL (8.0)\nSELECT\n    *\nFROM\n    Person;\n`,
  },
  {
    id: "postgresql-16",
    name: "PostgreSQL (16)",
    category: "DATABASE",
    monacoLanguage: "sql",
    defaultExtension: ".sql",
    defaultStarterCode: "",
    defaultValidatorCode: `-- Reference SQL query for PostgreSQL (16)\nSELECT\n    *\nFROM\n    Person;\n`,
  },
  {
    id: "sqlite-3.45",
    name: "SQLite (3.45)",
    category: "DATABASE",
    monacoLanguage: "sql",
    defaultExtension: ".sql",
    defaultStarterCode: "",
    defaultValidatorCode: `-- Reference SQL query for SQLite (3.45)\nSELECT\n    *\nFROM\n    Person;\n`,
  },
] as const

/* ==========================================================================
   Query Utilities
   ========================================================================== */

export function getAllLanguages(): LanguageConfig[] {
  return [...LANGUAGE_REGISTRY]
}

export function getGenericLanguages(): LanguageConfig[] {
  return LANGUAGE_REGISTRY.filter((lang) => lang.category === "GENERIC")
}

export function getDatabaseLanguages(): LanguageConfig[] {
  return LANGUAGE_REGISTRY.filter((lang) => lang.category === "DATABASE")
}

export function getLanguageById(id: string): LanguageConfig | undefined {
  return LANGUAGE_REGISTRY.find((lang) => lang.id === id)
}

export function getDefaultLanguageIds(category: ProblemCategory): string[] {
  if (category === "DATABASE") {
    return ["mysql-8.0", "postgresql-16", "sqlite-3.45"]
  }
  return ["javascript-node-20", "typescript-node-20", "python-3.12"]
}

export function getLanguageCheckboxOptions(category: ProblemCategory) {
  const list =
    category === "DATABASE" ? getDatabaseLanguages() : getGenericLanguages()
  return list.map((lang) => ({
    value: lang.id,
    label: lang.name,
  }))
}
