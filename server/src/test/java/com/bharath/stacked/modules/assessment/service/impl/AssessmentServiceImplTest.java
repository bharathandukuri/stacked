package com.bharath.stacked.modules.assessment.service.impl;

import com.bharath.stacked.exception.BadRequestException;
import com.bharath.stacked.modules.assessment.dto.*;
import com.bharath.stacked.modules.assessment.enums.AssessmentSelectionMode;
import com.bharath.stacked.modules.assessment.enums.AssessmentStatus;
import com.bharath.stacked.modules.assessment.enums.AttemptStatus;
import com.bharath.stacked.modules.assessment.enums.CandidateParticipationStatus;
import com.bharath.stacked.modules.assessment.model.*;
import com.bharath.stacked.modules.assessment.repository.*;
import com.bharath.stacked.modules.assessment.websocket.AssessmentWebSocketHandler;
import com.bharath.stacked.modules.coding_problem.dto.response.CodingProblemExecutionResponse;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemDifficulty;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemStatus;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import com.bharath.stacked.modules.coding_problem.judge.CodingProblemJudgeService;
import com.bharath.stacked.modules.coding_problem.model.CodingProblem;
import com.bharath.stacked.modules.coding_problem.model.TestCase;
import com.bharath.stacked.modules.coding_problem.repository.CodingProblemRepository;
import com.bharath.stacked.modules.execution.resolver.ExecutionConstraintResolver;
import com.bharath.stacked.modules.language.Language;
import com.bharath.stacked.modules.language.LanguageType;
import com.bharath.stacked.modules.language.registry.LanguageRegistry;
import com.bharath.stacked.modules.mcq.enums.McqStatus;
import com.bharath.stacked.modules.mcq.enums.McqType;
import com.bharath.stacked.modules.mcq.model.McqOption;
import com.bharath.stacked.modules.mcq.model.McqQuestion;
import com.bharath.stacked.modules.mcq.repository.McqQuestionRepository;
import com.bharath.stacked.modules.question_bank.enums.BankQuestionType;
import com.bharath.stacked.modules.question_bank.enums.QuestionBankStatus;
import com.bharath.stacked.modules.question_bank.model.QuestionBank;
import com.bharath.stacked.modules.question_bank.model.QuestionBankItem;
import com.bharath.stacked.modules.question_bank.repository.QuestionBankRepository;
import com.bharath.stacked.modules.security.model.User;
import com.bharath.stacked.modules.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceImplTest {

    @Mock
    private AssessmentRepository assessmentRepository;
    @Mock
    private AssessmentAssignmentRepository assignmentRepository;
    @Mock
    private AssessmentAttemptRepository attemptRepository;
    @Mock
    private AssessmentQuestionResponseRepository responseRepository;
    @Mock
    private AssessmentConnectionEventRepository connectionEventRepository;
    @Mock
    private QuestionBankRepository questionBankRepository;
    @Mock
    private CodingProblemRepository codingProblemRepository;
    @Mock
    private McqQuestionRepository mcqQuestionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CodingProblemJudgeService judgeService;
    @Mock
    private LanguageRegistry languageRegistry;
    @Mock
    private ExecutionConstraintResolver executionConstraintResolver;
    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private AssessmentAuditEventRepository auditEventRepository;
    @Mock
    private AssessmentWebSocketHandler webSocketHandler;

    @InjectMocks
    private AssessmentServiceImpl assessmentService;

    private McqQuestion sampleMcq;
    private CodingProblem sampleCoding;
    private QuestionBank sampleBank;

    @BeforeEach
    void setUp() {
        sampleMcq = McqQuestion.builder()
                .id("mcq-101")
                .title("Java Basics")
                .content("What is byte code?")
                .type(McqType.SINGLE_CHOICE)
                .status(McqStatus.PUBLISHED)
                .options(List.of(
                        new McqOption("opt-1", "Intermediate language", null),
                        new McqOption("opt-2", "Machine code", null)
                ))
                .correctOptionIds(List.of("opt-1"))
                .build();

        sampleCoding = CodingProblem.builder()
                .id("cp-201")
                .slug("reverse-string")
                .title("Reverse String")
                .statement("Reverse given string")
                .status(CodingProblemStatus.PUBLISHED)
                .difficulty(CodingProblemDifficulty.EASY)
                .problemType(CodingProblemType.GENERIC)
                .supportedLanguageIds(List.of("java-21"))
                .referenceSolutionLanguage("java-21")
                .referenceSolution("public String reverse(String s) { return s; }")
                .sampleTestCases(List.of(new TestCase("tc-1", "hello", null, "olleh")))
                .build();

        sampleBank = QuestionBank.builder()
                .id("qb-1")
                .title("Java Assessment Bank")
                .status(QuestionBankStatus.PUBLISHED)
                .items(List.of(
                        QuestionBankItem.builder().questionType(BankQuestionType.MCQ).questionId("mcq-101").order(1).customPoints(5.0).build(),
                        QuestionBankItem.builder().questionType(BankQuestionType.CODING).questionId("cp-201").order(2).customPoints(10.0).build()
                ))
                .build();
    }

    @Test
    @DisplayName("createAssessment should snapshot questions from published question bank")
    void shouldCreateAssessmentWithQuestionBank() {
        CreateAssessmentRequest req = CreateAssessmentRequest.builder()
                .title("Frontend Developer Screening")
                .selectionMode(AssessmentSelectionMode.QUESTION_BANK)
                .questionBankId("qb-1")
                .durationMinutes(60)
                .candidateEmailsRaw("alice@example.com, bob@example.com")
                .build();

        when(questionBankRepository.findById("qb-1")).thenReturn(Optional.of(sampleBank));
        when(mcqQuestionRepository.findById("mcq-101")).thenReturn(Optional.of(sampleMcq));
        when(codingProblemRepository.findById("cp-201")).thenReturn(Optional.of(sampleCoding));
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(inv -> {
            Assessment a = inv.getArgument(0);
            if (a.getId() == null) a.setId("ass-1");
            return a;
        });

        AssessmentSummaryResponse res = assessmentService.createAssessment(req, "admin-1");

        assertThat(res.getTitle()).isEqualTo("Frontend Developer Screening");
        assertThat(res.getTotalQuestions()).isEqualTo(2);
        assertThat(res.getTotalScore()).isEqualTo(15.0);
        assertThat(res.getDurationMinutes()).isEqualTo(60);
        verify(assessmentRepository, atLeastOnce()).save(any(Assessment.class));
    }

    @Test
    @DisplayName("validateAssessmentConfig should succeed when configuration and question bank are valid")
    void shouldValidateAssessmentSuccessfully() {
        CreateAssessmentRequest req = CreateAssessmentRequest.builder()
                .title("Valid Assessment")
                .selectionMode(AssessmentSelectionMode.QUESTION_BANK)
                .questionBankId("qb-1")
                .durationMinutes(60)
                .candidateEmailsRaw("cand@test.com")
                .build();

        when(questionBankRepository.findById("qb-1")).thenReturn(Optional.of(sampleBank));
        when(mcqQuestionRepository.findById("mcq-101")).thenReturn(Optional.of(sampleMcq));
        when(codingProblemRepository.findById("cp-201")).thenReturn(Optional.of(sampleCoding));

        AssessmentValidationResponse res = assessmentService.validateAssessmentConfig(req);

        assertThat(res.isValid()).isTrue();
        assertThat(res.getErrors()).isEmpty();
        assertThat(res.getCalculatedTotalQuestions()).isEqualTo(2);
        assertThat(res.getCalculatedTotalScore()).isEqualTo(15.0);
        assertThat(res.getBankPreview()).isNotNull();
        assertThat(res.getBankPreview().isEligibleForAssessment()).isTrue();
    }

    @Test
    @DisplayName("validateAssessmentConfig should fail when question bank is in DRAFT status")
    void shouldFailValidationWhenQuestionBankIsNotPublished() {
        QuestionBank draftBank = QuestionBank.builder()
                .id("qb-draft")
                .title("Draft Bank")
                .status(QuestionBankStatus.DRAFT)
                .items(List.of(QuestionBankItem.builder().questionType(BankQuestionType.MCQ).questionId("mcq-101").order(1).build()))
                .build();

        CreateAssessmentRequest req = CreateAssessmentRequest.builder()
                .title("Draft Assessment")
                .selectionMode(AssessmentSelectionMode.QUESTION_BANK)
                .questionBankId("qb-draft")
                .durationMinutes(30)
                .build();

        when(questionBankRepository.findById("qb-draft")).thenReturn(Optional.of(draftBank));
        when(mcqQuestionRepository.findById("mcq-101")).thenReturn(Optional.of(sampleMcq));

        AssessmentValidationResponse res = assessmentService.validateAssessmentConfig(req);

        assertThat(res.isValid()).isFalse();
        assertThat(res.getErrors()).anyMatch(e -> "questionBankId".equals(e.getField()) && "NOT_PUBLISHED".equals(e.getCode()));
    }

    @Test
    @DisplayName("validateAssessmentConfig should fail when scheduled window is shorter than duration")
    void shouldFailValidationWhenWindowShorterThanDuration() {
        Instant start = Instant.now().plus(Duration.ofHours(1));
        Instant end = start.plus(Duration.ofMinutes(30)); // 30 min window

        CreateAssessmentRequest req = CreateAssessmentRequest.builder()
                .title("Time Mismatch Assessment")
                .selectionMode(AssessmentSelectionMode.QUESTION_BANK)
                .questionBankId("qb-1")
                .scheduledStart(start)
                .scheduledEnd(end)
                .durationMinutes(60) // 60 min duration > 30 min window
                .build();

        when(questionBankRepository.findById("qb-1")).thenReturn(Optional.of(sampleBank));
        when(mcqQuestionRepository.findById("mcq-101")).thenReturn(Optional.of(sampleMcq));
        when(codingProblemRepository.findById("cp-201")).thenReturn(Optional.of(sampleCoding));

        AssessmentValidationResponse res = assessmentService.validateAssessmentConfig(req);

        assertThat(res.isValid()).isFalse();
        assertThat(res.getErrors()).anyMatch(e -> "scheduledWindow".equals(e.getField()) && "WINDOW_TOO_SHORT".equals(e.getCode()));
    }

    @Test
    @DisplayName("validateAssessmentConfig should fail when passing score exceeds total points")
    void shouldFailValidationWhenPassingScoreExceedsTotalPoints() {
        CreateAssessmentRequest req = CreateAssessmentRequest.builder()
                .title("Excess Passing Score")
                .selectionMode(AssessmentSelectionMode.QUESTION_BANK)
                .questionBankId("qb-1")
                .durationMinutes(60)
                .passingScore(100.0) // Total points is 15.0
                .build();

        when(questionBankRepository.findById("qb-1")).thenReturn(Optional.of(sampleBank));
        when(mcqQuestionRepository.findById("mcq-101")).thenReturn(Optional.of(sampleMcq));
        when(codingProblemRepository.findById("cp-201")).thenReturn(Optional.of(sampleCoding));

        AssessmentValidationResponse res = assessmentService.validateAssessmentConfig(req);

        assertThat(res.isValid()).isFalse();
        assertThat(res.getErrors()).anyMatch(e -> "passingScore".equals(e.getField()) && "EXCEEDS_TOTAL".equals(e.getCode()));
    }

    @Test
    @DisplayName("createAssessment should fail atomically when pre-creation validation fails")
    void shouldThrowBadRequestWhenValidationFailsOnCreate() {
        CreateAssessmentRequest req = CreateAssessmentRequest.builder()
                .title("") // Blank title
                .selectionMode(AssessmentSelectionMode.QUESTION_BANK)
                .questionBankId("qb-1")
                .durationMinutes(0) // Invalid duration
                .build();

        assertThatThrownBy(() -> assessmentService.createAssessment(req, "admin-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("validation failed");

        verify(assessmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("getQuestionBankPreview should return difficulty distribution and coding languages")
    void shouldGetQuestionBankPreviewDetails() {
        when(questionBankRepository.findById("qb-1")).thenReturn(Optional.of(sampleBank));
        when(mcqQuestionRepository.findById("mcq-101")).thenReturn(Optional.of(sampleMcq));
        when(codingProblemRepository.findById("cp-201")).thenReturn(Optional.of(sampleCoding));

        QuestionBankPreviewDto preview = assessmentService.getQuestionBankPreview("qb-1");

        assertThat(preview.getId()).isEqualTo("qb-1");
        assertThat(preview.getTitle()).isEqualTo("Java Assessment Bank");
        assertThat(preview.isEligibleForAssessment()).isTrue();
        assertThat(preview.getSupportedCodingLanguages()).containsExactly("java-21");
    }

    @Test
    @DisplayName("bulkAssignCandidates should parse whitespace and comma-separated emails and ignore duplicates")
    void shouldBulkAssignCandidatesSuccessfully() {
        Assessment assessment = Assessment.builder()
                .id("ass-1")
                .title("Mid-Level Backend Assessment")
                .status(AssessmentStatus.NOT_STARTED)
                .scheduledStart(Instant.now().plus(Duration.ofHours(2)))
                .build();

        when(assessmentRepository.findById("ass-1")).thenReturn(Optional.of(assessment));
        when(assignmentRepository.existsByAssessmentIdAndCandidateEmail("ass-1", "alice@example.com")).thenReturn(false);
        when(assignmentRepository.existsByAssessmentIdAndCandidateEmail("ass-1", "bob@example.com")).thenReturn(false);
        when(assignmentRepository.countByAssessmentId("ass-1")).thenReturn(2L);
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(inv -> inv.getArgument(0));

        BulkAssignCandidatesRequest req = BulkAssignCandidatesRequest.builder()
                .emailsRaw("alice@example.com, bob@example.com\n  invalid-email-address  \n  alice@example.com")
                .build();

        BulkAssignCandidatesResponse res = assessmentService.bulkAssignCandidates("ass-1", req);

        assertThat(res.getSuccessfullyAssigned()).isEqualTo(2);
        assertThat(res.getInvalidEmails()).contains("invalid-email-address");
        verify(assignmentRepository, times(2)).save(any(AssessmentAssignment.class));
    }

    @Test
    @DisplayName("startAttempt should compute effective deadline as min(now + duration, scheduledEnd)")
    void shouldStartAttemptWithEffectiveDeadline() {
        Instant now = Instant.now();
        Instant scheduledEnd = now.plus(Duration.ofMinutes(30)); // Closes in 30 mins

        Assessment assessment = Assessment.builder()
                .id("ass-1")
                .title("Spring Core Quiz")
                .status(AssessmentStatus.ACTIVE)
                .scheduledStart(now.minus(Duration.ofMinutes(10)))
                .durationMinutes(60) // 60 mins duration > scheduledEnd (30 mins)
                .scheduledEnd(scheduledEnd)
                .totalScore(100.0)
                .build();

        AssessmentAssignment assignment = AssessmentAssignment.builder()
                .id("assign-1")
                .assessmentId("ass-1")
                .candidateEmail("candidate@example.com")
                .attemptsCount(0)
                .status(CandidateParticipationStatus.INVITED)
                .build();

        when(assessmentRepository.findById("ass-1")).thenReturn(Optional.of(assessment));
        when(assignmentRepository.findByAssessmentIdAndCandidateUserId("ass-1", "user-1")).thenReturn(Optional.of(assignment));
        when(attemptRepository.findByAssessmentIdAndCandidateUserIdAndStatus("ass-1", "user-1", AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(attemptRepository.save(any(AssessmentAttempt.class))).thenAnswer(inv -> {
            AssessmentAttempt att = inv.getArgument(0);
            att.setId("att-123");
            return att;
        });

        StartAttemptResponse res = assessmentService.startAttempt("ass-1", "user-1", "candidate@example.com");

        assertThat(res.getAttemptId()).isEqualTo("att-123");
        assertThat(res.getAttemptNumber()).isEqualTo(1);
        assertThat(res.getStatus()).isEqualTo(AttemptStatus.IN_PROGRESS);
        // Effective deadline must cap at scheduledEnd (not now + 60 min)
        assertThat(res.getEffectiveDeadline()).isEqualTo(scheduledEnd);
    }

    @Test
    @DisplayName("startAttempt should resume existing IN_PROGRESS attempt without consuming a new attempt")
    void shouldResumeExistingInProgressAttempt() {
        Assessment assessment = Assessment.builder()
                .id("ass-1")
                .title("Python Assessment")
                .status(AssessmentStatus.ACTIVE)
                .scheduledStart(Instant.now().minus(Duration.ofMinutes(10)))
                .durationMinutes(45)
                .build();

        AssessmentAssignment assignment = AssessmentAssignment.builder()
                .id("assign-1")
                .assessmentId("ass-1")
                .candidateEmail("cand@test.com")
                .attemptsCount(1)
                .build();

        AssessmentAttempt existingAttempt = AssessmentAttempt.builder()
                .id("att-existing")
                .assessmentId("ass-1")
                .attemptNumber(1)
                .status(AttemptStatus.IN_PROGRESS)
                .startedAt(Instant.now().minus(Duration.ofMinutes(10)))
                .effectiveDeadline(Instant.now().plus(Duration.ofMinutes(35)))
                .build();

        when(assessmentRepository.findById("ass-1")).thenReturn(Optional.of(assessment));
        when(assignmentRepository.findByAssessmentIdAndCandidateUserId("ass-1", "user-1")).thenReturn(Optional.of(assignment));
        when(attemptRepository.findByAssessmentIdAndCandidateUserIdAndStatus("ass-1", "user-1", AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(existingAttempt));

        StartAttemptResponse res = assessmentService.startAttempt("ass-1", "user-1", "cand@test.com");

        assertThat(res.getAttemptId()).isEqualTo("att-existing");
        assertThat(res.getAttemptNumber()).isEqualTo(1);
        verify(attemptRepository, never()).save(any(AssessmentAttempt.class));
    }

    @Test
    @DisplayName("saveMcqResponse should evaluate answer correctness and store response")
    void shouldSaveAndEvaluateMcqResponse() {
        Assessment assessment = Assessment.builder()
                .id("ass-1")
                .items(List.of(
                        AssessmentItem.builder()
                                .questionId("mcq-101")
                                .questionType(BankQuestionType.MCQ)
                                .points(5.0)
                                .snapshot(QuestionSnapshot.builder()
                                        .options(List.of(new OptionSnapshot("opt-1", "A", 1), new OptionSnapshot("opt-2", "B", 2)))
                                        .correctOptionIds(List.of("opt-1"))
                                        .build())
                                .build()
                ))
                .build();

        AssessmentAttempt attempt = AssessmentAttempt.builder()
                .id("att-1")
                .assessmentId("ass-1")
                .candidateUserId("user-1")
                .status(AttemptStatus.IN_PROGRESS)
                .effectiveDeadline(Instant.now().plus(Duration.ofMinutes(20)))
                .build();

        when(assessmentRepository.findById("ass-1")).thenReturn(Optional.of(assessment));
        when(attemptRepository.findByAssessmentIdAndCandidateUserIdAndStatus("ass-1", "user-1", AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(attempt));
        when(responseRepository.findByAttemptIdAndQuestionId("att-1", "mcq-101")).thenReturn(Optional.empty());
        when(responseRepository.save(any(AssessmentQuestionResponse.class))).thenAnswer(inv -> inv.getArgument(0));

        SaveMcqResponseRequest req = SaveMcqResponseRequest.builder()
                .questionId("mcq-101")
                .selectedOptionIds(List.of("opt-1"))
                .build();

        SavedQuestionResponseDto res = assessmentService.saveMcqResponse("ass-1", "user-1", req);

        assertThat(res.getQuestionId()).isEqualTo("mcq-101");
        assertThat(res.getSelectedOptionIds()).containsExactly("opt-1");
        verify(responseRepository).save(any(AssessmentQuestionResponse.class));
    }

    @Test
    @DisplayName("finalSubmit should compute total candidate score and mark attempt SUBMITTED")
    void shouldFinalizeAssessmentSubmission() {
        Assessment assessment = Assessment.builder()
                .id("ass-1")
                .title("Full Stack Exam")
                .totalScore(20.0)
                .passingScore(15.0)
                .build();

        AssessmentAttempt attempt = AssessmentAttempt.builder()
                .id("att-1")
                .assessmentId("ass-1")
                .candidateUserId("user-1")
                .status(AttemptStatus.IN_PROGRESS)
                .effectiveDeadline(Instant.now().plus(Duration.ofMinutes(10)))
                .build();

        AssessmentQuestionResponse resp1 = AssessmentQuestionResponse.builder().awardedPoints(10.0).build();
        AssessmentQuestionResponse resp2 = AssessmentQuestionResponse.builder().awardedPoints(6.0).build();

        when(assessmentRepository.findById("ass-1")).thenReturn(Optional.of(assessment));
        when(attemptRepository.findByAssessmentIdAndCandidateUserIdAndStatus("ass-1", "user-1", AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(attempt));
        when(responseRepository.findAllByAttemptId("att-1")).thenReturn(List.of(resp1, resp2));
        when(assignmentRepository.findByAssessmentIdAndCandidateUserId("ass-1", "user-1")).thenReturn(Optional.empty());

        FinalSubmitResponse res = assessmentService.finalSubmit("ass-1", "user-1");

        assertThat(res.getStatus()).isEqualTo(AttemptStatus.SUBMITTED);
        assertThat(res.getScore()).isEqualTo(16.0);
        assertThat(res.getPassed()).isTrue();
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.SUBMITTED);
        verify(attemptRepository).save(attempt);
    }

    @Test
    @DisplayName("generateInvitationCode should assign unpredictable code and enable it")
    void shouldGenerateInvitationCode() {
        Assessment assessment = Assessment.builder()
                .id("ass-1")
                .title("Midterm")
                .items(new ArrayList<>())
                .build();

        when(assessmentRepository.findById("ass-1")).thenReturn(Optional.of(assessment));
        when(assessmentRepository.existsByInvitationCode(anyString())).thenReturn(false);
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(inv -> inv.getArgument(0));

        AssessmentDetailResponse response = assessmentService.generateInvitationCode("ass-1");

        assertThat(response.getInvitationCode()).isNotBlank();
        assertThat(response.getInvitationCode()).startsWith("STK-");
        assertThat(response.isInvitationCodeEnabled()).isTrue();
    }

    @Test
    @DisplayName("joinByInvitationCode should idempotently register candidate")
    void shouldJoinByInvitationCode() {
        Assessment assessment = Assessment.builder()
                .id("ass-1")
                .title("Coding Screening")
                .status(AssessmentStatus.NOT_STARTED)
                .scheduledStart(Instant.now().plus(Duration.ofHours(2)))
                .invitationCode("STK-TEST-CODE")
                .invitationCodeEnabled(true)
                .items(new ArrayList<>())
                .build();

        when(assessmentRepository.findByInvitationCode("STK-TEST-CODE")).thenReturn(Optional.of(assessment));
        when(assignmentRepository.findByAssessmentIdAndCandidateEmail("ass-1", "cand@test.com")).thenReturn(Optional.empty());
        when(assignmentRepository.findByAssessmentIdAndCandidateUserId("ass-1", "cand-user-1")).thenReturn(Optional.empty());
        when(assignmentRepository.save(any(AssessmentAssignment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(inv -> inv.getArgument(0));

        CandidateAssessmentSummaryResponse summary = assessmentService.joinByInvitationCode(
                "cand-user-1", "cand@test.com", new JoinByCodeRequest("STK-TEST-CODE")
        );

        assertThat(summary.getAssessmentId()).isEqualTo("ass-1");
        assertThat(summary.getTitle()).isEqualTo("Coding Screening");
        verify(assignmentRepository).save(any(AssessmentAssignment.class));
    }

    @Test
    @DisplayName("markForReview should toggle review flag on question response")
    void shouldMarkQuestionForReview() {
        Assessment assessment = Assessment.builder()
                .id("ass-1")
                .items(List.of(AssessmentItem.builder()
                        .questionId("q-1")
                        .points(10.0)
                        .questionType(BankQuestionType.MCQ)
                        .build()))
                .build();

        AssessmentAttempt attempt = AssessmentAttempt.builder()
                .id("att-1")
                .assessmentId("ass-1")
                .candidateUserId("user-1")
                .status(AttemptStatus.IN_PROGRESS)
                .effectiveDeadline(Instant.now().plus(Duration.ofMinutes(30)))
                .build();

        AssessmentQuestionResponse response = AssessmentQuestionResponse.builder()
                .attemptId("att-1")
                .questionId("q-1")
                .markedForReview(false)
                .build();

        when(assessmentRepository.findById("ass-1")).thenReturn(Optional.of(assessment));
        when(attemptRepository.findByAssessmentIdAndCandidateUserIdAndStatus("ass-1", "user-1", AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(attempt));
        when(responseRepository.findByAttemptIdAndQuestionId("att-1", "q-1")).thenReturn(Optional.of(response));
        when(responseRepository.save(any(AssessmentQuestionResponse.class))).thenAnswer(inv -> inv.getArgument(0));

        SavedQuestionResponseDto res = assessmentService.markForReview(
                "ass-1", "user-1", new MarkQuestionForReviewRequest("q-1", true)
        );

        assertThat(res.isMarkedForReview()).isTrue();
        assertThat(response.isMarkedForReview()).isTrue();
    }

    @Test
    @DisplayName("deriveStatus should evaluate precedence and boundaries accurately")
    void shouldDeriveStatusCorrectlyAcrossBoundaries() {
        Instant now = Instant.parse("2026-10-21T12:00:00Z");
        Instant start = Instant.parse("2026-10-21T10:00:00Z");
        Instant end = Instant.parse("2026-10-21T14:00:00Z");

        // 1. Within active window
        assertThat(AssessmentStatus.deriveStatus(start, end, now)).isEqualTo(AssessmentStatus.ACTIVE);

        // 2. Before start -> NOT_STARTED
        assertThat(AssessmentStatus.deriveStatus(start, end, start.minusSeconds(1))).isEqualTo(AssessmentStatus.NOT_STARTED);

        // 3. Inclusive start boundary -> ACTIVE
        assertThat(AssessmentStatus.deriveStatus(start, end, start)).isEqualTo(AssessmentStatus.ACTIVE);

        // 4. Exclusive end boundary -> COMPLETED
        assertThat(AssessmentStatus.deriveStatus(start, end, end)).isEqualTo(AssessmentStatus.COMPLETED);

        // 5. After end -> COMPLETED
        assertThat(AssessmentStatus.deriveStatus(start, end, end.plusSeconds(3600))).isEqualTo(AssessmentStatus.COMPLETED);

        // 6. No start time configured -> ACTIVE immediately
        assertThat(AssessmentStatus.deriveStatus(null, end, now)).isEqualTo(AssessmentStatus.ACTIVE);

        // 7. No start and end passed -> COMPLETED
        assertThat(AssessmentStatus.deriveStatus(null, end, end.plusSeconds(10))).isEqualTo(AssessmentStatus.COMPLETED);
    }

    @Test
    @DisplayName("bulkAssignCandidates should reject assignment when assessment is COMPLETED")
    void shouldRejectBulkAssignWhenAssessmentIsCompleted() {
        Assessment completedAssessment = Assessment.builder()
                .id("ass-ended")
                .title("Ended Test")
                .scheduledEnd(Instant.now().minus(Duration.ofMinutes(10)))
                .build();

        when(assessmentRepository.findById("ass-ended")).thenReturn(Optional.of(completedAssessment));

        BulkAssignCandidatesRequest req = BulkAssignCandidatesRequest.builder()
                .emails(List.of("late-candidate@example.com"))
                .build();

        assertThatThrownBy(() -> assessmentService.bulkAssignCandidates("ass-ended", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Candidate enrollment is closed");
    }

    @Test
    @DisplayName("joinByInvitationCode should reject enrollment when assessment is COMPLETED")
    void shouldRejectJoinByInvitationCodeWhenAssessmentIsCompleted() {
        Assessment completedAssessment = Assessment.builder()
                .id("ass-ended")
                .title("Ended Test")
                .invitationCode("STK-ENDED")
                .invitationCodeEnabled(true)
                .scheduledEnd(Instant.now().minus(Duration.ofMinutes(10)))
                .build();

        when(assessmentRepository.findByInvitationCode("STK-ENDED")).thenReturn(Optional.of(completedAssessment));

        JoinByCodeRequest req = new JoinByCodeRequest("STK-ENDED");

        assertThatThrownBy(() -> assessmentService.joinByInvitationCode("user-new", "new@test.com", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Candidate registration is closed");
    }

    @Test
    @DisplayName("joinByInvitationCode should allow candidate to join when assessment is ACTIVE")
    void shouldAllowJoinByInvitationCodeWhenAssessmentIsActive() {
        Assessment activeAssessment = Assessment.builder()
                .id("ass-active")
                .title("Active Test")
                .invitationCode("STK-ACTIVE")
                .invitationCodeEnabled(true)
                .durationMinutes(60)
                .items(new ArrayList<>())
                .build();

        when(assessmentRepository.findByInvitationCode("STK-ACTIVE")).thenReturn(Optional.of(activeAssessment));
        when(assignmentRepository.findByAssessmentIdAndCandidateEmail("ass-active", "new@test.com")).thenReturn(Optional.empty());
        when(assignmentRepository.findByAssessmentIdAndCandidateUserId("ass-active", "user-new")).thenReturn(Optional.empty());
        when(assignmentRepository.save(any(AssessmentAssignment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(inv -> inv.getArgument(0));

        CandidateAssessmentSummaryResponse summary = assessmentService.joinByInvitationCode(
                "user-new", "new@test.com", new JoinByCodeRequest("STK-ACTIVE")
        );

        assertThat(summary.getAssessmentId()).isEqualTo("ass-active");
        assertThat(summary.getAssessmentStatus()).isEqualTo(AssessmentStatus.ACTIVE);
    }

    @Test
    @DisplayName("startAttempt should reject candidate attempt if assessment is NOT_STARTED")
    void shouldRejectStartAttemptWhenAssessmentNotStarted() {
        Instant futureStart = Instant.now().plus(Duration.ofHours(2));
        Assessment notStartedAssessment = Assessment.builder()
                .id("ass-future")
                .title("Upcoming Test")
                .scheduledStart(futureStart)
                .scheduledEnd(futureStart.plus(Duration.ofHours(2)))
                .durationMinutes(60)
                .build();

        AssessmentAssignment assignment = AssessmentAssignment.builder()
                .id("assign-1")
                .assessmentId("ass-future")
                .candidateEmail("cand@test.com")
                .candidateUserId("user-1")
                .attemptsCount(0)
                .build();

        when(assessmentRepository.findById("ass-future")).thenReturn(Optional.of(notStartedAssessment));
        when(assignmentRepository.findByAssessmentIdAndCandidateUserId("ass-future", "user-1")).thenReturn(Optional.of(assignment));
        when(attemptRepository.findByAssessmentIdAndCandidateUserIdAndStatus("ass-future", "user-1", AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> assessmentService.startAttempt("ass-future", "user-1", "cand@test.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Assessment has not started yet");
    }

    @Test
    @DisplayName("startAttempt should reject candidate attempt if assessment is COMPLETED")
    void shouldRejectStartAttemptWhenAssessmentCompleted() {
        Instant pastEnd = Instant.now().minus(Duration.ofHours(1));
        Assessment completedAssessment = Assessment.builder()
                .id("ass-ended")
                .title("Ended Test")
                .scheduledStart(pastEnd.minus(Duration.ofHours(2)))
                .scheduledEnd(pastEnd)
                .durationMinutes(60)
                .build();

        AssessmentAssignment assignment = AssessmentAssignment.builder()
                .id("assign-1")
                .assessmentId("ass-ended")
                .candidateEmail("cand@test.com")
                .candidateUserId("user-1")
                .attemptsCount(0)
                .build();

        when(assessmentRepository.findById("ass-ended")).thenReturn(Optional.of(completedAssessment));
        when(assignmentRepository.findByAssessmentIdAndCandidateUserId("ass-ended", "user-1")).thenReturn(Optional.of(assignment));
        when(attemptRepository.findByAssessmentIdAndCandidateUserIdAndStatus("ass-ended", "user-1", AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> assessmentService.startAttempt("ass-ended", "user-1", "cand@test.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Assessment has already ended");
    }

    @Test
    @DisplayName("completeAssessment should set scheduledEnd to now and status to COMPLETED")
    void shouldManuallyCompleteAssessment() {
        Assessment assessment = Assessment.builder()
                .id("ass-open")
                .title("Open Test")
                .status(AssessmentStatus.ACTIVE)
                .build();

        when(assessmentRepository.findById("ass-open")).thenReturn(Optional.of(assessment));
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(attemptRepository.findAllByAssessmentIdAndStatus("ass-open", AttemptStatus.IN_PROGRESS)).thenReturn(List.of());

        AssessmentDetailResponse res = assessmentService.completeAssessment("ass-open");

        assertThat(res.getStatus()).isEqualTo(AssessmentStatus.COMPLETED);
        assertThat(assessment.getScheduledEnd()).isNotNull();
        assertThat(assessment.getStatus()).isEqualTo(AssessmentStatus.COMPLETED);
        assertThat(assessment.isManuallyCompleted()).isTrue();
        assertThat(assessment.getManualInterventionMessage()).contains("manually completed");
        verify(webSocketHandler).broadcastToAssessment(eq("ass-open"), anyMap());
        verify(auditEventRepository).save(any(AssessmentAuditEvent.class));
    }

    @Test
    @DisplayName("activateAssessment should override future start, set status to ACTIVE, and notify candidates")
    void shouldManuallyActivateAssessment() {
        AssessmentItem item = AssessmentItem.builder().questionId("q-1").questionType(BankQuestionType.MCQ).points(5.0).build();
        Assessment assessment = Assessment.builder()
                .id("ass-future")
                .title("Future Test")
                .status(AssessmentStatus.NOT_STARTED)
                .scheduledStart(Instant.now().plus(Duration.ofHours(24)))
                .scheduledEnd(Instant.now().plus(Duration.ofHours(48)))
                .items(List.of(item))
                .build();

        when(assessmentRepository.findById("ass-future")).thenReturn(Optional.of(assessment));
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(inv -> inv.getArgument(0));

        AssessmentDetailResponse res = assessmentService.activateAssessment("ass-future");

        assertThat(res.getStatus()).isEqualTo(AssessmentStatus.ACTIVE);
        assertThat(assessment.isManuallyActivated()).isTrue();
        assertThat(assessment.getScheduledStart()).isBeforeOrEqualTo(Instant.now());
        assertThat(assessment.getManualInterventionMessage()).contains("manually activated");
        verify(webSocketHandler).broadcastToAssessment(eq("ass-future"), anyMap());
        verify(auditEventRepository).save(any(AssessmentAuditEvent.class));
    }

    @Test
    @DisplayName("activateAssessment should reject if assessment has no questions")
    void activateAssessment_FailsWhenNoQuestions() {
        Assessment assessment = Assessment.builder()
                .id("ass-empty")
                .status(AssessmentStatus.NOT_STARTED)
                .scheduledStart(Instant.now().plus(Duration.ofHours(2)))
                .items(List.of())
                .build();

        when(assessmentRepository.findById("ass-empty")).thenReturn(Optional.of(assessment));

        assertThatThrownBy(() -> assessmentService.activateAssessment("ass-empty"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no questions configured");
    }

    @Test
    @DisplayName("activateAssessment should reject if already ACTIVE or COMPLETED")
    void activateAssessment_FailsWhenAlreadyActiveOrCompleted() {
        Assessment activeAssessment = Assessment.builder()
                .id("ass-active")
                .status(AssessmentStatus.ACTIVE)
                .items(List.of(AssessmentItem.builder().questionId("q-1").build()))
                .build();

        when(assessmentRepository.findById("ass-active")).thenReturn(Optional.of(activeAssessment));

        assertThatThrownBy(() -> assessmentService.activateAssessment("ass-active"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already ACTIVE");
    }

    @Test
    @DisplayName("bulkAssignCandidates should reject enrollment after assessment is manually activated")
    void bulkAssignCandidates_FailsWhenManuallyActivated() {
        Assessment assessment = Assessment.builder()
                .id("ass-activated")
                .manuallyActivated(true)
                .status(AssessmentStatus.ACTIVE)
                .build();

        when(assessmentRepository.findById("ass-activated")).thenReturn(Optional.of(assessment));

        BulkAssignCandidatesRequest req = BulkAssignCandidatesRequest.builder()
                .emails(List.of("new@test.com"))
                .build();

        assertThatThrownBy(() -> assessmentService.bulkAssignCandidates("ass-activated", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Candidate roster is frozen");
    }

    @Test
    @DisplayName("joinByInvitationCode should reject new registration when assessment is manually activated")
    void joinByInvitationCode_FailsWhenManuallyActivated() {
        Assessment assessment = Assessment.builder()
                .id("ass-man-act")
                .invitationCode("STK-FROZEN")
                .invitationCodeEnabled(true)
                .manuallyActivated(true)
                .status(AssessmentStatus.ACTIVE)
                .build();

        when(assessmentRepository.findByInvitationCode("STK-FROZEN")).thenReturn(Optional.of(assessment));
        when(assignmentRepository.findByAssessmentIdAndCandidateEmail(eq("ass-man-act"), anyString())).thenReturn(Optional.empty());
        when(assignmentRepository.findByAssessmentIdAndCandidateUserId(eq("ass-man-act"), anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assessmentService.joinByInvitationCode("user-99", "new@test.com", new JoinByCodeRequest("STK-FROZEN")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Candidate roster is frozen");
    }
}

