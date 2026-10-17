package com.bharath.stacked.modules.language.factory.impl;

import com.bharath.stacked.modules.language.CompiledLanguage;
import com.bharath.stacked.modules.language.DatabaseLanguage;
import com.bharath.stacked.modules.language.InterpretedLanguage;
import com.bharath.stacked.modules.language.Language;
import com.bharath.stacked.modules.language.exception.LanguageNotFoundException;
import com.bharath.stacked.modules.language.factory.LanguageFactory;
import com.bharath.stacked.modules.language.impl.DefaultCompiledLanguage;
import com.bharath.stacked.modules.language.impl.DefaultDatabaseLanguage;
import com.bharath.stacked.modules.language.impl.DefaultInterpretedLanguage;
import com.bharath.stacked.modules.execution.registry.DockerImageRegistry;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Component
public class LanguageFactoryImpl implements LanguageFactory {

    public static final String ID_JAVA_21 = "java-21";
    public static final String ID_C_17 = "c-17";
    public static final String ID_CPP_23 = "cpp-23";
    public static final String ID_PYTHON_3_12 = "python-3.12";
    public static final String ID_JAVASCRIPT_NODE_20 = "javascript-node-20";
    public static final String ID_MYSQL_8_0 = "mysql-8.0";
    public static final String ID_POSTGRESQL_16 = "postgresql-16";
    public static final String ID_SQLITE_3 = "sqlite-3";
    public static final String ID_MONGODB_8_0 = "mongodb-8.0";

    private static final List<String> SUPPORTED_IDS = List.of(
            ID_JAVA_21,
            ID_C_17,
            ID_CPP_23,
            ID_PYTHON_3_12,
            ID_JAVASCRIPT_NODE_20,
            ID_MYSQL_8_0,
            ID_POSTGRESQL_16,
            ID_SQLITE_3,
            ID_MONGODB_8_0
    );

    @Override
    public Language create(String languageId) {
        if (languageId == null || languageId.isBlank()) {
            throw new LanguageNotFoundException("Language ID must not be null or blank.");
        }

        return switch (languageId.trim().toLowerCase()) {
            case ID_JAVA_21 -> createJava21();
            case ID_C_17 -> createC17();
            case ID_CPP_23 -> createCpp23();
            case ID_PYTHON_3_12 -> createPython312();
            case ID_JAVASCRIPT_NODE_20 -> createJavaScriptNode20();
            case ID_MYSQL_8_0 -> createMySql80();
            case ID_POSTGRESQL_16 -> createPostgreSql16();
            case ID_SQLITE_3, "sqlite", "sqlite3" -> createSqlite3();
            case ID_MONGODB_8_0, "mongodb", "mongo" -> createMongoDb80();
            default -> throw new LanguageNotFoundException(languageId);
        };
    }

    @Override
    public CompiledLanguage createCompiled(
            String id,
            String name,
            String fileExtension,
            DockerImageRegistry dockerImage,
            Function<List<String>, List<String>> compileCommandGenerator,
            Function<List<String>, List<String>> runCommandGenerator
    ) {
        return new DefaultCompiledLanguage(
                id,
                name,
                fileExtension,
                dockerImage,
                compileCommandGenerator,
                runCommandGenerator
        );
    }

    @Override
    public InterpretedLanguage createInterpreted(
            String id,
            String name,
            String fileExtension,
            DockerImageRegistry dockerImage,
            Function<List<String>, List<String>> runCommandGenerator
    ) {
        return new DefaultInterpretedLanguage(
                id,
                name,
                fileExtension,
                dockerImage,
                runCommandGenerator
        );
    }

    @Override
    public DatabaseLanguage createDatabase(
            String id,
            String name,
            String fileExtension,
            DockerImageRegistry dockerImage,
            Function<List<String>, List<String>> runCommandGenerator
    ) {
        return new DefaultDatabaseLanguage(
                id,
                name,
                fileExtension,
                dockerImage,
                runCommandGenerator
        );
    }

    @Override
    public List<String> supportedLanguageIds() {
        return SUPPORTED_IDS;
    }

    // =========================================================================
    // Built-in Language Creators
    // =========================================================================

    private CompiledLanguage createJava21() {
        return new DefaultCompiledLanguage(
                ID_JAVA_21,
                "Java (OpenJDK 21)",
                ".java",
                DockerImageRegistry.JAVA_21,
                files -> {
                    List<String> effectiveFiles = (files == null || files.isEmpty())
                            ? List.of("Solution.java")
                            : files;
                    List<String> command = new ArrayList<>();
                    command.add("javac");
                    command.addAll(effectiveFiles);
                    return command;
                },
                files -> {
                    String mainFile = (files == null || files.isEmpty())
                            ? "Solution.java"
                            : files.get(0);
                    String className = extractBaseName(mainFile);
                    return List.of("java", "-Xmx192m", "-cp", ".", className);
                },
                (files, constraints) -> {
                    String mainFile = (files == null || files.isEmpty())
                            ? "Solution.java"
                            : files.get(0);
                    String className = extractBaseName(mainFile);
                    int heapMb = 192;
                    if (constraints != null && constraints.memoryLimitKb() != null && constraints.memoryLimitKb() > 0) {
                        heapMb = Math.max(16, (int) (constraints.memoryLimitKb() / 1024L * 3L / 4L));
                    }
                    return List.of("java", "-Xmx" + heapMb + "m", "-cp", ".", className);
                },
                "Main.java",
                "Main"
        );
    }

