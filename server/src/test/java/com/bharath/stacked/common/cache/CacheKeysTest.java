package com.bharath.stacked.common.cache;

import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CacheKeysTest {

    @Test
    @DisplayName("Analytics keys are correctly namespaced and formatted")
    void analyticsKeysFormat() {
        assertThat(CacheKeys.analyticsOverview()).isEqualTo("cache:analytics:overview");
        assertThat(CacheKeys.analyticsContentDefault()).isEqualTo("cache:analytics:content:default");
        assertThat(CacheKeys.analyticsAssessmentsDefault()).isEqualTo("cache:analytics:assessments:default");
        assertThat(CacheKeys.analyticsSubmissionsDefault()).isEqualTo("cache:analytics:submissions:default");
    }

    @Test
    @DisplayName("Coding problem keys are normalized and deterministic")
    void codingProblemKeys() {
        assertThat(CacheKeys.codingProblemDefaultPage()).isEqualTo("cache:coding-problem:default-page");
        assertThat(CacheKeys.codingProblemDetail("Two-Sum")).isEqualTo("cache:coding-problem:detail:two-sum");
        assertThat(CacheKeys.codingProblemDetail("  60d5ec49f1a2  ")).isEqualTo("cache:coding-problem:detail:60d5ec49f1a2");
        assertThat(CacheKeys.codingProblemSupportedLanguages(null)).isEqualTo("cache:coding-problem:languages:all");
        assertThat(CacheKeys.codingProblemSupportedLanguages(CodingProblemType.DATABASE)).isEqualTo("cache:coding-problem:languages:database");
        assertThat(CacheKeys.codingProblemSupportedLanguages(CodingProblemType.GENERIC)).isEqualTo("cache:coding-problem:languages:generic");
    }

    @Test
    @DisplayName("MCQ keys are correctly formatted")
    void mcqKeys() {
        assertThat(CacheKeys.mcqDefaultPage()).isEqualTo("cache:mcq:default-page");
        assertThat(CacheKeys.mcqDetail("MCQ-123")).isEqualTo("cache:mcq:detail:mcq-123");
        assertThat(CacheKeys.mcqStats()).isEqualTo("cache:mcq:stats");
        assertThat(CacheKeys.mcqTopics()).isEqualTo("cache:mcq:topics");
    }

    @Test
    @DisplayName("Question bank keys are correctly formatted")
    void questionBankKeys() {
        assertThat(CacheKeys.questionBankDefaultPage()).isEqualTo("cache:question-bank:default-page");
        assertThat(CacheKeys.questionBankDetail("QB-456")).isEqualTo("cache:question-bank:detail:qb-456");
        assertThat(CacheKeys.questionBankCategories()).isEqualTo("cache:question-bank:categories");
        assertThat(CacheKeys.questionBankStats()).isEqualTo("cache:question-bank:stats");
    }

    @Test
    @DisplayName("Assessment keys are correctly formatted")
    void assessmentKeys() {
        assertThat(CacheKeys.assessmentDefaultPage()).isEqualTo("cache:assessment:default-page");
        assertThat(CacheKeys.assessmentDetail("Spring-Boot-Quiz")).isEqualTo("cache:assessment:detail:spring-boot-quiz");
        assertThat(CacheKeys.assessmentMetrics("ASM-789")).isEqualTo("cache:assessment:metrics:asm-789");
        assertThat(CacheKeys.assessmentBankPreview("QB-101")).isEqualTo("cache:assessment:bank-preview:qb-101");
    }

    @Test
    @DisplayName("Blank or null identifiers throw IllegalArgumentException")
    void invalidIdentifiersThrow() {
        assertThatThrownBy(() -> CacheKeys.codingProblemDetail(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CacheKeys.codingProblemDetail("   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CacheKeys.mcqDetail(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

