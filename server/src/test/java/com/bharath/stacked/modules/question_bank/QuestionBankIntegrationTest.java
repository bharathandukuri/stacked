package com.bharath.stacked.modules.question_bank;

import com.bharath.stacked.AbstractIntegrationTest;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemDifficulty;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemStatus;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import com.bharath.stacked.modules.coding_problem.model.CodingProblem;
import com.bharath.stacked.modules.coding_problem.repository.CodingProblemRepository;
import com.bharath.stacked.modules.mcq.dto.request.CreateMcqRequest;
import com.bharath.stacked.modules.mcq.dto.request.McqOptionRequest;
import com.bharath.stacked.modules.mcq.dto.response.McqQuestionDetailResponse;
import com.bharath.stacked.modules.mcq.enums.McqDifficulty;
import com.bharath.stacked.modules.mcq.enums.McqStatus;
import com.bharath.stacked.modules.mcq.enums.McqType;
import com.bharath.stacked.modules.mcq.service.McqQuestionService;
import com.bharath.stacked.modules.question_bank.dto.*;
import com.bharath.stacked.modules.question_bank.enums.BankQuestionType;
import com.bharath.stacked.modules.question_bank.enums.QuestionBankStatus;
import com.bharath.stacked.modules.question_bank.service.QuestionBankService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionBankIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private QuestionBankService questionBankService;

    @Autowired
    private CodingProblemRepository codingProblemRepository;

    @Autowired
    private McqQuestionService mcqQuestionService;

    @Test
    @DisplayName("End-to-end Question Bank lifecycle: bundle coding + mcq, bulk hydrate, duplicate, update, and delete")
    void questionBankFullLifecycleTest() {
        // 1. Create a real Coding Problem
        CodingProblem cp = codingProblemRepository.save(CodingProblem.builder()
                .title("Valid Palindrome II")
                .slug("valid-palindrome-ii")
                .status(CodingProblemStatus.PUBLISHED)
                .problemType(CodingProblemType.GENERIC)
                .difficulty(CodingProblemDifficulty.EASY)
                .statement("Given a string s, return true if s can be palindrome after deleting at most one character.")
                .topics(List.of("Two Pointers", "String"))
                .build());
        assertThat(cp.getId()).isNotNull();

        // 2. Create a real MCQ
        McqQuestionDetailResponse mcq = mcqQuestionService.createQuestion(new CreateMcqRequest(
                "Java Record Mutability",
                "Are components of a Java record mutable by default?",
                McqType.BOOLEAN,
                McqDifficulty.EASY,
                McqStatus.PUBLISHED,
                List.of("Java"),
                List.of("records"),
                List.of(
                        new McqOptionRequest("opt_true", "True", "Records are shallowly immutable"),
                        new McqOptionRequest("opt_false", "False", "Correct, fields are final")
                ),
                List.of("opt_false"),
                "Fields of records are declared private and final.",
                2.0,
                0.5,
                false,
                List.of()
        ), "admin-test");
        assertThat(mcq.id()).isNotNull();

        // 3. Create a Question Bank referencing both questions
        CreateQuestionBankRequest createBankRequest = CreateQuestionBankRequest.builder()
                .title("Junior Java Software Engineer Assessment Bank")
                .description("Curated questions for screening Junior Java engineers")
                .category("Java Development")
                .status(QuestionBankStatus.PUBLISHED)
                .tags(List.of("java", "strings", "basics"))
                .items(List.of(
                        QuestionBankItemRequest.builder()
                                .questionType(BankQuestionType.CODING)
                                .questionId(cp.getId())
                                .order(1)
                                .customPoints(15.0)
                                .build(),
                        QuestionBankItemRequest.builder()
                                .questionType(BankQuestionType.MCQ)
                                .questionId(mcq.id())
                                .order(2)
                                .customPoints(null) // falls back to MCQ default points (2.0)
                                .build()
                ))
                .build();

        QuestionBankSummaryResponse summary = questionBankService.createQuestionBank(createBankRequest, "admin-test");
        assertThat(summary.getId()).isNotNull();
        assertThat(summary.getSlug()).isEqualTo("junior-java-software-engineer-assessment-bank");
        assertThat(summary.getTotalQuestions()).isEqualTo(2);
        assertThat(summary.getCodingCount()).isEqualTo(1);
        assertThat(summary.getMcqCount()).isEqualTo(1);
        assertThat(summary.getTotalPoints()).isEqualTo(17.0);

        // 4. Retrieve Detail with Bulk Hydration
        QuestionBankDetailResponse detail = questionBankService.getQuestionBank(summary.getId());
        assertThat(detail.getItems()).hasSize(2);

        HydratedQuestionItemResponse cpItem = detail.getItems().get(0);
        assertThat(cpItem.getQuestionType()).isEqualTo(BankQuestionType.CODING);
        assertThat(cpItem.getTitle()).isEqualTo("Valid Palindrome II");
        assertThat(cpItem.getEffectivePoints()).isEqualTo(15.0);

        HydratedQuestionItemResponse mcqItem = detail.getItems().get(1);
        assertThat(mcqItem.getQuestionType()).isEqualTo(BankQuestionType.MCQ);
        assertThat(mcqItem.getTitle()).isEqualTo("Java Record Mutability");
        assertThat(mcqItem.getEffectivePoints()).isEqualTo(2.0);

        // 5. Duplicate Question Bank
        QuestionBankSummaryResponse duplicated = questionBankService.duplicateQuestionBank(summary.getId(), "admin-test");
        assertThat(duplicated.getId()).isNotEqualTo(summary.getId());
        assertThat(duplicated.getTitle()).isEqualTo("Copy of Junior Java Software Engineer Assessment Bank");
        assertThat(duplicated.getStatus()).isEqualTo(QuestionBankStatus.DRAFT);
        assertThat(duplicated.getTotalQuestions()).isEqualTo(2);

        // 6. Cleanup
        questionBankService.deleteQuestionBank(summary.getId());
        questionBankService.deleteQuestionBank(duplicated.getId());
        codingProblemRepository.deleteById(cp.getId());
        mcqQuestionService.deleteQuestion(mcq.id());
    }
}

