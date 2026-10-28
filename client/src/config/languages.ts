/**
 * Language Configuration
 *
 * Single source of truth for programming languages and compilers supported and registered
 * at the backend (LanguageFactoryImpl & LanguageRegistryImpl).
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
  // --- Generic Algorithmic Languages (5 supported by backend) ---
  {
    id: "javascript-node-20",
    name: "JavaScript (Node.js 20)",
    category: "GENERIC",
    monacoLanguage: "javascript",
    defaultExtension: ".js",
    defaultStarterCode: `// File: solution.js
`,
    defaultValidatorCode: `// File: verifier.js
const fs = require('fs');

// File paths passed via CLI arguments:
// argv[2]: testcase input file
// argv[3]: candidate output file
// argv[4]: reference / expected output file
const [,, inputFile, candidateOutputFile, expectedOutputFile] = process.argv;

const input = fs.readFileSync(inputFile, 'utf-8');
const candidateOutput = fs.readFileSync(candidateOutputFile, 'utf-8');
// const expectedOutput = fs.readFileSync(expectedOutputFile, 'utf-8');

// Exit 0 for ACCEPTED, process.exit(1) for WRONG ANSWER
process.exit(0);
`,
  },
  {
    id: "python-3.12",
    name: "Python (CPython 3.12)",
    category: "GENERIC",
    monacoLanguage: "python",
    defaultExtension: ".py",
    defaultStarterCode: `# File: solution.py
`,
    defaultValidatorCode: `# File: verifier.py
import sys

# File paths passed via CLI arguments:
# sys.argv[1]: testcase input file
# sys.argv[2]: candidate output file
# sys.argv[3]: reference / expected output file
input_path, candidate_path, expected_path = sys.argv[1], sys.argv[2], sys.argv[3]

with open(input_path, 'r') as f:
    testcase_input = f.read()

with open(candidate_path, 'r') as f:
    candidate_output = f.read()

# with open(expected_path, 'r') as f:
#     expected_output = f.read()

# Exit 0 for ACCEPTED, sys.exit(1) for WRONG ANSWER
sys.exit(0)
`,
  },
  {
    id: "java-21",
    name: "Java (OpenJDK 21)",
    category: "GENERIC",
    monacoLanguage: "java",
    defaultExtension: ".java",
    defaultStarterCode: `// File: Solution.java
`,
    defaultValidatorCode: `// File: Verifier.java
import java.nio.file.Files;
import java.nio.file.Path;

public class Verifier {
    public static void main(String[] args) throws Exception {
        // args[0]: testcase input file
        // args[1]: candidate output file
        // args[2]: reference / expected output file
        String input = Files.readString(Path.of(args[0]));
        String candidateOutput = Files.readString(Path.of(args[1]));
        // String expectedOutput = Files.readString(Path.of(args[2]));

        // System.exit(0) for ACCEPTED, System.exit(1) for WRONG ANSWER
        System.exit(0);
    }
}
`,
  },
  {
    id: "cpp-23",
    name: "C++ (GCC 13.2)",
    category: "GENERIC",
    monacoLanguage: "cpp",
    defaultExtension: ".cpp",
    defaultStarterCode: `// File: solution.cpp
`,
    defaultValidatorCode: `// File: verifier.cpp
#include <iostream>
#include <fstream>
#include <string>
#include <cstdlib>

int main(int argc, char* argv[]) {
    // argv[1]: testcase input file
    // argv[2]: candidate output file
    // argv[3]: reference / expected output file
    if (argc < 3) return 1;

    std::ifstream in(argv[1]);
    std::ifstream cand(argv[2]);
    // std::ifstream exp(argv[3]);

    // return 0 for ACCEPTED, exit(1) for WRONG ANSWER
    return 0;
}
`,
  },
  {
    id: "c-17",
    name: "C (GCC 13.2)",
    category: "GENERIC",
    monacoLanguage: "c",
    defaultExtension: ".c",
    defaultStarterCode: `// File: solution.c
`,
    defaultValidatorCode: `// File: verifier.c
#include <stdio.h>
#include <stdlib.h>

int main(int argc, char* argv[]) {
    // argv[1]: testcase input file
    // argv[2]: candidate output file
    // argv[3]: reference / expected output file
    if (argc < 3) return 1;

    FILE* in = fopen(argv[1], "r");
    FILE* cand = fopen(argv[2], "r");
    // FILE* exp = fopen(argv[3], "r");

    // return 0 for ACCEPTED, exit(1) for WRONG ANSWER
    if (in) fclose(in);
    if (cand) fclose(cand);
    return 0;
}
`,
  },

  // --- Database Engines (4 supported by backend) ---
  {
    id: "mysql-8.0",
    name: "MySQL (8.0)",
    category: "DATABASE",
    monacoLanguage: "sql",
    defaultExtension: ".sql",
    defaultStarterCode: `-- File: solution.sql\n`,
    defaultValidatorCode: `-- File: verifier.sql\n`,
  },
  {
    id: "postgresql-16",
    name: "PostgreSQL (16)",
    category: "DATABASE",
    monacoLanguage: "sql",
    defaultExtension: ".sql",
    defaultStarterCode: `-- File: solution.sql\n`,
    defaultValidatorCode: `-- File: verifier.sql\n`,
  },
  {
    id: "sqlite-3",
    name: "SQLite (3.45)",
    category: "DATABASE",
    monacoLanguage: "sql",
    defaultExtension: ".sql",
    defaultStarterCode: `-- File: solution.sql\n`,
    defaultValidatorCode: `-- File: verifier.sql\n`,
  },
  {
    id: "mongodb-8.0",
    name: "MongoDB (8.0)",
    category: "DATABASE",
    monacoLanguage: "javascript",
    defaultExtension: ".js",
    defaultStarterCode: `// File: solution.js\n`,
    defaultValidatorCode: `// File: verifier.js\n`,
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
  const normalized = id.trim().toLowerCase()
  if (
    normalized === "sqlite-3.45" ||
    normalized === "sqlite" ||
    normalized === "sqlite3"
  ) {
    return LANGUAGE_REGISTRY.find((lang) => lang.id === "sqlite-3")
  }
  if (normalized === "mongodb" || normalized === "mongo") {
    return LANGUAGE_REGISTRY.find((lang) => lang.id === "mongodb-8.0")
  }
  return LANGUAGE_REGISTRY.find((lang) => lang.id === normalized)
}

export function getDefaultLanguageIds(category: ProblemCategory): string[] {
  if (category === "DATABASE") {
    return ["postgresql-16"]
  }
  return ["javascript-node-20", "python-3.12", "java-21"]
}

export function getLanguageCheckboxOptions(category: ProblemCategory) {
  const list =
    category === "DATABASE" ? getDatabaseLanguages() : getGenericLanguages()
  return list.map((lang) => ({
    value: lang.id,
    label: lang.name,
  }))
}

/* ==========================================================================
   Filename and Constraint Utilities (Matching LanguageFileNameRegistry)
   ========================================================================== */

