package com.bharath.stacked.modules.execution.resolver;

import com.bharath.stacked.modules.execution.config.ExecutionProperties;
import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.modules.execution.dto.DockerContainerConstraints;
import com.bharath.stacked.modules.execution.dto.IsolateExecutionConstraints;
import com.bharath.stacked.modules.language.Language;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Resolves execution constraints field-by-field with strict layering:
 * Execution-level (item) -> Batch-level (request) -> Platform Global Defaults.
 * Clamps all requested values against platform hard upper bounds.
 */
@Component
public class ExecutionConstraintResolver {

    private static final Set<String> MANAGED_RUNTIME_PREFIXES = Set.of(
            "java", "node", "javascript", "kotlin", "typescript", "go"
    );

    private final ExecutionProperties executionProperties;

    public ExecutionConstraintResolver(ExecutionProperties executionProperties) {
        this.executionProperties = executionProperties;
    }

    /**
     * Resolves the effective time limit in milliseconds using field-by-field precedence:
     * item -> batch -> language-specific defaults -> global platform defaults, clamped to hard limits.
     */
    public long resolveTimeLimitMs(Language language, CodeExecutionConstraints itemConstraints, CodeExecutionConstraints batchConstraints) {
        long requestedTimeMs;
        String langKey = (language != null && language.id() != null) ? language.id().trim().toLowerCase() : null;
        if (itemConstraints != null && itemConstraints.timeLimitMs() > 0) {
            requestedTimeMs = itemConstraints.timeLimitMs();
        } else if (batchConstraints != null && batchConstraints.timeLimitMs() > 0) {
            requestedTimeMs = batchConstraints.timeLimitMs();
        } else if (langKey != null
                && executionProperties.getLanguages().containsKey(langKey)
                && executionProperties.getLanguages().get(langKey).getTimeLimitMs() != null) {
            requestedTimeMs = executionProperties.getLanguages().get(langKey).getTimeLimitMs();
        } else if (langKey != null
                && executionProperties.getLanguages().containsKey(langKey)
                && executionProperties.getLanguages().get(langKey).getQueryTimeoutMs() != null) {
            requestedTimeMs = executionProperties.getLanguages().get(langKey).getQueryTimeoutMs();
        } else {
            requestedTimeMs = executionProperties.getDefaults().getTimeLimitMs();
        }

        long maxTimeMs = executionProperties.getHardLimits().getMaxTimeLimitMs();
        return Math.min(requestedTimeMs, maxTimeMs);
    }

    public long resolveTimeLimitMs(CodeExecutionConstraints itemConstraints, CodeExecutionConstraints batchConstraints) {
        return resolveTimeLimitMs(null, itemConstraints, batchConstraints);
    }

    /**
     * Resolves the effective memory limit in KB using field-by-field precedence:
     * item -> batch -> language-specific defaults -> global platform defaults, clamped to hard limits.
     */
    public long resolveMemoryLimitKb(Language language, CodeExecutionConstraints itemConstraints, CodeExecutionConstraints batchConstraints) {
        long requestedMemoryKb;
        String langKey = (language != null && language.id() != null) ? language.id().trim().toLowerCase() : null;
        if (itemConstraints != null && itemConstraints.memoryLimitKb() != null && itemConstraints.memoryLimitKb() > 0) {
            requestedMemoryKb = itemConstraints.memoryLimitKb();
        } else if (batchConstraints != null && batchConstraints.memoryLimitKb() != null && batchConstraints.memoryLimitKb() > 0) {
            requestedMemoryKb = batchConstraints.memoryLimitKb();
        } else if (langKey != null
                && executionProperties.getLanguages().containsKey(langKey)
                && executionProperties.getLanguages().get(langKey).getMemoryLimitKb() != null) {
            requestedMemoryKb = executionProperties.getLanguages().get(langKey).getMemoryLimitKb();
        } else {
            requestedMemoryKb = executionProperties.getDefaults().getMemoryLimitKb();
        }

        long maxMemoryKb = executionProperties.getHardLimits().getMaxMemoryLimitKb();
        return Math.min(requestedMemoryKb, maxMemoryKb);
    }

    public long resolveMemoryLimitKb(CodeExecutionConstraints itemConstraints, CodeExecutionConstraints batchConstraints) {
        return resolveMemoryLimitKb(null, itemConstraints, batchConstraints);
    }

