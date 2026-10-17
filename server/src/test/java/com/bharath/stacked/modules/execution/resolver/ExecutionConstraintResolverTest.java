package com.bharath.stacked.modules.execution.resolver;

import com.bharath.stacked.modules.execution.config.ExecutionProperties;
import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.modules.execution.dto.DockerContainerConstraints;
import com.bharath.stacked.modules.execution.dto.IsolateExecutionConstraints;
import com.bharath.stacked.modules.language.Language;
import com.bharath.stacked.modules.language.factory.impl.LanguageFactoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExecutionConstraintResolver Unit Tests")
class ExecutionConstraintResolverTest {

    private ExecutionConstraintResolver resolver;
    private ExecutionProperties properties;
    private final LanguageFactoryImpl languageFactory = new LanguageFactoryImpl();

    @BeforeEach
    void setUp() {
        properties = new ExecutionProperties();
        resolver = new ExecutionConstraintResolver(properties);
    }

    @Test
    @DisplayName("Layered resolution: item takes highest precedence")
    void itemTakesHighestPrecedence() {
        CodeExecutionConstraints item = new CodeExecutionConstraints(1500L, 128000L);
        CodeExecutionConstraints batch = new CodeExecutionConstraints(3000L, 512000L);

        long timeMs = resolver.resolveTimeLimitMs(item, batch);
        long memKb = resolver.resolveMemoryLimitKb(item, batch);

        assertThat(timeMs).isEqualTo(1500L);
        assertThat(memKb).isEqualTo(128000L);
    }

    @Test
    @DisplayName("Field-by-field fallback: item time + batch memory")
    void fieldByFieldFallbackItemTimeBatchMemory() {
        CodeExecutionConstraints item = new CodeExecutionConstraints(1200L, null); // Memory unspecified in item
        CodeExecutionConstraints batch = new CodeExecutionConstraints(3000L, 512000L);

        long timeMs = resolver.resolveTimeLimitMs(item, batch);
        long memKb = resolver.resolveMemoryLimitKb(item, batch);

        assertThat(timeMs).isEqualTo(1200L);
        assertThat(memKb).isEqualTo(512000L);
    }

    @Test
    @DisplayName("Field-by-field fallback: item time + global default memory when batch memory is null")
    void fieldByFieldFallbackToGlobalDefaults() {
        CodeExecutionConstraints item = new CodeExecutionConstraints(1800L, null);
        CodeExecutionConstraints batch = new CodeExecutionConstraints(null != null ? 1 : 0, null);

        long timeMs = resolver.resolveTimeLimitMs(item, batch);
        long memKb = resolver.resolveMemoryLimitKb(item, batch);

        assertThat(timeMs).isEqualTo(1800L);
        assertThat(memKb).isEqualTo(properties.getDefaults().getMemoryLimitKb()); // 262144L
    }

    @Test
    @DisplayName("Global defaults apply when item and batch are completely null")
    void globalDefaultsApplyWhenBothNull() {
        long timeMs = resolver.resolveTimeLimitMs(null, null);
        long memKb = resolver.resolveMemoryLimitKb(null, null);

        assertThat(timeMs).isEqualTo(properties.getDefaults().getTimeLimitMs()); // 5000L
        assertThat(memKb).isEqualTo(properties.getDefaults().getMemoryLimitKb()); // 262144L
    }

    @Test
    @DisplayName("Hard upper bounds clamp excessively large requests")
    void hardUpperBoundsClampExcessiveLimits() {
        CodeExecutionConstraints item = new CodeExecutionConstraints(99999999L, 99999999L);

        long timeMs = resolver.resolveTimeLimitMs(item, null);
        long memKb = resolver.resolveMemoryLimitKb(item, null);

        assertThat(timeMs).isEqualTo(properties.getHardLimits().getMaxTimeLimitMs()); // 60000L
        assertThat(memKb).isEqualTo(properties.getHardLimits().getMaxMemoryLimitKb()); // 1048576L
    }

