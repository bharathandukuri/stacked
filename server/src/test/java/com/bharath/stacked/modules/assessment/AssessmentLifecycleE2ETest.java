package com.bharath.stacked.modules.assessment;

import com.bharath.stacked.AbstractIntegrationTest;
import com.bharath.stacked.exception.BadRequestException;
import com.bharath.stacked.modules.assessment.dto.*;
import com.bharath.stacked.modules.assessment.enums.AssessmentAuditEventType;
import com.bharath.stacked.modules.assessment.enums.AssessmentSelectionMode;
import com.bharath.stacked.modules.assessment.enums.AssessmentStatus;
import com.bharath.stacked.modules.assessment.enums.AttemptStatus;
import com.bharath.stacked.modules.assessment.model.AssessmentAttempt;
import com.bharath.stacked.modules.assessment.repository.AssessmentAuditEventRepository;
import com.bharath.stacked.modules.assessment.repository.AssessmentAttemptRepository;
import com.bharath.stacked.modules.assessment.service.AssessmentService;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemDifficulty;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemStatus;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import com.bharath.stacked.modules.coding_problem.model.CodingProblem;
import com.bharath.stacked.modules.coding_problem.model.TestCase;
import com.bharath.stacked.modules.coding_problem.repository.CodingProblemRepository;
import com.bharath.stacked.modules.mcq.dto.request.CreateMcqRequest;
import com.bharath.stacked.modules.mcq.dto.request.McqOptionRequest;
import com.bharath.stacked.modules.mcq.dto.response.McqQuestionDetailResponse;
import com.bharath.stacked.modules.mcq.enums.McqDifficulty;
import com.bharath.stacked.modules.mcq.enums.McqStatus;
import com.bharath.stacked.modules.mcq.enums.McqType;
import com.bharath.stacked.modules.mcq.service.McqQuestionService;
import com.bharath.stacked.modules.question_bank.dto.CreateQuestionBankRequest;
import com.bharath.stacked.modules.question_bank.dto.QuestionBankItemRequest;
import com.bharath.stacked.modules.question_bank.dto.QuestionBankSummaryResponse;
import com.bharath.stacked.modules.question_bank.enums.BankQuestionType;
import com.bharath.stacked.modules.question_bank.enums.QuestionBankStatus;
import com.bharath.stacked.modules.question_bank.service.QuestionBankService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssessmentLifecycleE2ETest extends AbstractIntegrationTest {

    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private QuestionBankService questionBankService;

    @Autowired
    private CodingProblemRepository codingProblemRepository;

    @Autowired
    private McqQuestionService mcqQuestionService;

    @Autowired
    private AssessmentAttemptRepository attemptRepository;

    @Autowired
    private AssessmentAuditEventRepository auditEventRepository;

    @Test
    @DisplayName("Complete Assessment Lifecycle: Creation, Pre-start Rejection, Manual Activation, Roster Freeze, Taking Exam, Manual Completion, Score Tallied, Post-completion Lockdown")
    void fullAssessmentLifecycleTest() {
        // 1. Create a published Coding Problem
        CodingProblem cp = codingProblemRepository.save(CodingProblem.builder()
                .title("Two Sum E2E")
                .slug("two-sum-e2e")
                .status(CodingProblemStatus.PUBLISHED)
                .problemType(CodingProblemType.GENERIC)
                .difficulty(CodingProblemDifficulty.EASY)
                .statement("Find indices of two numbers that add up to target.")
                .topics(List.of("Array", "Hash Table"))
                .supportedLanguageIds(List.of("java-21"))
                .referenceSolutionLanguage("java-21")
                .referenceSolution("public int[] twoSum(int[] nums, int target) { return new int[]{0, 1}; }")
                .sampleTestCases(List.of(new TestCase("tc-1", "2 7 11 15\n9", null, "0 1")))
                .build());
        assertThat(cp.getId()).isNotNull();

        // 2. Create a published MCQ
        McqQuestionDetailResponse mcq = mcqQuestionService.createQuestion(new CreateMcqRequest(
                "Java Virtual Threads E2E",
                "Are Java virtual threads scheduled on OS threads directly or carrier threads?",
                McqType.SINGLE_CHOICE,
                McqDifficulty.MEDIUM,
                McqStatus.PUBLISHED,
                List.of("Java"),
                List.of("concurrency", "threads"),
                List.of(
                        new McqOptionRequest("opt_os", "OS Threads", "Virtual threads are lightweight"),
                        new McqOptionRequest("opt_carrier", "Carrier Threads", "Virtual threads are mounted on carrier threads")
                ),
                List.of("opt_carrier"),
                "Virtual threads are user-mode threads mounted on ForkJoinPool carrier threads.",
                false,
                List.of()
        ), "admin-lifecycle-author");
        assertThat(mcq.id()).isNotNull();

        // 3. Create a Question Bank referencing both questions
        CreateQuestionBankRequest bankRequest = CreateQuestionBankRequest.builder()
                .title("Senior Backend Assessment Bank")
                .description("Lifecycle test bank")
                .category("Backend Engineering")
                .status(QuestionBankStatus.PUBLISHED)
                .tags(List.of("java", "virtual-threads"))
                .items(List.of(
                        QuestionBankItemRequest.builder()
                                .questionType(BankQuestionType.CODING)
                                .questionId(cp.getId())
                                .order(1)
                                .customPoints(50.0)
                                .build(),
                        QuestionBankItemRequest.builder()
                                .questionType(BankQuestionType.MCQ)
                                .questionId(mcq.id())
                                .order(2)
                                .customPoints(10.0)
                                .build()
                ))
                .build();
        QuestionBankSummaryResponse bank = questionBankService.createQuestionBank(bankRequest, "admin-lifecycle-author");
        assertThat(bank.getId()).isNotNull();

        // 4. Create an Assessment scheduled in the future (NOT_STARTED)
        Instant futureStart = Instant.now().plus(Duration.ofHours(3));
        Instant futureEnd = Instant.now().plus(Duration.ofHours(6));

        CreateAssessmentRequest assessmentReq = CreateAssessmentRequest.builder()
                .title("Platform Engineering Screening Q4")
                .description("E2E verification of manual activation and completion controls")
                .selectionMode(AssessmentSelectionMode.QUESTION_BANK)
                .questionBankId(bank.getId())
                .durationMinutes(60)
                .scheduledStart(futureStart)
                .scheduledEnd(futureEnd)
                .candidateEmails(List.of("candidate1@stacked.com"))
                .enableInvitationCode(true)
                .build();

        AssessmentSummaryResponse created = assessmentService.createAssessment(assessmentReq, "admin-lifecycle-author");
        String assessmentId = created.getId();
        assertThat(assessmentId).isNotNull();

        AssessmentDetailResponse detailsBefore = assessmentService.getAssessment(assessmentId);
        assertThat(detailsBefore.getStatus()).isEqualTo(AssessmentStatus.NOT_STARTED);
        assertThat(detailsBefore.isManuallyActivated()).isFalse();
        assertThat(detailsBefore.isManuallyCompleted()).isFalse();

        // 5. Verify Candidate cannot begin attempt before activation
        assertThatThrownBy(() -> assessmentService.startAttempt(assessmentId, "user-cand-1", "candidate1@stacked.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Assessment has not started yet");

        // 6. Admin manually activates the assessment
        AssessmentDetailResponse activated = assessmentService.activateAssessment(assessmentId);
        assertThat(activated.getStatus()).isEqualTo(AssessmentStatus.ACTIVE);
        assertThat(activated.isManuallyActivated()).isTrue();
        assertThat(activated.getManuallyActivatedAt()).isNotNull();
        assertThat(activated.getManualInterventionMessage()).startsWith("The administrator has manually activated this assessment");

        // Verify audit event persisted
        long activationAuditCount = auditEventRepository.countByAssessmentIdAndEventType(assessmentId, AssessmentAuditEventType.MANUAL_ACTIVATION);
        assertThat(activationAuditCount).isGreaterThanOrEqualTo(1);

        // 7. Verify Candidate roster is frozen once activated
        // (a) Bulk assign is rejected
        assertThatThrownBy(() -> assessmentService.bulkAssignCandidates(
                assessmentId,
                BulkAssignCandidatesRequest.builder().emails(List.of("late-comer@stacked.com")).build()
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Candidate roster is frozen");

        // (b) New join-by-invitation-code is rejected
        assertThatThrownBy(() -> assessmentService.joinByInvitationCode(
                "user-cand-late",
                "late-comer@stacked.com",
                JoinByCodeRequest.builder().code(activated.getInvitationCode()).build()
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Candidate roster is frozen");

        // 8. Assigned candidate starts attempt
        StartAttemptResponse startResp = assessmentService.startAttempt(assessmentId, "user-cand-1", "candidate1@stacked.com");
        assertThat(startResp.getAttemptId()).isNotNull();
        assertThat(startResp.getStatus()).isEqualTo(AttemptStatus.IN_PROGRESS);

        // 9. Candidate submits an MCQ answer
        AssessmentItemResponse mcqItem = activated.getItems().stream()
                .filter(i -> i.getQuestionType().name().equals("MCQ"))
                .findFirst()
                .orElseThrow();

        SaveMcqResponseRequest saveMcqReq = SaveMcqResponseRequest.builder()
                .questionId(mcqItem.getQuestionId())
                .selectedOptionIds(List.of("opt_carrier"))
                .build();

        SavedQuestionResponseDto savedResp = assessmentService.saveMcqResponse(assessmentId, "user-cand-1", saveMcqReq);
        assertThat(savedResp).isNotNull();
        assertThat(savedResp.getSelectedOptionIds()).containsExactly("opt_carrier");

        // 10. Admin manually completes the assessment
        AssessmentDetailResponse completed = assessmentService.completeAssessment(assessmentId);
        assertThat(completed.getStatus()).isEqualTo(AssessmentStatus.COMPLETED);
        assertThat(completed.isManuallyCompleted()).isTrue();
        assertThat(completed.getManuallyCompletedAt()).isNotNull();
        assertThat(completed.getManualInterventionMessage()).startsWith("The administrator has manually completed this assessment");

        // Verify audit event persisted
        long completionAuditCount = auditEventRepository.countByAssessmentIdAndEventType(assessmentId, AssessmentAuditEventType.MANUAL_COMPLETION);
        assertThat(completionAuditCount).isGreaterThanOrEqualTo(1);

        // 11. Verify candidate's ongoing attempt was finalized and score tallied
        AssessmentAttempt attemptAfterCompletion = attemptRepository.findById(startResp.getAttemptId()).orElseThrow();
        assertThat(attemptAfterCompletion.getStatus()).isEqualTo(AttemptStatus.EXPIRED);
        assertThat(attemptAfterCompletion.getScore()).isEqualTo(10.0); // Got the MCQ right (10 pts)

        // 12. Verify further candidate modification is rejected
        assertThatThrownBy(() -> assessmentService.saveMcqResponse(assessmentId, "user-cand-1", saveMcqReq))
                .isInstanceOf(BadRequestException.class);
    }
}