    /**
     * Resolves Isolate constraints for compiled and interpreted languages.
     */
    public IsolateExecutionConstraints resolveIsolateConstraints(
            Language language,
            CodeExecutionConstraints itemConstraints,
            CodeExecutionConstraints batchConstraints
    ) {
        long timeLimitMs = resolveTimeLimitMs(language, itemConstraints, batchConstraints);
        long memoryLimitKb = resolveMemoryLimitKb(language, itemConstraints, batchConstraints);

        double cpuTimeSeconds = timeLimitMs / 1000.0;
        double wallTimeSeconds;
        if ((itemConstraints == null || itemConstraints.timeLimitMs() <= 0)
                && (batchConstraints == null || batchConstraints.timeLimitMs() <= 0)) {
            wallTimeSeconds = Math.max(5.0, cpuTimeSeconds + 2.0);
        } else {
            wallTimeSeconds = Math.max(cpuTimeSeconds * 2.0, cpuTimeSeconds + 1.0);
        }

        int processLimit = Math.min(
                executionProperties.getDefaults().getProcessLimit(),
                executionProperties.getHardLimits().getMaxProcessLimit()
        );

        long fileSizeKb = Math.min(
                executionProperties.getDefaults().getFileSizeKb(),
                executionProperties.getHardLimits().getMaxFileSizeKb()
        );

        // Managed runtimes (JVM, V8, Go) pre-allocate large virtual address spaces
        // for JIT code caches, GC card tables, and pointer compression.
        // Applying RLIMIT_AS (--mem) starves virtual address space and causes initialization crashes.
        // Container-level Docker memory remains the authoritative outer boundary.
        Long memoryKbForIsolate = isManagedRuntime(language) ? null : memoryLimitKb;

        return new IsolateExecutionConstraints(
                cpuTimeSeconds,
                wallTimeSeconds,
                memoryKbForIsolate,
                processLimit,
                fileSizeKb
        );
    }

    /**
     * Resolves compilation timeout in milliseconds for the given language.
     */
    public long resolveCompilationTimeoutMs(Language language) {
        String langKey = (language != null && language.id() != null) ? language.id().trim().toLowerCase() : null;
        if (langKey != null
                && executionProperties.getLanguages().containsKey(langKey)
                && executionProperties.getLanguages().get(langKey).getCompilationTimeoutMs() != null) {
            return executionProperties.getLanguages().get(langKey).getCompilationTimeoutMs();
        }
        return executionProperties.getCompilation().getTimeoutMs();
    }

    /**
     * Resolves JVM heap limit in MB (-Xmx) for Java executions.
     */
    public int resolveJvmMaxHeapMb(Language language, Long effectiveMemoryKb) {
        String langKey = (language != null && language.id() != null) ? language.id().trim().toLowerCase() : null;
        int baseHeapMb = 192;
        if (langKey != null
                && executionProperties.getLanguages().containsKey(langKey)
                && executionProperties.getLanguages().get(langKey).getMaxHeapMb() != null) {
            baseHeapMb = executionProperties.getLanguages().get(langKey).getMaxHeapMb();
        }
        if (effectiveMemoryKb != null && effectiveMemoryKb > 0) {
            int memoryMb = (int) (effectiveMemoryKb / 1024L);
            int proportionalHeapMb = (int) (memoryMb * 0.75); // 75% for heap, 25% for metaspace/threads
            int maxPlatformHeapMb = (int) (executionProperties.getHardLimits().getMaxMemoryLimitKb() / 1024L * 0.75);
            return Math.max(16, Math.min(proportionalHeapMb, maxPlatformHeapMb));
        }
        return baseHeapMb;
    }

    /**
     * Resolves Node.js V8 max-old-space-size in MB for JavaScript executions.
     */
    public int resolveNodeMaxOldSpaceSizeMb(Language language, Long effectiveMemoryKb) {
        String langKey = (language != null && language.id() != null) ? language.id().trim().toLowerCase() : null;
        int baseHeapMb = 192;
        if (langKey != null
                && executionProperties.getLanguages().containsKey(langKey)
                && executionProperties.getLanguages().get(langKey).getMaxOldSpaceSizeMb() != null) {
            baseHeapMb = executionProperties.getLanguages().get(langKey).getMaxOldSpaceSizeMb();
        }
        if (effectiveMemoryKb != null && effectiveMemoryKb > 0) {
            int memoryMb = (int) (effectiveMemoryKb / 1024L);
            int proportionalHeapMb = (int) (memoryMb * 0.75);
            int maxPlatformHeapMb = (int) (executionProperties.getHardLimits().getMaxMemoryLimitKb() / 1024L * 0.75);
            return Math.max(16, Math.min(proportionalHeapMb, maxPlatformHeapMb));
        }
        return baseHeapMb;
    }

