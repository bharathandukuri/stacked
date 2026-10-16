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
        assertThat(constraints.memoryLimitKb()).isEqualTo(Math.min(properties.getDefaults().getMemoryLimitKb(), properties.getDockerLimits().getContainerMemoryLimitKb()));
        assertThat(constraints.pidsLimit()).isEqualTo(properties.getDockerLimits().getContainerPidsLimit());
        assertThat(constraints.networkDisabled()).isTrue();
    }
}

