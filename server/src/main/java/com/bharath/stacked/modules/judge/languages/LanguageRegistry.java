package com.bharath.stacked.modules.judge.languages;

import com.bharath.stacked.modules.judge.config.JudgeProperties;
import com.bharath.stacked.modules.judge.enums.LanguageType;
import com.bharath.stacked.modules.judge.enums.ProblemCategory;
import com.bharath.stacked.modules.judge.languages.impl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry of all supported execution languages and compilers.
 * Supports distinct versions of the same language as unique language entities
 * (e.g. "java-21" vs "java-17"). All settings can be overridden via
 * application.properties or .env.
 */
@Component
public class LanguageRegistry {

    private static final Logger log = LoggerFactory.getLogger(LanguageRegistry.class);

    private final JudgeProperties judgeProperties;
    private final Map<String, LanguageDetails> registry = new ConcurrentHashMap<>();

    public LanguageRegistry(JudgeProperties judgeProperties) {
        this.judgeProperties = judgeProperties;
        initializeDefaults();
        applyConfigurationOverrides();
    }

    private void initializeDefaults() {
        // --- Java 21 ---
        register(JavaLanguageDetails.builder()
                .id("java-21")
                .name("Java (OpenJDK 21)")
                .category(ProblemCategory.GENERIC)
                .languageType(LanguageType.COMPILED)
                .dockerImage("eclipse-temurin:21-jdk-alpine")
                .firecrackerRootfs("rootfs-java-21.ext4")
                .defaultTimeoutMs(5000)
                .defaultMemoryLimitMb(256)
                .solutionFile("Solution.java")
                .validationFile("Validator.java")
                .build());

        // --- Java 17 ---
        register(JavaLanguageDetails.builder()
                .id("java-17")
                .name("Java (OpenJDK 17)")
                .category(ProblemCategory.GENERIC)
                .languageType(LanguageType.COMPILED)
                .dockerImage("eclipse-temurin:17-jdk-alpine")
                .firecrackerRootfs("rootfs-java-17.ext4")
                .defaultTimeoutMs(5000)
                .defaultMemoryLimitMb(256)
                .solutionFile("Solution.java")
                .validationFile("Validator.java")
                .build());

        // --- Python 3.12 (CPython) ---
        register(PythonLanguageDetails.builder()
                .id("python-3.12")
                .name("Python (CPython 3.12)")
                .category(ProblemCategory.GENERIC)
                .languageType(LanguageType.INTERPRETED)
                .dockerImage("python:3.12-alpine")
                .firecrackerRootfs("rootfs-python-3.12.ext4")
                .pythonBinary("python3")
                .defaultTimeoutMs(5000)
                .defaultMemoryLimitMb(256)
                .sourceFile("solution.py")
                .validationFile("validator.py")
                .build());

        // --- Python PyPy 3.10 ---
        register(PythonLanguageDetails.builder()
                .id("python-pypy")
                .name("Python (PyPy 3.10)")
                .category(ProblemCategory.GENERIC)
                .languageType(LanguageType.INTERPRETED)
                .dockerImage("pypy:3.10-slim")
                .firecrackerRootfs("rootfs-pypy-3.10.ext4")
                .pythonBinary("pypy3")
                .defaultTimeoutMs(5000)
                .defaultMemoryLimitMb(256)
                .sourceFile("solution.py")
                .validationFile("validator.py")
                .build());

        // --- C++ 23 (GCC 13.2) ---
        register(CppLanguageDetails.builder()
                .id("cpp-23")
                .name("C++ (GCC 13.2)")
                .category(ProblemCategory.GENERIC)
                .languageType(LanguageType.COMPILED)
                .dockerImage("gcc:13.2")
                .firecrackerRootfs("rootfs-gcc-13.ext4")
                .compilerBinary("g++")
                .cppStandard("c++23")
                .defaultTimeoutMs(3000)
                .defaultMemoryLimitMb(128)
                .solutionFile("Solution.cpp")
                .validationFile("Validator.cpp")
                .build());

        // --- C++ (Clang 17) ---
        register(CppLanguageDetails.builder()
                .id("cpp-clang-17")
                .name("C++ (Clang 17)")
                .category(ProblemCategory.GENERIC)
                .languageType(LanguageType.COMPILED)
                .dockerImage("silkeh/clang:17")
                .firecrackerRootfs("rootfs-clang-17.ext4")
                .compilerBinary("clang++")
                .cppStandard("c++2b")
                .defaultTimeoutMs(3000)
                .defaultMemoryLimitMb(128)
                .solutionFile("Solution.cpp")
                .validationFile("Validator.cpp")
                .build());

        // --- C 17 (GCC 13.2) ---
        register(CLanguageDetails.builder()
                .id("c-17")
                .name("C (GCC 13.2)")
                .category(ProblemCategory.GENERIC)
                .languageType(LanguageType.COMPILED)
                .dockerImage("gcc:13.2")
                .firecrackerRootfs("rootfs-gcc-13.ext4")
                .compilerBinary("gcc")
                .cStandard("c17")
                .defaultTimeoutMs(2000)
                .defaultMemoryLimitMb(128)
                .solutionFile("Solution.c")
                .validationFile("Validator.c")
                .build());

        // --- JavaScript (Node.js 20) ---
        register(NodeLanguageDetails.builder()
                .id("javascript-node-20")
                .name("JavaScript (Node.js 20)")
                .category(ProblemCategory.GENERIC)
                .languageType(LanguageType.INTERPRETED)
                .dockerImage("node:20-alpine")
                .firecrackerRootfs("rootfs-node-20.ext4")
                .nodeBinary("node")
                .defaultTimeoutMs(5000)
                .defaultMemoryLimitMb(256)
                .sourceFile("solution.js")
                .validationFile("validator.js")
                .build());

        // --- TypeScript (Node.js 20) ---
        register(NodeLanguageDetails.builder()
                .id("typescript-node-20")
                .name("TypeScript (Node.js 20)")
                .category(ProblemCategory.GENERIC)
                .languageType(LanguageType.INTERPRETED)
                .dockerImage("node:20-alpine")
                .firecrackerRootfs("rootfs-node-20.ext4")
                .nodeBinary("node")
                .defaultTimeoutMs(5000)
                .defaultMemoryLimitMb(256)
                .sourceFile("solution.ts")
                .validationFile("validator.ts")
                .build());

        // --- Go 1.22 ---
        register(GoLanguageDetails.builder()
                .id("go-1.22")
                .name("Go (1.22)")
                .category(ProblemCategory.GENERIC)
                .languageType(LanguageType.COMPILED)
                .dockerImage("golang:1.22-alpine")
                .firecrackerRootfs("rootfs-go-1.22.ext4")
                .defaultTimeoutMs(3000)
                .defaultMemoryLimitMb(256)
                .solutionFile("solution.go")
                .validationFile("validator.go")
                .build());

        // --- Rust 1.76 ---
        register(RustLanguageDetails.builder()
                .id("rust-1.76")
                .name("Rust (1.76)")
                .category(ProblemCategory.GENERIC)
                .languageType(LanguageType.COMPILED)
                .dockerImage("rust:1.76-alpine")
                .firecrackerRootfs("rootfs-rust-1.76.ext4")
                .defaultTimeoutMs(3000)
                .defaultMemoryLimitMb(256)
                .solutionFile("solution.rs")
                .validationFile("validator.rs")
                .build());

        // --- C# (.NET 8.0) ---
        register(CSharpLanguageDetails.builder()
                .id("csharp-dotnet-8")
                .name("C# (.NET 8.0)")
                .category(ProblemCategory.GENERIC)
                .languageType(LanguageType.COMPILED)
                .dockerImage("mcr.microsoft.com/dotnet/sdk:8.0-alpine")
                .firecrackerRootfs("rootfs-dotnet-8.ext4")
                .defaultTimeoutMs(5000)
                .defaultMemoryLimitMb(256)
                .solutionFile("Solution.cs")
                .validationFile("Validator.cs")
                .build());

        // --- MySQL 8.0 ---
        register(MySQLLanguageDetails.builder()
                .id("mysql-8.0")
                .name("MySQL (8.0)")
                .category(ProblemCategory.DATABASE)
                .languageType(LanguageType.DATABASE)
                .dockerImage("mysql:8.0")
                .firecrackerRootfs("rootfs-mysql-8.0.ext4")
                .defaultTimeoutMs(10000)
                .defaultMemoryLimitMb(512)
                .initFile("schema.sql")
                .solutionFile("solution.sql")
                .validationFile("reference.sql")
                .build());

        // --- PostgreSQL 16 ---
        register(PostgreSQLLanguageDetails.builder()
                .id("postgresql-16")
                .name("PostgreSQL (16)")
                .category(ProblemCategory.DATABASE)
                .languageType(LanguageType.DATABASE)
                .dockerImage("postgres:16-alpine")
                .firecrackerRootfs("rootfs-postgres-16.ext4")
                .defaultTimeoutMs(10000)
                .defaultMemoryLimitMb(512)
                .initFile("schema.sql")
                .solutionFile("solution.sql")
                .validationFile("reference.sql")
                .build());

        // --- SQLite 3.45 ---
        register(SQLiteLanguageDetails.builder()
                .id("sqlite-3.45")
                .name("SQLite (3.45)")
                .category(ProblemCategory.DATABASE)
                .languageType(LanguageType.DATABASE)
                .dockerImage("keinos/sqlite3:3.45.0")
                .firecrackerRootfs("rootfs-sqlite-3.45.ext4")
                .defaultTimeoutMs(5000)
                .defaultMemoryLimitMb(128)
                .initFile("schema.sql")
                .solutionFile("solution.sql")
                .validationFile("reference.sql")
                .build());

        log.info("Initialized LanguageRegistry with {} language definitions", registry.size());
    }