    @Test
    @DisplayName("Managed runtimes (JVM) omit Isolate --mem constraint to prevent virtual memory crash")
    void jvmOmitsIsolateMemConstraint() {
        Language java = languageFactory.create("java-21");
        CodeExecutionConstraints item = new CodeExecutionConstraints(2000L, 256000L);

        IsolateExecutionConstraints isolate = resolver.resolveIsolateConstraints(java, item, null);

        assertThat(isolate.cpuTimeSeconds()).isEqualTo(2.0);
        assertThat(isolate.wallTimeSeconds()).isEqualTo(4.0);
        assertThat(isolate.memoryKb()).isNull(); // JVM must not use RLIMIT_AS
    }

    @Test
    @DisplayName("Native languages (C++) apply Isolate --mem constraint")
    void nativeLanguagesApplyIsolateMemConstraint() {
        Language cpp = languageFactory.create("cpp-23");
        CodeExecutionConstraints item = new CodeExecutionConstraints(2000L, 256000L);

        IsolateExecutionConstraints isolate = resolver.resolveIsolateConstraints(cpp, item, null);

        assertThat(isolate.cpuTimeSeconds()).isEqualTo(2.0);
        assertThat(isolate.memoryKb()).isEqualTo(256000L);
    }

    @Test
    @DisplayName("Database timeout resolves properly and clamps to max limit")
    void databaseTimeoutResolvesAndClamps() {
        CodeExecutionConstraints item = new CodeExecutionConstraints(3000L);
        long timeoutMs = resolver.resolveDatabaseTimeoutMs(item, null);
        assertThat(timeoutMs).isEqualTo(3000L);

        CodeExecutionConstraints excessive = new CodeExecutionConstraints(999999L);
        long clampedTimeout = resolver.resolveDatabaseTimeoutMs(excessive, null);
        assertThat(clampedTimeout).isEqualTo(properties.getHardLimits().getMaxTimeLimitMs());
    }

    @Test
    @DisplayName("Outer Docker container constraints provide authoritative boundaries")
    void dockerContainerConstraintsProvideAuthoritativeBoundaries() {
        DockerContainerConstraints constraints = resolver.resolveDockerContainerConstraints();

        assertThat(constraints.cpuLimit()).isEqualTo(properties.getDockerLimits().getContainerCpuLimit());
        assertThat(constraints.memoryLimitKb()).isEqualTo(properties.getDockerLimits().getContainerMemoryLimitKb());
        assertThat(constraints.pidsLimit()).isEqualTo(properties.getDockerLimits().getContainerPidsLimit());
        assertThat(constraints.networkDisabled()).isTrue();
    }

    @Test
    @DisplayName("Database container constraints provide authoritative container boundaries")
    void databaseContainerConstraintsProvideAuthoritativeBoundaries() {
        DockerContainerConstraints constraints = resolver.resolveDatabaseContainerConstraints();

        assertThat(constraints.cpuLimit()).isEqualTo(properties.getDockerLimits().getContainerCpuLimit());
        assertThat(constraints.memoryLimitKb()).isEqualTo(properties.getDockerLimits().getContainerMemoryLimitKb());
        assertThat(constraints.pidsLimit()).isEqualTo(properties.getDockerLimits().getContainerPidsLimit());
        assertThat(constraints.networkDisabled()).isTrue();
    }

    @Test
    @DisplayName("Resolves language-specific defaults when constraints are null")
    void resolvesLanguageSpecificDefaults() {
        Language python = languageFactory.create("python-3.12");
        Language cpp = languageFactory.create("cpp-23");

        long pythonTime = resolver.resolveTimeLimitMs(python, null, null);
        long pythonMem = resolver.resolveMemoryLimitKb(python, null, null);
        assertThat(pythonTime).isEqualTo(3000L);
        assertThat(pythonMem).isEqualTo(262144L);

        long cppTime = resolver.resolveTimeLimitMs(cpp, null, null);
        long cppMem = resolver.resolveMemoryLimitKb(cpp, null, null);
        assertThat(cppTime).isEqualTo(1000L);
        assertThat(cppMem).isEqualTo(131072L);
    }

