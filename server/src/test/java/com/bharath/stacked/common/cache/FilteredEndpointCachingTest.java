package com.bharath.stacked.common.cache;

import com.bharath.stacked.modules.assessment.dto.AssessmentFilterRequest;
import com.bharath.stacked.modules.assessment.enums.AssessmentStatus;
import com.bharath.stacked.modules.coding_problem.dto.request.CodingProblemFilterRequest;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemDifficulty;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemStatus;
import com.bharath.stacked.modules.mcq.dto.request.McqFilterRequest;
import com.bharath.stacked.modules.mcq.enums.McqDifficulty;
import com.bharath.stacked.modules.mcq.enums.McqStatus;
import com.bharath.stacked.modules.question_bank.dto.QuestionBankFilterRequest;
import com.bharath.stacked.modules.question_bank.enums.QuestionBankStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FilteredEndpointCachingTest {

    @Nested
    @DisplayName("CodingProblemFilterRequest default filter detection")
    class CodingProblemFilters {

        @Test
        @DisplayName("Empty or default constructor evaluates to default")
        void emptyConstructorIsDefault() {
            CodingProblemFilterRequest filter = new CodingProblemFilterRequest();
            assertThat(filter.isDefault()).isTrue();
        }

        @Test
        @DisplayName("Explicit default values evaluate to default")
        void explicitDefaultsEvaluateToDefault() {
            CodingProblemFilterRequest filter = CodingProblemFilterRequest.builder()
                    .search(null)
                    .status(null)
                    .difficulty(null)
                    .page(0)
                    .size(20)
                    .sortBy("createdAt")
                    .sortDirection("DESC")
                    .build();
            assertThat(filter.isDefault()).isTrue();
        }

        @Test
        @DisplayName("Search term is non-default")
        void searchIsNonDefault() {
            CodingProblemFilterRequest filter = CodingProblemFilterRequest.builder()
                    .search("binary search")
                    .build();
            assertThat(filter.isDefault()).isFalse();
        }

        @Test
        @DisplayName("Status or difficulty filter is non-default")
        void statusOrDifficultyIsNonDefault() {
            assertThat(CodingProblemFilterRequest.builder().status(CodingProblemStatus.PUBLISHED).build().isDefault()).isFalse();
            assertThat(CodingProblemFilterRequest.builder().difficulty(CodingProblemDifficulty.HARD).build().isDefault()).isFalse();
        }

        @Test
        @DisplayName("Non-zero page or custom size is non-default")
        void paginationIsNonDefault() {
            assertThat(CodingProblemFilterRequest.builder().page(1).build().isDefault()).isFalse();
            assertThat(CodingProblemFilterRequest.builder().size(50).build().isDefault()).isFalse();
        }
    }

    @Nested
    @DisplayName("McqFilterRequest default filter detection")
    class McqFilters {

        @Test
        @DisplayName("Default McqFilterRequest evaluates to default")
        void defaultMcqFilter() {
            McqFilterRequest filter = new McqFilterRequest();
            assertThat(filter.isDefault()).isTrue();
        }

        @Test
        @DisplayName("Topic or difficulty filter is non-default")
        void topicOrDifficultyIsNonDefault() {
            assertThat(McqFilterRequest.builder().topic("Java Streams").build().isDefault()).isFalse();
            assertThat(McqFilterRequest.builder().difficulty(McqDifficulty.MEDIUM).build().isDefault()).isFalse();
            assertThat(McqFilterRequest.builder().status(McqStatus.DRAFT).build().isDefault()).isFalse();
            assertThat(McqFilterRequest.builder().page(2).build().isDefault()).isFalse();
        }
    }

    @Nested
    @DisplayName("QuestionBankFilterRequest default filter detection")
    class QuestionBankFilters {

        @Test
        @DisplayName("Default QuestionBankFilterRequest evaluates to default")
        void defaultQuestionBankFilter() {
            QuestionBankFilterRequest filter = new QuestionBankFilterRequest();
            assertThat(filter.isDefault()).isTrue();
        }

        @Test
        @DisplayName("Category, status, or search is non-default")
        void customFiltersAreNonDefault() {
            assertThat(QuestionBankFilterRequest.builder().category("Algorithms").build().isDefault()).isFalse();
            assertThat(QuestionBankFilterRequest.builder().status(QuestionBankStatus.PUBLISHED).build().isDefault()).isFalse();
            assertThat(QuestionBankFilterRequest.builder().search("senior").build().isDefault()).isFalse();
            assertThat(QuestionBankFilterRequest.builder().page(1).build().isDefault()).isFalse();
        }
    }

    @Nested
    @DisplayName("AssessmentFilterRequest default filter detection")
    class AssessmentFilters {

        @Test
        @DisplayName("Default AssessmentFilterRequest evaluates to default")
        void defaultAssessmentFilter() {
            AssessmentFilterRequest filter = new AssessmentFilterRequest();
            assertThat(filter.isDefault()).isTrue();
        }

        @Test
        @DisplayName("Status, search, or pagination is non-default")
        void customFiltersAreNonDefault() {
            assertThat(AssessmentFilterRequest.builder().status(AssessmentStatus.ACTIVE).build().isDefault()).isFalse();
            assertThat(AssessmentFilterRequest.builder().search("intern").build().isDefault()).isFalse();
            assertThat(AssessmentFilterRequest.builder().page(1).build().isDefault()).isFalse();
            assertThat(AssessmentFilterRequest.builder().size(25).build().isDefault()).isFalse();
        }
    }
}