    private void applyConfigurationOverrides() {
        if (judgeProperties == null || judgeProperties.languages() == null) {
            return;
        }

        judgeProperties.languages().forEach((id, override) -> {
            LanguageDetails details = registry.get(id);
            if (details != null && override != null) {
                if (override.dockerImage() != null && !override.dockerImage().isBlank()) {
                    details.setDockerImage(override.dockerImage());
                }
                if (override.firecrackerRootfs() != null && !override.firecrackerRootfs().isBlank()) {
                    details.setFirecrackerRootfs(override.firecrackerRootfs());
                }
                if (override.timeoutMs() != null && override.timeoutMs() > 0) {
                    details.setDefaultTimeoutMs(override.timeoutMs());
                }
                if (override.memoryLimitMb() != null && override.memoryLimitMb() > 0) {
                    details.setDefaultMemoryLimitMb(override.memoryLimitMb());
                }
                log.info("Applied property overrides for language: {}", id);
            }
        });
    }

    public void register(LanguageDetails details) {
        registry.put(details.getId(), details);
    }

    public Optional<LanguageDetails> getLanguage(String id) {
        if (id == null)
            return Optional.empty();
        return Optional.ofNullable(registry.get(id));
    }

    public LanguageDetails getLanguageOrThrow(String id) {
        return getLanguage(id)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported language identifier: " + id));
    }

    public Collection<LanguageDetails> getAllLanguages() {
        return Collections.unmodifiableCollection(registry.values());
    }

    public List<LanguageDetails> getByCategory(ProblemCategory category) {
        return registry.values().stream()
                .filter(lang -> lang.getCategory() == category)
                .toList();
    }
}