export function resolveReferenceSolutionFileName(langId: string): string {
  const id = langId.toLowerCase().trim()
  if (id === "java-21") return "Solution.java"
  if (id === "cpp-23") return "solution.cpp"
  if (id === "c-17") return "solution.c"
  if (id === "python-3.12") return "solution.py"
  if (id === "javascript-node-20") return "solution.js"
  if (
    id === "sqlite-3" ||
    id === "sqlite-3.45" ||
    id === "postgresql-16" ||
    id === "mysql-8.0"
  ) {
    return "solution.sql"
  }
  if (id === "mongodb-8.0") {
    return "solution.js"
  }
  const lang = getLanguageById(langId)
  return lang ? `solution${lang.defaultExtension}` : "solution.txt"
}

export function resolveVerificationScriptFileName(langId: string): string {
  const id = langId.toLowerCase().trim()
  if (id === "java-21") return "Verifier.java"
  if (id === "cpp-23") return "verifier.cpp"
  if (id === "c-17") return "verifier.c"
  if (id === "python-3.12") return "verifier.py"
  if (id === "javascript-node-20" || id === "mongodb-8.0") return "verifier.js"
  if (
    id === "sqlite-3" ||
    id === "sqlite-3.45" ||
    id === "postgresql-16" ||
    id === "mysql-8.0"
  ) {
    return "verifier.sql"
  }
  const lang = getLanguageById(langId)
  return lang ? `verifier${lang.defaultExtension}` : "verifier.txt"
}

export function resolveStarterCodeFileName(langId: string): string {
  return resolveReferenceSolutionFileName(langId)
}

export function resolveGeneratorScriptFileName(langId: string): string {
  const id = langId.toLowerCase().trim()
  if (id === "java-21") return "Generator.java"
  if (id === "cpp-23") return "generator.cpp"
  if (id === "c-17") return "generator.c"
  if (id === "python-3.12") return "generator.py"
  if (id === "javascript-node-20") return "generator.js"
  const lang = getLanguageById(langId)
  return lang ? `generator${lang.defaultExtension}` : "generator.txt"
}

export function resolveDatabaseInitFileName(langId: string): string {
  const id = langId.toLowerCase().trim()
  if (id === "mongodb-8.0" || id.includes("mongo")) return "init.js"
  return "init.sql"
}

export function getDefaultExecutionConstraints(langId: string): {
  timeLimitMs: number
  memoryLimitKb: number
} {
  const id = langId.toLowerCase().trim()
  if (id === "java-21") {
    return { timeLimitMs: 2000, memoryLimitKb: 262144 } // 2s, 256MB
  }
  if (id === "cpp-23" || id === "c-17") {
    return { timeLimitMs: 1000, memoryLimitKb: 131072 } // 1s, 128MB
  }
  if (id === "python-3.12") {
    return { timeLimitMs: 3000, memoryLimitKb: 262144 } // 3s, 256MB
  }
  if (id === "javascript-node-20") {
    return { timeLimitMs: 2000, memoryLimitKb: 262144 } // 2s, 256MB
  }
  if (id === "sqlite-3" || id === "sqlite-3.45") {
    return { timeLimitMs: 5000, memoryLimitKb: 262144 } // 5s, 256MB
  }
  if (id === "postgresql-16" || id === "mysql-8.0" || id === "mongodb-8.0") {
    return { timeLimitMs: 5000, memoryLimitKb: 524288 } // 5s, 512MB
  }
  return { timeLimitMs: 2000, memoryLimitKb: 262144 }
}
