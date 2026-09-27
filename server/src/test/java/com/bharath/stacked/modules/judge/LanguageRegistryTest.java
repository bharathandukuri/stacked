package com.bharath.stacked.modules.judge;

import com.bharath.stacked.modules.judge.config.JudgeProperties;
import com.bharath.stacked.modules.judge.enums.LanguageType;
import com.bharath.stacked.modules.judge.enums.ProblemCategory;
import com.bharath.stacked.modules.judge.enums.SandboxDriverType;
import com.bharath.stacked.modules.judge.languages.LanguageDetails;
import com.bharath.stacked.modules.judge.languages.LanguageRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LanguageRegistryTest {

    @Test
    @DisplayName("Java 21 and Java 17 should be registered as distinct language entities with versioned identifiers")
    void shouldRegisterJavaVersionsAsDistinctEntities() {
        JudgeProperties properties = new JudgeProperties(
                SandboxDriverType.DOCKER, 5000, 10000, 65536, "/tmp/test", null, null, null);
        LanguageRegistry registry = new LanguageRegistry(properties);

        Optional<LanguageDetails> java21Opt = registry.getLanguage("java-21");
        Optional<LanguageDetails> java17Opt = registry.getLanguage("java-17");

        assertThat(java21Opt).isPresent();
        assertThat(java17Opt).isPresent();

        LanguageDetails java21 = java21Opt.get();
        LanguageDetails java17 = java17Opt.get();

        // Must NOT be the same object or ID
        assertThat(java21.getId()).isEqualTo("java-21");
        assertThat(java17.getId()).isEqualTo("java-17");
        assertThat(java21.getId()).isNotEqualTo(java17.getId());

        // Images and RootFS must be distinct per version
        assertThat(java21.getDockerImage()).contains("21");
        assertThat(java17.getDockerImage()).contains("17");
        assertThat(java21.getFirecrackerRootfs()).contains("21");
        assertThat(java17.getFirecrackerRootfs()).contains("17");

        // Both are COMPILED and GENERIC
        assertThat(java21.getLanguageType()).isEqualTo(LanguageType.COMPILED);
        assertThat(java17.getLanguageType()).isEqualTo(LanguageType.COMPILED);
        assertThat(java21.getCategory()).isEqualTo(ProblemCategory.GENERIC);
    }

    @Test
    @DisplayName("Python 3.12 and PyPy should be registered as distinct language entities")
    void shouldRegisterPythonVariantsAsDistinctEntities() {
        JudgeProperties properties = new JudgeProperties(
                SandboxDriverType.DOCKER, 5000, 10000, 65536, "/tmp/test", null, null, null);
        LanguageRegistry registry = new LanguageRegistry(properties);

        Optional<LanguageDetails> cpythonOpt = registry.getLanguage("python-3.12");
        Optional<LanguageDetails> pypyOpt = registry.getLanguage("python-pypy");

        assertThat(cpythonOpt).isPresent();
        assertThat(pypyOpt).isPresent();

        LanguageDetails cpython = cpythonOpt.get();
        LanguageDetails pypy = pypyOpt.get();

        assertThat(cpython.getId()).isEqualTo("python-3.12");
        assertThat(pypy.getId()).isEqualTo("python-pypy");
        assertThat(cpython.getDockerImage()).contains("3.12");
        assertThat(pypy.getDockerImage()).contains("pypy");
    }

    @Test
    @DisplayName("Should apply per-language configuration overrides from JudgeProperties")
    void shouldApplyConfigurationOverrides() {
        JudgeProperties.LanguageOverrideProperties java21Override = new JudgeProperties.LanguageOverrideProperties(
                "custom-registry.internal/java:21.0.3",
                "custom-rootfs-java21.ext4",
                null,
                null,
                8000L,
                512);

        JudgeProperties properties = new JudgeProperties(
                SandboxDriverType.DOCKER, 5000, 10000, 65536, "/tmp/test", null, null,
                Map.of("java-21", java21Override));

        LanguageRegistry registry = new LanguageRegistry(properties);
        LanguageDetails java21 = registry.getLanguageOrThrow("java-21");

        assertThat(java21.getDockerImage()).isEqualTo("custom-registry.internal/java:21.0.3");
        assertThat(java21.getFirecrackerRootfs()).isEqualTo("custom-rootfs-java21.ext4");
        assertThat(java21.getDefaultTimeoutMs()).isEqualTo(8000L);
        assertThat(java21.getDefaultMemoryLimitMb()).isEqualTo(512);
    }
}