    @Test
    @DisplayName("Resolves JVM heap limits and scales proportionally to memory constraint")
    void resolvesJvmMaxHeap() {
        Language java = languageFactory.create("java-21");

        // Base configured default
        int defaultHeap = resolver.resolveJvmMaxHeapMb(java, null);
        assertThat(defaultHeap).isEqualTo(192);

        // Constrained memory 64MB -> 48MB heap
        int scaledHeap = resolver.resolveJvmMaxHeapMb(java, 65536L);
        assertThat(scaledHeap).isEqualTo(48);

        // Very small memory -> minimum 16MB
        int minHeap = resolver.resolveJvmMaxHeapMb(java, 10240L);
        assertThat(minHeap).isEqualTo(16);
    }

    @Test
    @DisplayName("Resolves Node.js V8 max-old-space-size limits")
    void resolvesNodeMaxOldSpaceSize() {
        Language node = languageFactory.create("javascript-node-20");

        int defaultHeap = resolver.resolveNodeMaxOldSpaceSizeMb(node, null);
        assertThat(defaultHeap).isEqualTo(192);

        int scaledHeap = resolver.resolveNodeMaxOldSpaceSizeMb(node, 65536L);
        assertThat(scaledHeap).isEqualTo(48);
    }

    @Test
    @DisplayName("Respects explicitly configured problem limits and applies defaults only for missing values")
    void respectsExplicitProblemLimitsAndAppliesDefaultsOnlyForMissing() {
        Language cpp = languageFactory.create("cpp-23");
        Language java = languageFactory.create("java-21");

        // Problem specifies time limit 1500ms, memory is unspecified -> time is 1500ms, memory falls back to language default
        CodeExecutionConstraints timeOnly = new CodeExecutionConstraints(1500L, null);
        assertThat(resolver.resolveTimeLimitMs(cpp, timeOnly, null)).isEqualTo(1500L);
        assertThat(resolver.resolveMemoryLimitKb(cpp, timeOnly, null)).isEqualTo(131072L); // C++ default 128MB
        assertThat(resolver.resolveTimeLimitMs(java, timeOnly, null)).isEqualTo(1500L);
        assertThat(resolver.resolveMemoryLimitKb(java, timeOnly, null)).isEqualTo(262144L); // Java default 256MB

        // Problem specifies memory limit 512000KB, time is unspecified (0) -> memory is 512000KB, time falls back to language default
        CodeExecutionConstraints memOnly = new CodeExecutionConstraints(0L, 512000L);
        assertThat(resolver.resolveTimeLimitMs(cpp, memOnly, null)).isEqualTo(1000L); // C++ default 1000ms
        assertThat(resolver.resolveMemoryLimitKb(cpp, memOnly, null)).isEqualTo(512000L);
        assertThat(resolver.resolveTimeLimitMs(java, memOnly, null)).isEqualTo(2000L); // Java default 2000ms
        assertThat(resolver.resolveMemoryLimitKb(java, memOnly, null)).isEqualTo(512000L);
    }

