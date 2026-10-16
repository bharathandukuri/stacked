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
     * item -> batch -> global defaults, clamped to hard limits.
     */
    public long resolveTimeLimitMs(CodeExecutionConstraints itemConstraints, CodeExecutionConstraints batchConstraints) {
        long requestedTimeMs;
        if (itemConstraints != null && itemConstraints.timeLimitMs() > 0) {
            requestedTimeMs = itemConstraints.timeLimitMs();
        } else if (batchConstraints != null && batchConstraints.timeLimitMs() > 0) {
            requestedTimeMs = batchConstraints.timeLimitMs();
        } else {
            requestedTimeMs = executionProperties.getDefaults().getTimeLimitMs();
        }

        long maxTimeMs = executionProperties.getHardLimits().getMaxTimeLimitMs();
        return Math.min(requestedTimeMs, maxTimeMs);
    }

    /**
     * Resolves the effective memory limit in KB using field-by-field precedence:
     * item -> batch -> global defaults, clamped to hard limits.
     */
    public long resolveMemoryLimitKb(CodeExecutionConstraints itemConstraints, CodeExecutionConstraints batchConstraints) {
        long requestedMemoryKb;
        if (itemConstraints != null && itemConstraints.memoryLimitKb() != null && itemConstraints.memoryLimitKb() > 0) {
            requestedMemoryKb = itemConstraints.memoryLimitKb();
        } else if (batchConstraints != null && batchConstraints.memoryLimitKb() != null && batchConstraints.memoryLimitKb() > 0) {
            requestedMemoryKb = batchConstraints.memoryLimitKb();
        } else {
            requestedMemoryKb = executionProperties.getDefaults().getMemoryLimitKb();
        }

        long maxMemoryKb = executionProperties.getHardLimits().getMaxMemoryLimitKb();
        return Math.min(requestedMemoryKb, maxMemoryKb);
    }

    /**
     * Resolves Isolate constraints for compiled and interpreted languages.
     */
    public IsolateExecutionConstraints resolveIsolateConstraints(
            Language language,
            CodeExecutionConstraints itemConstraints,
            CodeExecutionConstraints batchConstraints
    ) {
        long timeLimitMs = resolveTimeLimitMs(itemConstraints, batchConstraints);
        long memoryLimitKb = resolveMemoryLimitKb(itemConstraints, batchConstraints);

        double cpuTimeSeconds = timeLimitMs / 1000.0;
        double wallTimeSeconds;
        if ((itemConstraints == null || itemConstraints.timeLimitMs() <= 0)
                && (batchConstraints == null || batchConstraints.timeLimitMs() <= 0)) {
            wallTimeSeconds = 5.0;
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
     * Resolves database query execution timeout in milliseconds.
     * Note: Per-query memory is not independently limited by SQL/database engines;
     * protection relies on the outer Docker container resource limits.
     */
    public long resolveDatabaseTimeoutMs(CodeExecutionConstraints itemConstraints, CodeExecutionConstraints batchConstraints) {
        long timeLimitMs;
        if (itemConstraints != null && itemConstraints.timeLimitMs() > 0) {
            timeLimitMs = itemConstraints.timeLimitMs();
        } else if (batchConstraints != null && batchConstraints.timeLimitMs() > 0) {
            timeLimitMs = batchConstraints.timeLimitMs();
        } else {
            timeLimitMs = executionProperties.getDatabase().getDefaultQueryTimeoutMs();
        }
        return Math.min(timeLimitMs, executionProperties.getHardLimits().getMaxTimeLimitMs());
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
     * Creates authoritative outer Docker container limits for database containers.
     */
    public DockerContainerConstraints resolveDatabaseContainerConstraints() {
        return new DockerContainerConstraints(
                executionProperties.getDockerLimits().getContainerCpuLimit(),
                Math.min(executionProperties.getDefaults().getMemoryLimitKb(), executionProperties.getDockerLimits().getContainerMemoryLimitKb()),
                executionProperties.getDockerLimits().getContainerPidsLimit(),
                true
        );
    }

    public boolean isManagedRuntime(Language language) {
        if (language == null || language.id() == null) {
            return false;
        }
        String langId = language.id().toLowerCase();
        return MANAGED_RUNTIME_PREFIXES.stream().anyMatch(langId::contains);
    }
}