    private CompiledLanguage createC17() {
        return new DefaultCompiledLanguage(
                ID_C_17,
                "C (GCC 13.2)",
                ".c",
                DockerImageRegistry.C_17,
                files -> {
                    List<String> effectiveFiles = (files == null || files.isEmpty())
                            ? List.of("solution.c")
                            : files;
                    List<String> command = new ArrayList<>();
                    command.add("gcc");
                    command.add("-std=c17");
                    command.add("-O2");
                    command.add("-Wall");
                    command.addAll(effectiveFiles);
                    command.add("-o");
                    command.add("a.out");
                    command.add("-lm");
                    return command;
                },
                files -> List.of("./a.out"),
                "main.c",
                "a.out"
        );
    }

    private CompiledLanguage createCpp23() {
        return new DefaultCompiledLanguage(
                ID_CPP_23,
                "C++ (GCC 13.2)",
                ".cpp",
                DockerImageRegistry.CPP_23,
                files -> {
                    List<String> effectiveFiles = (files == null || files.isEmpty())
                            ? List.of("solution.cpp")
                            : files;
                    List<String> command = new ArrayList<>();
                    command.add("g++");
                    command.add("-std=c++23");
                    command.add("-O2");
                    command.add("-Wall");
                    command.addAll(effectiveFiles);
                    command.add("-o");
                    command.add("a.out");
                    return command;
                },
                files -> List.of("./a.out"),
                "main.cpp",
                "a.out"
        );
    }

    private InterpretedLanguage createPython312() {
        return new DefaultInterpretedLanguage(
                ID_PYTHON_3_12,
                "Python (CPython 3.12)",
                ".py",
                DockerImageRegistry.PYTHON_3_12,
                files -> {
                    String scriptFile = (files == null || files.isEmpty())
                            ? "solution.py"
                            : files.get(0);
                    return List.of("python3", scriptFile);
                },
                "main.py"
        );
    }

    private InterpretedLanguage createJavaScriptNode20() {
        return new DefaultInterpretedLanguage(
                ID_JAVASCRIPT_NODE_20,
                "JavaScript (Node.js 20)",
                ".js",
                DockerImageRegistry.JAVASCRIPT_NODE_20,
                files -> {
                    String scriptFile = (files == null || files.isEmpty())
                            ? "solution.js"
                            : files.get(0);
                    return List.of("node", "--max-old-space-size=192", scriptFile);
                },
                (files, constraints) -> {
                    String scriptFile = (files == null || files.isEmpty())
                            ? "solution.js"
                            : files.get(0);
                    int heapMb = 192;
                    if (constraints != null && constraints.memoryLimitKb() != null && constraints.memoryLimitKb() > 0) {
                        heapMb = Math.max(16, (int) (constraints.memoryLimitKb() / 1024L * 3L / 4L));
                    }
                    return List.of("node", "--max-old-space-size=" + heapMb, scriptFile);
                },
                "main.js"
        );
    }

    private DatabaseLanguage createMySql80() {
        return new DefaultDatabaseLanguage(
                ID_MYSQL_8_0,
                "MySQL (8.0)",
                ".sql",
                DockerImageRegistry.MYSQL_8_0,
                files -> {
                    String scriptFile = (files == null || files.isEmpty())
                            ? "solution.sql"
                            : files.get(0);
                    return List.of("mysql", "-u", "root", "-pstacked_judge", "stacked_judge_db", "-e", "source " + scriptFile);
                },
                List.of("mysql", "-S", "/var/run/mysqld/mysqld.sock", "stacked_judge_db", "-e", "SELECT 1;"),
                true,
                "main.sql"
        );
    }

    private DatabaseLanguage createPostgreSql16() {
        return new DefaultDatabaseLanguage(
                ID_POSTGRESQL_16,
                "PostgreSQL (16)",
                ".sql",
                DockerImageRegistry.POSTGRES_16,
                files -> {
                    String scriptFile = (files == null || files.isEmpty())
                            ? "solution.sql"
                            : files.get(0);
                    return List.of("psql", "-U", "postgres", "-d", "stacked_judge_db", "-f", scriptFile);
                },
                List.of("psql", "-U", "postgres", "-d", "stacked_judge_db", "-c", "SELECT 1;"),
                true,
                "main.sql"
        );
    }

    private DatabaseLanguage createSqlite3() {
        return new DefaultDatabaseLanguage(
                ID_SQLITE_3,
                "SQLite (3.45)",
                ".sql",
                DockerImageRegistry.SQLITE_3,
                files -> {
                    String scriptFile = (files == null || files.isEmpty())
                            ? "main.sql"
                            : files.get(0);
                    return List.of("sqlite3", "-header", "-column", "/tmp/stacked_judge.db", ".read " + scriptFile);
                },
                List.of(),
                false,
                "main.sql"
        );
    }

    private DatabaseLanguage createMongoDb80() {
        return new DefaultDatabaseLanguage(
                ID_MONGODB_8_0,
                "MongoDB (8.0)",
                ".js",
                DockerImageRegistry.MONGODB_8_0,
                files -> {
                    String scriptFile = (files == null || files.isEmpty())
                            ? "solution.js"
                            : files.get(0);
                    return List.of("mongosh", "--quiet", "--norc", "mongodb://127.0.0.1:27017/stacked_judge_db", "--file", scriptFile);
                },
                List.of("mongosh", "--quiet", "--eval", "db.adminCommand('ping')"),
                true,
                "main.js"
        );
    }

    private String extractBaseName(String filePath) {
        String fileName = new File(filePath).getName();
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }
}