    @Test
    @DisplayName("Resolves default time and memory for all 9 supported languages when constraints are null")
    void resolvesAllSupportedLanguageDefaults() {
        // C-17: 1000ms, 128MB
        Language c = languageFactory.create("c-17");
        assertThat(resolver.resolveTimeLimitMs(c, null, null)).isEqualTo(1000L);
        assertThat(resolver.resolveMemoryLimitKb(c, null, null)).isEqualTo(131072L);

        // CPP-23: 1000ms, 128MB
        Language cpp = languageFactory.create("cpp-23");
        assertThat(resolver.resolveTimeLimitMs(cpp, null, null)).isEqualTo(1000L);
        assertThat(resolver.resolveMemoryLimitKb(cpp, null, null)).isEqualTo(131072L);

        // Java-21: 2000ms, 256MB
        Language java = languageFactory.create("java-21");
        assertThat(resolver.resolveTimeLimitMs(java, null, null)).isEqualTo(2000L);
        assertThat(resolver.resolveMemoryLimitKb(java, null, null)).isEqualTo(262144L);

        // Python-3.12: 3000ms, 256MB
        Language python = languageFactory.create("python-3.12");
        assertThat(resolver.resolveTimeLimitMs(python, null, null)).isEqualTo(3000L);
        assertThat(resolver.resolveMemoryLimitKb(python, null, null)).isEqualTo(262144L);

        // JavaScript Node-20: 2000ms, 256MB
        Language node = languageFactory.create("javascript-node-20");
        assertThat(resolver.resolveTimeLimitMs(node, null, null)).isEqualTo(2000L);
        assertThat(resolver.resolveMemoryLimitKb(node, null, null)).isEqualTo(262144L);

        // SQLite-3: 5000ms query timeout, 256MB container memory
        Language sqlite = languageFactory.create("sqlite-3");
        assertThat(resolver.resolveDatabaseTimeoutMs(sqlite, null, null)).isEqualTo(5000L);
        assertThat(resolver.resolveMemoryLimitKb(sqlite, null, null)).isEqualTo(262144L);
        assertThat(resolver.resolveDatabaseContainerConstraints(sqlite).memoryLimitKb()).isEqualTo(262144L);

        // PostgreSQL-16: 5000ms, 512MB
        Language postgres = languageFactory.create("postgresql-16");
        assertThat(resolver.resolveTimeLimitMs(postgres, null, null)).isEqualTo(5000L);
        assertThat(resolver.resolveDatabaseTimeoutMs(postgres, null, null)).isEqualTo(5000L);
        assertThat(resolver.resolveMemoryLimitKb(postgres, null, null)).isEqualTo(524288L);
        assertThat(resolver.resolveDatabaseContainerConstraints(postgres).memoryLimitKb()).isEqualTo(524288L);

        // MySQL-8.0: 5000ms, 512MB
        Language mysql = languageFactory.create("mysql-8.0");
        assertThat(resolver.resolveTimeLimitMs(mysql, null, null)).isEqualTo(5000L);
        assertThat(resolver.resolveDatabaseTimeoutMs(mysql, null, null)).isEqualTo(5000L);
        assertThat(resolver.resolveMemoryLimitKb(mysql, null, null)).isEqualTo(524288L);
        assertThat(resolver.resolveDatabaseContainerConstraints(mysql).memoryLimitKb()).isEqualTo(524288L);

        // MongoDB-8.0: 5000ms, 512MB
        Language mongo = languageFactory.create("mongodb-8.0");
        assertThat(resolver.resolveTimeLimitMs(mongo, null, null)).isEqualTo(5000L);
        assertThat(resolver.resolveDatabaseTimeoutMs(mongo, null, null)).isEqualTo(5000L);
        assertThat(resolver.resolveMemoryLimitKb(mongo, null, null)).isEqualTo(524288L);
        assertThat(resolver.resolveDatabaseContainerConstraints(mongo).memoryLimitKb()).isEqualTo(524288L);
    }

    @Test
    @DisplayName("Scales JVM heap and Node old-space up when problem setter specifies larger memory limit")
    void scalesHeapUpWhenMemoryLimitIsLarge() {
        Language java = languageFactory.create("java-21");
        Language node = languageFactory.create("javascript-node-20");

        // When problem allows 512MB, heap can scale up to 75% = 384MB
        int javaHeap512 = resolver.resolveJvmMaxHeapMb(java, 524288L);
        assertThat(javaHeap512).isEqualTo(384);

        int nodeOldSpace512 = resolver.resolveNodeMaxOldSpaceSizeMb(node, 524288L);
        assertThat(nodeOldSpace512).isEqualTo(384);
    }

    @Test
    @DisplayName("Resolves compilation timeout per language")
    void resolvesCompilationTimeout() {
        Language cpp = languageFactory.create("cpp-23");
        long cppCompileTimeout = resolver.resolveCompilationTimeoutMs(cpp);
        assertThat(cppCompileTimeout).isEqualTo(10000L);
    }
}

