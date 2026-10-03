package com.bharath.stacked.modules.execution.mapper;

import com.bharath.stacked.modules.execution.enums.CodeExecutionStatus;
import com.bharath.stacked.modules.execution.enums.IsolateExecutionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CodeExecutionStatusMapper Unit Tests")
class CodeExecutionStatusMapperTest {

    private CodeExecutionStatusMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CodeExecutionStatusMapper();
    }

    @Test
    @DisplayName("Should map SUCCESS to SUCCESS")
    void shouldMapSuccess() {
        assertThat(mapper.map(IsolateExecutionStatus.SUCCESS))
                .isEqualTo(CodeExecutionStatus.SUCCESS);
    }

    @Test
    @DisplayName("Should map TIME_LIMIT_EXCEEDED to TIME_LIMIT_EXCEEDED")
    void shouldMapTimeLimitExceeded() {
        assertThat(mapper.map(IsolateExecutionStatus.TIME_LIMIT_EXCEEDED))
                .isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("Should map MEMORY_LIMIT_EXCEEDED to MEMORY_LIMIT_EXCEEDED")
    void shouldMapMemoryLimitExceeded() {
        assertThat(mapper.map(IsolateExecutionStatus.MEMORY_LIMIT_EXCEEDED))
                .isEqualTo(CodeExecutionStatus.MEMORY_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("Should map RUNTIME_ERROR to RUNTIME_ERROR")
    void shouldMapRuntimeError() {
        assertThat(mapper.map(IsolateExecutionStatus.RUNTIME_ERROR))
                .isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
    }

    @Test
    @DisplayName("Should map SYSTEM_ERROR to SYSTEM_ERROR")
    void shouldMapSystemError() {
        assertThat(mapper.map(IsolateExecutionStatus.SYSTEM_ERROR))
                .isEqualTo(CodeExecutionStatus.SYSTEM_ERROR);
    }

    @Test
    @DisplayName("Should return SYSTEM_ERROR when status is null")
    void shouldReturnSystemErrorWhenStatusIsNull() {
        assertThat(mapper.map(null))
                .isEqualTo(CodeExecutionStatus.SYSTEM_ERROR);
    }
}
