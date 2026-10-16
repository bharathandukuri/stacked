package com.bharath.stacked.modules.execution.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExecutionProperties Unit Tests")
class ExecutionPropertiesTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Defaults are populated with expected safe values")
    void defaultValuesAreValid() {
        ExecutionProperties properties = new ExecutionProperties();

        assertThat(properties.getDefaults().getTimeLimitMs()).isEqualTo(2000L);
        assertThat(properties.getDefaults().getMemoryLimitKb()).isEqualTo(262144L);
        assertThat(properties.getDefaults().getProcessLimit()).isEqualTo(50);
        assertThat(properties.getDefaults().getFileSizeKb()).isEqualTo(10240L);
        assertThat(properties.getDefaults().getOutputLimitBytes()).isEqualTo(10485760L);

        assertThat(properties.getHardLimits().getMaxTimeLimitMs()).isEqualTo(60000L);
        assertThat(properties.getHardLimits().getMaxMemoryLimitKb()).isEqualTo(1048576L);
        assertThat(properties.getHardLimits().getMaxProcessLimit()).isEqualTo(100);
        assertThat(properties.getHardLimits().getMaxFileSizeKb()).isEqualTo(51200L);
        assertThat(properties.getHardLimits().getMaxOutputLimitBytes()).isEqualTo(52428800L);
        assertThat(properties.getHardLimits().getMaxBatchSize()).isEqualTo(50);
        assertThat(properties.getHardLimits().getMaxFileCount()).isEqualTo(20);
        assertThat(properties.getHardLimits().getMaxFileSizeBytes()).isEqualTo(10485760L);

        assertThat(properties.getDockerLimits().getContainerCpuLimit()).isEqualTo(1L);
        assertThat(properties.getDockerLimits().getContainerMemoryLimitKb()).isEqualTo(1048576L);
        assertThat(properties.getDockerLimits().getContainerPidsLimit()).isEqualTo(200L);

        assertThat(properties.getCompilation().getTimeoutMs()).isEqualTo(10000L);
        assertThat(properties.getCompilation().getMemoryLimitKb()).isEqualTo(524288L);

        assertThat(properties.getDatabase().getReadinessTimeoutMs()).isEqualTo(30000L);
        assertThat(properties.getDatabase().getReadinessPollIntervalMs()).isEqualTo(150L);
        assertThat(properties.getDatabase().getDefaultQueryTimeoutMs()).isEqualTo(5000L);

        assertThat(properties.getCleanup().getStaleRetentionMinutes()).isEqualTo(30L);
        assertThat(properties.getCleanup().getCron()).isEqualTo("0 */5 * * * *");

        assertThat(validator.validate(properties)).isEmpty();
    }

    @Test
    @DisplayName("Validation fails when negative or invalid values are set")
    void validationDetectsInvalidValues() {
        ExecutionProperties properties = new ExecutionProperties();
        properties.getDefaults().setTimeLimitMs(-1L);
        properties.getCleanup().setCron("   ");

        var violations = validator.validate(properties);
        assertThat(violations).hasSize(2);
    }
}