    /**
     * Resolves database query execution timeout in milliseconds.
     * Note: Per-query memory is not independently limited by SQL/database engines;
     * protection relies on the outer Docker container resource limits.
     */
    public long resolveDatabaseTimeoutMs(Language language, CodeExecutionConstraints itemConstraints, CodeExecutionConstraints batchConstraints) {
        long timeLimitMs;
        if (itemConstraints != null && itemConstraints.timeLimitMs() > 0) {
            timeLimitMs = itemConstraints.timeLimitMs();
        } else if (batchConstraints != null && batchConstraints.timeLimitMs() > 0) {
            timeLimitMs = batchConstraints.timeLimitMs();
        } else {
            String langKey = (language != null && language.id() != null) ? language.id().trim().toLowerCase() : null;
            if (langKey != null
                    && executionProperties.getLanguages().containsKey(langKey)
                    && executionProperties.getLanguages().get(langKey).getQueryTimeoutMs() != null) {
                timeLimitMs = executionProperties.getLanguages().get(langKey).getQueryTimeoutMs();
            } else if (langKey != null
                    && executionProperties.getLanguages().containsKey(langKey)
                    && executionProperties.getLanguages().get(langKey).getTimeLimitMs() != null) {
                timeLimitMs = executionProperties.getLanguages().get(langKey).getTimeLimitMs();
            } else {
                timeLimitMs = executionProperties.getDatabase().getDefaultQueryTimeoutMs();
            }
        }
        return Math.min(timeLimitMs, executionProperties.getHardLimits().getMaxTimeLimitMs());
    }

    public long resolveDatabaseTimeoutMs(CodeExecutionConstraints itemConstraints, CodeExecutionConstraints batchConstraints) {
        return resolveDatabaseTimeoutMs(null, itemConstraints, batchConstraints);
    }

    /**
     * Creates authoritative outer Docker container limits for execution sandboxes.
     */
    public DockerContainerConstraints resolveDockerContainerConstraints() {
        return new DockerContainerConstraints(
                executionProperties.getDockerLimits().getContainerCpuLimit(),
                executionProperties.getDockerLimits().getContainerMemoryLimitKb(),
                executionProperties.getDockerLimits().getContainerPidsLimit(),
                true
        );
    }

    /**
     * Creates authoritative outer Docker container limits for database containers,
     * taking into account language-specific memory configuration (e.g. 512MB for MongoDB/Postgres/MySQL).
     */
    public DockerContainerConstraints resolveDatabaseContainerConstraints(Language language) {
        String langKey = (language != null && language.id() != null) ? language.id().trim().toLowerCase() : null;
        long memoryKb;
        if (langKey != null
                && executionProperties.getLanguages().containsKey(langKey)
                && executionProperties.getLanguages().get(langKey).getMemoryLimitKb() != null) {
            memoryKb = executionProperties.getLanguages().get(langKey).getMemoryLimitKb();
        } else {
            memoryKb = executionProperties.getDockerLimits().getContainerMemoryLimitKb();
        }
        memoryKb = Math.min(memoryKb, executionProperties.getDockerLimits().getContainerMemoryLimitKb());
        return new DockerContainerConstraints(
                executionProperties.getDockerLimits().getContainerCpuLimit(),
                memoryKb,
                executionProperties.getDockerLimits().getContainerPidsLimit(),
                true
        );
    }

    public DockerContainerConstraints resolveDatabaseContainerConstraints() {
        return resolveDatabaseContainerConstraints(null);
    }

    public boolean isManagedRuntime(Language language) {
        if (language == null || language.id() == null) {
            return false;
        }
        String langId = language.id().toLowerCase();
        return MANAGED_RUNTIME_PREFIXES.stream().anyMatch(langId::contains);
    }
}

