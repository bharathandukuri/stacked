package com.bharath.stacked.modules.execution.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "stacked.execution")
public class ExecutionProperties {

    @Valid
    @NotNull
    private Defaults defaults = new Defaults();

    @Valid
    @NotNull
    private HardLimits hardLimits = new HardLimits();

    @Valid
    @NotNull
    private DockerLimits dockerLimits = new DockerLimits();

    @Valid
    @NotNull
    private Compilation compilation = new Compilation();

    @Valid
    @NotNull
    private Database database = new Database();

    @Valid
    @NotNull
    private Cleanup cleanup = new Cleanup();

    @NotNull
    private Map<String, @Valid LanguageLimits> languages = new HashMap<>();

    public ExecutionProperties() {
        initDefaultLanguageLimits();
    }

    private void initDefaultLanguageLimits() {
        languages.put("java-21", new LanguageLimits(2000L, 262144L, 10000L, 192, null, null));
        languages.put("cpp-23", new LanguageLimits(1000L, 131072L, 10000L, null, null, null));
        languages.put("c-17", new LanguageLimits(1000L, 131072L, 10000L, null, null, null));
        languages.put("python-3_12", new LanguageLimits(3000L, 262144L, null, null, null, null));
        languages.put("javascript-node-20", new LanguageLimits(2000L, 262144L, null, null, 192, null));
        languages.put("sqlite-3", new LanguageLimits(null, 262144L, null, null, null, 5000L));
        languages.put("postgresql-16", new LanguageLimits(null, 524288L, null, null, null, 5000L));
        languages.put("mysql-8_0", new LanguageLimits(null, 524288L, null, null, null, 5000L));
        languages.put("mongodb-8_0", new LanguageLimits(null, 524288L, null, null, null, 5000L));
    }

    public LanguageLimits getLanguageLimits(String languageId) {
        if (languageId == null) {
            return null;
        }
        String rawKey = languageId.trim().toLowerCase();
        String underscoreKey = rawKey.replace('.', '_');
        if (languages.containsKey(underscoreKey)) {
            return languages.get(underscoreKey);
        }
        return languages.get(rawKey);
    }

    @Getter
    @Setter
    public static class LanguageLimits {
        @Positive
        private Long timeLimitMs;

        @Positive
        private Long memoryLimitKb;

        @Positive
        private Long compilationTimeoutMs;

        @Positive
        private Integer maxHeapMb;

        @Positive
        private Integer maxOldSpaceSizeMb;

        @Positive
        private Long queryTimeoutMs;

        public LanguageLimits() {}

        public LanguageLimits(Long timeLimitMs, Long memoryLimitKb, Long compilationTimeoutMs,
                              Integer maxHeapMb, Integer maxOldSpaceSizeMb, Long queryTimeoutMs) {
            this.timeLimitMs = timeLimitMs;
            this.memoryLimitKb = memoryLimitKb;
            this.compilationTimeoutMs = compilationTimeoutMs;
            this.maxHeapMb = maxHeapMb;
            this.maxOldSpaceSizeMb = maxOldSpaceSizeMb;
            this.queryTimeoutMs = queryTimeoutMs;
        }
    }

    @Getter
    @Setter
    public static class Defaults {
        @Positive
        private long timeLimitMs = 2000L;

        @Positive
        private long memoryLimitKb = 262144L; // 256 MB

        @Positive
        private int processLimit = 50;

        @Positive
        private long fileSizeKb = 10240L; // 10 MB

        @Positive
        private long outputLimitBytes = 10485760L; // 10 MB
    }

    @Getter
    @Setter
    public static class HardLimits {
        @Positive
        private long maxTimeLimitMs = 60000L; // 60s

        @Positive
        private long maxMemoryLimitKb = 1048576L; // 1 GB

        @Positive
        private int maxProcessLimit = 100;

        @Positive
        private long maxFileSizeKb = 51200L; // 50 MB

        @Positive
        private long maxOutputLimitBytes = 52428800L; // 50 MB

        @Positive
        private int maxBatchSize = 50;

        @Positive
        private int maxFileCount = 20;

        @Positive
        private long maxFileSizeBytes = 10485760L; // 10 MB
    }

    @Getter
    @Setter
    public static class DockerLimits {
        @Positive
        private long containerCpuLimit = 1L;

        @Positive
        private long containerMemoryLimitKb = 1048576L; // 1 GB

        @Positive
        private long containerPidsLimit = 200L;
    }

    @Getter
    @Setter
    public static class Compilation {
        @Positive
        private long timeoutMs = 10000L; // 10s

        @Positive
        private long memoryLimitKb = 524288L; // 512 MB
    }

    @Getter
    @Setter
    public static class Database {
        @Positive
        private long readinessTimeoutMs = 30000L; // 30s

        @Positive
        private long readinessPollIntervalMs = 150L;

        @Positive
        private long defaultQueryTimeoutMs = 5000L; // 5s
    }

    @Getter
    @Setter
    public static class Cleanup {
        @Positive
        private long staleRetentionMinutes = 30L;

        @NotBlank
        private String cron = "0 */5 * * * *";
    }
}

