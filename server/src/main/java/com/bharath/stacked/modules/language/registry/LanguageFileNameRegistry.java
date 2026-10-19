package com.bharath.stacked.modules.language.registry;

import com.bharath.stacked.modules.language.Language;

public interface LanguageFileNameRegistry {

    /**
     * Resolves the source file name for a reference solution in the given language.
     * E.g., Java: "Solution.java", Python: "solution.py", C++: "solution.cpp", SQLite: "solution.sql"
     */
    String resolveReferenceSolutionFileName(Language language);

    String resolveReferenceSolutionFileName(String languageId);

    /**
     * Resolves the source file name for a verification script in the given language.
     * E.g., Java: "Verifier.java", Python: "verifier.py", C++: "verifier.cpp", SQLite: "verifier.sql"
     */
    String resolveVerificationScriptFileName(Language language);

    String resolveVerificationScriptFileName(String languageId);

    /**
     * Resolves the file name for database schema initialization (DDL/DML setup script).
     * E.g., SQL engines: "init.sql", MongoDB: "init.js"
     */
    String resolveDatabaseInitFileName(Language language);

    String resolveDatabaseInitFileName(String languageId);

    /**
     * Resolves the file name for database query/solution script.
     * E.g., SQL engines: "solution.sql", MongoDB: "solution.js"
     */
    String resolveDatabaseSolutionFileName(Language language);

    String resolveDatabaseSolutionFileName(String languageId);

    /**
     * Resolves the default file name for the given language.
     */
    String resolveDefaultFileName(Language language);

    String resolveDefaultFileName(String languageId);
}

