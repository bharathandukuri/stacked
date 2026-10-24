package com.bharath.stacked.modules.question_bank.service.impl;

import com.bharath.stacked.exception.BadRequestException;
import com.bharath.stacked.exception.ConflictException;
import com.bharath.stacked.exception.ResourceNotFoundException;
import com.bharath.stacked.modules.assessment.model.Assessment;
import com.bharath.stacked.modules.assessment.repository.AssessmentRepository;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemDifficulty;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemStatus;
import com.bharath.stacked.modules.coding_problem.model.CodingProblem;
import com.bharath.stacked.modules.coding_problem.repository.CodingProblemRepository;
import com.bharath.stacked.modules.mcq.enums.McqStatus;
import com.bharath.stacked.modules.mcq.model.McqQuestion;
import com.bharath.stacked.modules.mcq.repository.McqQuestionRepository;
import com.bharath.stacked.modules.question_bank.dto.*;
import com.bharath.stacked.modules.question_bank.enums.BankQuestionType;
import com.bharath.stacked.modules.question_bank.enums.QuestionBankStatus;
import com.bharath.stacked.modules.question_bank.model.QuestionBank;
import com.bharath.stacked.modules.question_bank.model.QuestionBankItem;
import com.bharath.stacked.modules.question_bank.repository.QuestionBankRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionBankServiceImplTest {

    @Mock
    private QuestionBankRepository questionBankRepository;

    @Mock
    private CodingProblemRepository codingProblemRepository;

    @Mock
    private McqQuestionRepository mcqQuestionRepository;

    @Mock
    private AssessmentRepository assessmentRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private QuestionBankServiceImpl questionBankService;

    private CodingProblem sampleCodingProblem;
    private McqQuestion sampleMcqQuestion;
    private QuestionBank sampleQuestionBank;

    @BeforeEach
    void setUp() {
        sampleCodingProblem = CodingProblem.builder()
                .id("cp-101")
                .title("Two Sum")
                .slug("two-sum")
                .status(CodingProblemStatus.PUBLISHED)
                .difficulty(CodingProblemDifficulty.EASY)
                .statement("Find two numbers that add up to target.")
                .topics(List.of("Arrays", "Hash Table"))
                .build();

        sampleMcqQuestion = McqQuestion.builder()
                .id("mcq-201")
                .title("Java Volatile Visibility")
                .status(McqStatus.PUBLISHED)
                .content("What does volatile keyword guarantee?")
                .topics(List.of("Java", "Concurrency"))
                .build();

        sampleQuestionBank = QuestionBank.builder()
                .id("qb-301")
                .title("Backend Java Assessment Bank")
                .description("Curated questions for Java assessments")
                .category("Backend Engineering")
                .status(QuestionBankStatus.DRAFT)
                .tags(List.of("java", "concurrency", "algorithms"))
                .items(List.of(
                        QuestionBankItem.builder()
                                .questionType(BankQuestionType.CODING)
                                .questionId("cp-101")
                                .order(1)
                                .customPoints(15.0)
                                .build(),
                        QuestionBankItem.builder()
                                .questionType(BankQuestionType.MCQ)
                                .questionId("mcq-201")
                                .order(2)
                                .customPoints(2.0)
                                .build()
                ))
                .totalQuestions(2)
                .codingCount(1)
                .mcqCount(1)
                .totalPoints(17.0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("createQuestionBank should calculate stats, generate slug and persist")
    void createQuestionBank_Success() {
        CreateQuestionBankRequest request = CreateQuestionBankRequest.builder()
                .title("Backend Java Assessment Bank")
                .description("Curated questions for Java assessments")
                .category("Backend Engineering")
                .status(QuestionBankStatus.PUBLISHED)
                .tags(List.of("java", "concurrency"))
                .items(List.of(
                        QuestionBankItemRequest.builder()
                                .questionType(BankQuestionType.CODING)
                                .questionId("cp-101")
                                .order(1)
                                .customPoints(15.0)
                                .build(),
                        QuestionBankItemRequest.builder()
                                .questionType(BankQuestionType.MCQ)
                                .questionId("mcq-201")
                                .order(2)
                                .customPoints(2.0)
                                .build()
                ))
                .build();

        when(codingProblemRepository.findAllById(List.of("cp-101"))).thenReturn(List.of(sampleCodingProblem));
        when(mcqQuestionRepository.findAllById(List.of("mcq-201"))).thenReturn(List.of(sampleMcqQuestion));
        when(questionBankRepository.save(any(QuestionBank.class))).thenAnswer(invocation -> {
            QuestionBank bank = invocation.getArgument(0);
            bank.setId("qb-301");
            return bank;
        });

        QuestionBankSummaryResponse response = questionBankService.createQuestionBank(request, "admin-1");

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Backend Java Assessment Bank");
        assertThat(response.getTotalQuestions()).isEqualTo(2);
        assertThat(response.getCodingCount()).isEqualTo(1);
        assertThat(response.getMcqCount()).isEqualTo(1);
        assertThat(response.getTotalPoints()).isEqualTo(17.0);
    }

    @Test
    @DisplayName("createQuestionBank should throw ResourceNotFoundException when referenced item does not exist")
    void createQuestionBank_WhenQuestionNotFound_ThrowsException() {
        CreateQuestionBankRequest request = CreateQuestionBankRequest.builder()
                .title("Invalid Bank")
                .items(List.of(
                        QuestionBankItemRequest.builder()
                                .questionType(BankQuestionType.CODING)
                                .questionId("non-existing-id")
                                .build()
                ))
                .build();

        when(codingProblemRepository.findAllById(List.of("non-existing-id"))).thenReturn(List.of());

        assertThatThrownBy(() -> questionBankService.createQuestionBank(request, "admin-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Coding Problem not found with ID");
    }

    @Test
    @DisplayName("getQuestionBank should bulk-hydrate items with question details")
    void getQuestionBank_Success() {
        when(questionBankRepository.findById("qb-301")).thenReturn(Optional.of(sampleQuestionBank));
        when(codingProblemRepository.findAllById(List.of("cp-101"))).thenReturn(List.of(sampleCodingProblem));
        when(mcqQuestionRepository.findAllById(List.of("mcq-201"))).thenReturn(List.of(sampleMcqQuestion));

        QuestionBankDetailResponse response = questionBankService.getQuestionBank("qb-301");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("qb-301");
        assertThat(response.getItems()).hasSize(2);

        HydratedQuestionItemResponse cpItem = response.getItems().get(0);
        assertThat(cpItem.getQuestionType()).isEqualTo(BankQuestionType.CODING);
        assertThat(cpItem.getTitle()).isEqualTo("Two Sum");
        assertThat(cpItem.getEffectivePoints()).isEqualTo(15.0);

        HydratedQuestionItemResponse mcqItem = response.getItems().get(1);
        assertThat(mcqItem.getQuestionType()).isEqualTo(BankQuestionType.MCQ);
        assertThat(mcqItem.getTitle()).isEqualTo("Java Volatile Visibility");
        assertThat(mcqItem.getEffectivePoints()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("updateQuestionBank should update details and recalculate totals")
    void updateQuestionBank_Success() {
        UpdateQuestionBankRequest request = UpdateQuestionBankRequest.builder()
                .title("Updated Bank Title")
                .description("New Description")
                .category("Full Stack")
                .status(QuestionBankStatus.PUBLISHED)
                .items(List.of(
                        QuestionBankItemRequest.builder()
                                .questionType(BankQuestionType.MCQ)
                                .questionId("mcq-201")
                                .customPoints(5.0)
                                .build()
                ))
                .build();

        when(questionBankRepository.findById("qb-301")).thenReturn(Optional.of(sampleQuestionBank));
        when(mcqQuestionRepository.findAllById(List.of("mcq-201"))).thenReturn(List.of(sampleMcqQuestion));
        when(questionBankRepository.save(any(QuestionBank.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuestionBankSummaryResponse response = questionBankService.updateQuestionBank("qb-301", request);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Updated Bank Title");
        assertThat(response.getTotalQuestions()).isEqualTo(1);
        assertThat(response.getMcqCount()).isEqualTo(1);
        assertThat(response.getCodingCount()).isEqualTo(0);
        assertThat(response.getTotalPoints()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("duplicateQuestionBank should clone existing bank with copy prefix and draft status")
    void duplicateQuestionBank_Success() {
        when(questionBankRepository.findById("qb-301")).thenReturn(Optional.of(sampleQuestionBank));
        when(questionBankRepository.save(any(QuestionBank.class))).thenAnswer(invocation -> {
            QuestionBank bank = invocation.getArgument(0);
            bank.setId("qb-clone-999");
            return bank;
        });

        QuestionBankSummaryResponse response = questionBankService.duplicateQuestionBank("qb-301", "admin-1");

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Copy of Backend Java Assessment Bank");
        assertThat(response.getStatus()).isEqualTo(QuestionBankStatus.DRAFT);
        assertThat(response.getTotalQuestions()).isEqualTo(2);
    }

    @Test
    @DisplayName("deleteQuestionBank should delete by id or slug")
    void deleteQuestionBank_Success() {
        when(questionBankRepository.findById("qb-301")).thenReturn(Optional.of(sampleQuestionBank));

        questionBankService.deleteQuestionBank("qb-301");

        verify(questionBankRepository).delete(sampleQuestionBank);
    }

    @Test
    @DisplayName("updateQuestionBank should throw ConflictException when bank is PUBLISHED")
    void updateQuestionBank_WhenPublished_ThrowsConflictException() {
        QuestionBank publishedBank = QuestionBank.builder()
                .id("qb-published")
                .title("Published Bank")
                .status(QuestionBankStatus.PUBLISHED)
                .items(List.of())
                .build();
        when(questionBankRepository.findById("qb-published")).thenReturn(Optional.of(publishedBank));

        UpdateQuestionBankRequest req = UpdateQuestionBankRequest.builder().title("New Title").build();

        assertThatThrownBy(() -> questionBankService.updateQuestionBank("qb-published", req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot edit Question Bank 'Published Bank' because it is currently PUBLISHED. Unpublish to DRAFT first.");
    }

    @Test
    @DisplayName("unpublish should throw ConflictException when bank is in use by assessment")
    void unpublish_WhenInUse_ThrowsConflictException() {
        QuestionBank publishedBank = QuestionBank.builder()
                .id("qb-301")
                .title("Backend Java Assessment Bank")
                .status(QuestionBankStatus.PUBLISHED)
                .items(List.of(
                        QuestionBankItem.builder().questionType(BankQuestionType.CODING).questionId("cp-101").order(1).build()
                ))
                .build();
        when(questionBankRepository.findById("qb-301")).thenReturn(Optional.of(publishedBank));
        when(assessmentRepository.existsByQuestionBankId("qb-301")).thenReturn(true);
        when(assessmentRepository.findAllByQuestionBankId("qb-301")).thenReturn(List.of(
                Assessment.builder().id("asm-1").title("Fullstack Developer Assessment").build()
        ));

        assertThatThrownBy(() -> questionBankService.unpublish("qb-301"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot move question bank 'Backend Java Assessment Bank' back to draft because it is currently in use");
    }

    @Test
    @DisplayName("isInUse should return true when question bank is used by an assessment")
    void isInUse_ReturnsTrue_WhenAssessmentReferencesBank() {
        when(assessmentRepository.existsByQuestionBankId("qb-301")).thenReturn(true);

        assertThat(questionBankService.isInUse("qb-301")).isTrue();
    }

    @Test
    @DisplayName("deleteQuestionBank should throw ConflictException when bank is in use by assessment")
    void deleteQuestionBank_ThrowsConflictException_WhenInUse() {
        when(questionBankRepository.findById("qb-301")).thenReturn(Optional.of(sampleQuestionBank));
        when(assessmentRepository.existsByQuestionBankId("qb-301")).thenReturn(true);
        when(assessmentRepository.findAllByQuestionBankId("qb-301")).thenReturn(List.of(
                Assessment.builder().id("asm-1").title("Fullstack Developer Assessment").build()
        ));

        assertThatThrownBy(() -> questionBankService.deleteQuestionBank("qb-301"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot delete question bank 'Backend Java Assessment Bank'")
                .hasMessageContaining("Fullstack Developer Assessment");

        verify(questionBankRepository, never()).delete(sampleQuestionBank);
    }

    @Test
    @DisplayName("getStats should aggregate total banks, status counts, and categories")
    void getStats_Success() {
        when(questionBankRepository.count()).thenReturn(10L);
        when(questionBankRepository.countByStatus(QuestionBankStatus.PUBLISHED)).thenReturn(7L);
        when(questionBankRepository.countByStatus(QuestionBankStatus.DRAFT)).thenReturn(3L);
        when(questionBankRepository.findAll()).thenReturn(List.of(sampleQuestionBank));

        QuestionBankStatsResponse stats = questionBankService.getStats();

        assertThat(stats.getTotalBanks()).isEqualTo(10L);
        assertThat(stats.getPublishedBanks()).isEqualTo(7L);
        assertThat(stats.getDraftBanks()).isEqualTo(3L);
        assertThat(stats.getTotalCuratedQuestions()).isEqualTo(2L);
        assertThat(stats.getTotalCategories()).isEqualTo(1L);
    }

    @Test
    @DisplayName("createQuestionBank should throw BadRequestException when referenced coding problem is not PUBLISHED")
    void createQuestionBank_WhenCodingProblemNotPublished_ThrowsBadRequest() {
        CodingProblem draftProblem = CodingProblem.builder()
                .id("cp-draft")
                .title("Draft Problem")
                .status(CodingProblemStatus.UNPUBLISHED)
                .build();

        CreateQuestionBankRequest request = CreateQuestionBankRequest.builder()
                .title("Bank with Draft")
                .items(List.of(
                        QuestionBankItemRequest.builder()
                                .questionType(BankQuestionType.CODING)
                                .questionId("cp-draft")
                                .build()
                ))
                .build();

        when(codingProblemRepository.findAllById(List.of("cp-draft"))).thenReturn(List.of(draftProblem));

        assertThatThrownBy(() -> questionBankService.createQuestionBank(request, "admin-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("is not published and cannot be added to a Question Bank");
    }

    @Test
    @DisplayName("createQuestionBank should throw BadRequestException when referenced MCQ is not PUBLISHED")
    void createQuestionBank_WhenMcqNotPublished_ThrowsBadRequest() {
        McqQuestion draftMcq = McqQuestion.builder()
                .id("mcq-draft")
                .title("Draft MCQ")
                .status(McqStatus.DRAFT)
                .build();

        CreateQuestionBankRequest request = CreateQuestionBankRequest.builder()
                .title("Bank with Draft MCQ")
                .items(List.of(
                        QuestionBankItemRequest.builder()
                                .questionType(BankQuestionType.MCQ)
                                .questionId("mcq-draft")
                                .build()
                ))
                .build();

        when(mcqQuestionRepository.findAllById(List.of("mcq-draft"))).thenReturn(List.of(draftMcq));

        assertThatThrownBy(() -> questionBankService.createQuestionBank(request, "admin-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("is not published and cannot be added to a Question Bank");
    }

    @Test
    @DisplayName("createQuestionBank should throw BadRequestException when published without questions")
    void createQuestionBank_WhenPublishedWithoutQuestions_ThrowsBadRequest() {
        CreateQuestionBankRequest request = CreateQuestionBankRequest.builder()
                .title("Empty Published Bank")
                .status(QuestionBankStatus.PUBLISHED)
                .items(List.of())
                .build();

        assertThatThrownBy(() -> questionBankService.createQuestionBank(request, "admin-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot publish an empty Question Bank");
    }

    @Test
    @DisplayName("updateStatus should throw BadRequestException when publishing an empty bank")
    void updateStatus_WhenPublishingEmptyBank_ThrowsBadRequest() {
        QuestionBank emptyBank = QuestionBank.builder()
                .id("qb-empty")
                .title("Empty Bank")
                .status(QuestionBankStatus.DRAFT)
                .items(List.of())
                .build();

        when(questionBankRepository.findById("qb-empty")).thenReturn(Optional.of(emptyBank));

        assertThatThrownBy(() -> questionBankService.updateStatus("qb-empty", QuestionBankStatus.PUBLISHED))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot publish an empty Question Bank");
    }

    @Test
    @DisplayName("updateStatus should throw BadRequestException when publishing bank whose curated question became unpublished")
    void updateStatus_WhenCuratedQuestionBecameUnpublished_ThrowsBadRequest() {
        QuestionBank draftBank = QuestionBank.builder()
                .id("qb-draft")
                .title("Draft Bank")
                .status(QuestionBankStatus.DRAFT)
                .items(List.of(
                        QuestionBankItem.builder()
                                .questionType(BankQuestionType.CODING)
                                .questionId("cp-101")
                                .order(1)
                                .build()
                ))
                .build();

        CodingProblem unpublishedProblem = CodingProblem.builder()
                .id("cp-101")
                .title("Two Sum")
                .status(CodingProblemStatus.UNPUBLISHED)
                .build();

        when(questionBankRepository.findById("qb-draft")).thenReturn(Optional.of(draftBank));
        when(codingProblemRepository.findAllById(List.of("cp-101"))).thenReturn(List.of(unpublishedProblem));

        assertThatThrownBy(() -> questionBankService.updateStatus("qb-draft", QuestionBankStatus.PUBLISHED))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("is not published. All questions must be published to publish this Question Bank.");
    }

    @Test
    @DisplayName("updateStatus, publish, unpublish should execute valid lifecycle transitions")
    void updateStatus_ValidTransitions_Success() {
        QuestionBank draftBank = QuestionBank.builder()
                .id("qb-valid")
                .title("Valid Bank")
                .status(QuestionBankStatus.DRAFT)
                .items(List.of(
                        QuestionBankItem.builder()
                                .questionType(BankQuestionType.CODING)
                                .questionId("cp-101")
                                .order(1)
                                .build()
                ))
                .build();

        when(questionBankRepository.findById("qb-valid")).thenReturn(Optional.of(draftBank));
        when(codingProblemRepository.findAllById(List.of("cp-101"))).thenReturn(List.of(sampleCodingProblem));
        when(questionBankRepository.save(any(QuestionBank.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 1. Publish
        QuestionBankSummaryResponse pubRes = questionBankService.publish("qb-valid");
        assertThat(pubRes.getStatus()).isEqualTo(QuestionBankStatus.PUBLISHED);

        // 2. Unpublish back to DRAFT
        QuestionBankSummaryResponse unpubRes = questionBankService.unpublish("qb-valid");
        assertThat(unpubRes.getStatus()).isEqualTo(QuestionBankStatus.DRAFT);
    }

    @Test
    @DisplayName("removeQuestionFromAllBanks should remove deleted question and recalculate metrics")
    void shouldRemoveQuestionFromAllBanksAndRecalculateMetrics() {
        QuestionBank bank = QuestionBank.builder()
                .id("qb-multi")
                .title("Multi-question bank")
                .status(QuestionBankStatus.PUBLISHED)
                .items(new ArrayList<>(List.of(
                        QuestionBankItem.builder().questionType(BankQuestionType.CODING).questionId("cp-101").order(1).build(),
                        QuestionBankItem.builder().questionType(BankQuestionType.MCQ).questionId("mcq-201").order(2).build()
                )))
                .build();

        when(questionBankRepository.findAllByItemsQuestionId("cp-101")).thenReturn(List.of(bank));
        when(mcqQuestionRepository.findAllById(anyList())).thenReturn(List.of(sampleMcqQuestion));
        when(questionBankRepository.save(any(QuestionBank.class))).thenAnswer(inv -> inv.getArgument(0));

        questionBankService.removeQuestionFromAllBanks("cp-101");

        assertThat(bank.getItems()).hasSize(1);
        assertThat(bank.getItems().get(0).getQuestionId()).isEqualTo("mcq-201");
        assertThat(bank.getCodingCount()).isEqualTo(0);
        assertThat(bank.getMcqCount()).isEqualTo(1);
        assertThat(bank.getStatus()).isEqualTo(QuestionBankStatus.PUBLISHED);
        verify(questionBankRepository).save(bank);
    }

    @Test
    @DisplayName("removeQuestionFromAllBanks should fallback to DRAFT when all questions removed from bank")
    void shouldFallbackToDraftWhenBankBecomesEmpty() {
        QuestionBank bank = QuestionBank.builder()
                .id("qb-single")
                .title("Single-question bank")
                .status(QuestionBankStatus.PUBLISHED)
                .items(new ArrayList<>(List.of(
                        QuestionBankItem.builder().questionType(BankQuestionType.CODING).questionId("cp-101").order(1).build()
                )))
                .build();

        when(questionBankRepository.findAllByItemsQuestionId("cp-101")).thenReturn(List.of(bank));
        when(questionBankRepository.save(any(QuestionBank.class))).thenAnswer(inv -> inv.getArgument(0));

        questionBankService.removeQuestionFromAllBanks("cp-101");

        assertThat(bank.getItems()).isEmpty();
        assertThat(bank.getStatus()).isEqualTo(QuestionBankStatus.DRAFT);
        assertThat(bank.getCodingCount()).isEqualTo(0);
        assertThat(bank.getMcqCount()).isEqualTo(0);
        verify(questionBankRepository).save(bank);
    }
}

