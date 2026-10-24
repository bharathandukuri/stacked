package com.bharath.stacked.modules.question_bank;

import com.bharath.stacked.AbstractIntegrationTest;
import com.bharath.stacked.exception.BadRequestException;
import com.bharath.stacked.exception.ConflictException;
import com.bharath.stacked.modules.coding_problem.dto.request.CreateCodingProblemRequest;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemDifficulty;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemStatus;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import com.bharath.stacked.modules.coding_problem.model.CodingProblem;
import com.bharath.stacked.modules.coding_problem.repository.CodingProblemRepository;
import com.bharath.stacked.modules.coding_problem.service.CodingProblemService;
import com.bharath.stacked.modules.mcq.dto.request.CreateMcqRequest;
import com.bharath.stacked.modules.mcq.dto.request.McqOptionRequest;
import com.bharath.stacked.modules.mcq.dto.response.McqQuestionDetailResponse;
import com.bharath.stacked.modules.mcq.enums.McqDifficulty;
import com.bharath.stacked.modules.mcq.enums.McqStatus;
import com.bharath.stacked.modules.mcq.enums.McqType;
import com.bharath.stacked.modules.mcq.service.McqQuestionService;
import com.bharath.stacked.modules.question_bank.dto.CreateQuestionBankRequest;
import com.bharath.stacked.modules.question_bank.dto.QuestionBankDetailResponse;
import com.bharath.stacked.modules.question_bank.dto.QuestionBankItemRequest;
import com.bharath.stacked.modules.question_bank.dto.QuestionBankSummaryResponse;
import com.bharath.stacked.modules.question_bank.enums.BankQuestionType;
import com.bharath.stacked.modules.question_bank.enums.QuestionBankStatus;
import com.bharath.stacked.modules.question_bank.service.QuestionBankService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Question Bank Curriculum & Regression Integration Tests")
class QuestionBankCurriculumIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private QuestionBankService questionBankService;

    @Autowired
    private CodingProblemService codingProblemService;

    @Autowired
    private CodingProblemRepository codingProblemRepository;

    @Autowired
    private McqQuestionService mcqQuestionService;

    private final List<String> createdBankIds = new ArrayList<>();
    private final List<String> createdCodingIds = new ArrayList<>();
    private final List<String> createdMcqIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (String id : createdBankIds) {
            try {
                questionBankService.deleteQuestionBank(id);
            } catch (Exception ignored) {}
        }
        for (String id : createdCodingIds) {
            try {
                codingProblemRepository.deleteById(id);
            } catch (Exception ignored) {}
        }
        for (String id : createdMcqIds) {
            try {
                mcqQuestionService.deleteQuestion(id);
            } catch (Exception ignored) {}
        }
    }

    @Test
    @DisplayName("Question Bank rejects adding unpublished (DRAFT) MCQ or coding problem")
    void cannotAddUnpublishedQuestionToBank() {
        // 1. Create a DRAFT MCQ
        McqQuestionDetailResponse draftMcq = mcqQuestionService.createQuestion(new CreateMcqRequest(
                "Draft MCQ for Eligibility Test",
                "Testing eligibility rule",
                McqType.SINGLE_CHOICE,
                McqDifficulty.EASY,
                McqStatus.DRAFT,
                List.of("Programming Fundamentals"),
                List.of("test"),
                List.of(
                        new McqOptionRequest("opt-1", "Option A", "Desc A"),
                        new McqOptionRequest("opt-2", "Option B", "Desc B")
                ),
                List.of("opt-1"),
                "Explanation",
                true,
                List.of()
        ), "admin");
        createdMcqIds.add(draftMcq.id());

        // 2. Attempt to create Question Bank referencing draft MCQ
        CreateQuestionBankRequest request = CreateQuestionBankRequest.builder()
                .title("Bank with Draft MCQ")
                .description("Should fail")
                .category("Test")
                .status(QuestionBankStatus.DRAFT)
                .items(List.of(
                        QuestionBankItemRequest.builder()
                                .questionType(BankQuestionType.MCQ)
                                .questionId(draftMcq.id())
                                .order(1)
                                .build()
                ))
                .build();

        assertThatThrownBy(() -> questionBankService.createQuestionBank(request, "admin"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("is not published and cannot be added to a Question Bank");
    }

    @Test
    @DisplayName("Cannot publish an empty Question Bank")
    void cannotPublishEmptyQuestionBank() {
        CreateQuestionBankRequest emptyRequest = CreateQuestionBankRequest.builder()
                .title("Empty Publish Bank")
                .description("Empty bank")
                .category("Test")
                .status(QuestionBankStatus.PUBLISHED)
                .items(List.of())
                .build();

        assertThatThrownBy(() -> questionBankService.createQuestionBank(emptyRequest, "admin"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot publish an empty Question Bank");
    }

    @Test
    @DisplayName("Cannot delete MCQ question when in use by Question Bank")
    void cannotDeleteMcqWhenInUseByQuestionBank() {
        // Create published MCQ
        McqQuestionDetailResponse mcq = mcqQuestionService.createQuestion(new CreateMcqRequest(
                "MCQ in Active Bank",
                "Testing deletion constraints",
                McqType.BOOLEAN,
                McqDifficulty.EASY,
                McqStatus.PUBLISHED,
                List.of("Programming Fundamentals"),
                List.of("test"),
                List.of(
                        new McqOptionRequest("opt-t", "True", null),
                        new McqOptionRequest("opt-f", "False", null)
                ),
                List.of("opt-t"),
                "Explanation",
                true,
                List.of()
        ), "admin");
        createdMcqIds.add(mcq.id());

        QuestionBankSummaryResponse bank = questionBankService.createQuestionBank(CreateQuestionBankRequest.builder()
                .title("Bank Protecting MCQ From Deletion")
                .status(QuestionBankStatus.PUBLISHED)
                .items(List.of(
                        QuestionBankItemRequest.builder()
                                .questionType(BankQuestionType.MCQ)
                                .questionId(mcq.id())
                                .order(1)
                                .build()
                ))
                .build(), "admin");
        createdBankIds.add(bank.getId());

        // Attempt to delete MCQ -> must throw ConflictException
        assertThatThrownBy(() -> mcqQuestionService.deleteQuestion(mcq.id()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot delete MCQ question")
                .hasMessageContaining("Bank Protecting MCQ From Deletion");

        // Delete question bank first
        questionBankService.deleteQuestionBank(bank.getId());
        createdBankIds.remove(bank.getId());

        // Now delete MCQ succeeds
        mcqQuestionService.deleteQuestion(mcq.id());
        createdMcqIds.remove(mcq.id());
    }

    @Test
    @DisplayName("Cannot delete Coding Problem when in use by Question Bank")
    void cannotDeleteCodingProblemWhenInUseByQuestionBank() {
        CodingProblem codingProblem = CodingProblem.builder()
                .title("Coding Problem in Active Bank")
                .slug("coding-prob-active-bank")
                .statement("Solve this")
                .status(CodingProblemStatus.PUBLISHED)
                .difficulty(CodingProblemDifficulty.EASY)
                .problemType(CodingProblemType.GENERIC)
                .build();
        CodingProblem savedProblem = codingProblemRepository.save(codingProblem);
        createdCodingIds.add(savedProblem.getId());

        QuestionBankSummaryResponse bank = questionBankService.createQuestionBank(CreateQuestionBankRequest.builder()
                .title("Bank Protecting Coding Problem From Deletion")
                .status(QuestionBankStatus.PUBLISHED)
                .items(List.of(
                        QuestionBankItemRequest.builder()
                                .questionType(BankQuestionType.CODING)
                                .questionId(savedProblem.getId())
                                .order(1)
                                .build()
                ))
                .build(), "admin");
        createdBankIds.add(bank.getId());

        // Attempt to delete coding problem -> must throw ConflictException
        assertThatThrownBy(() -> codingProblemService.delete(savedProblem.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot delete coding problem")
                .hasMessageContaining("Bank Protecting Coding Problem From Deletion");

        // Delete question bank first
        questionBankService.deleteQuestionBank(bank.getId());
        createdBankIds.remove(bank.getId());

        // Now delete Coding Problem succeeds
        codingProblemService.delete(savedProblem.getId());
        createdCodingIds.remove(savedProblem.getId());
    }
}

