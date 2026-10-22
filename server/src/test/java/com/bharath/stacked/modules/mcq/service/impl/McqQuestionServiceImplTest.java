package com.bharath.stacked.modules.mcq.service.impl;

import com.bharath.stacked.common.api.PageResponse;
import com.bharath.stacked.exception.BadRequestException;
import com.bharath.stacked.exception.ConflictException;
import com.bharath.stacked.exception.ResourceNotFoundException;
import com.bharath.stacked.modules.assessment.model.Assessment;
import com.bharath.stacked.modules.assessment.repository.AssessmentRepository;
import com.bharath.stacked.modules.file.service.FileService;
import com.bharath.stacked.modules.question_bank.model.QuestionBank;
import com.bharath.stacked.modules.question_bank.repository.QuestionBankRepository;
import com.bharath.stacked.modules.mcq.dto.request.CreateMcqRequest;
import com.bharath.stacked.modules.mcq.dto.request.McqFilterRequest;
import com.bharath.stacked.modules.mcq.dto.request.McqOptionRequest;
import com.bharath.stacked.modules.mcq.dto.request.UpdateMcqRequest;
import com.bharath.stacked.modules.mcq.dto.response.CandidateMcqResponse;
import com.bharath.stacked.modules.mcq.dto.response.McqQuestionDetailResponse;
import com.bharath.stacked.modules.mcq.dto.response.McqQuestionSummaryResponse;
import com.bharath.stacked.modules.mcq.dto.response.McqStatsResponse;
import com.bharath.stacked.modules.mcq.enums.McqDifficulty;
import com.bharath.stacked.modules.mcq.enums.McqStatus;
import com.bharath.stacked.modules.mcq.enums.McqType;
import com.bharath.stacked.modules.mcq.model.McqOption;
import com.bharath.stacked.modules.mcq.model.McqQuestion;
import com.bharath.stacked.modules.mcq.repository.McqQuestionRepository;
import com.bharath.stacked.modules.mcq.repository.McqTopicRepository;
import com.bharath.stacked.modules.mcq.service.McqTopicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class McqQuestionServiceImplTest {

    @Mock
    private McqQuestionRepository questionRepository;

    @Mock
    private McqTopicRepository topicRepository;

    @Mock
    private McqTopicService topicService;

    @Mock
    private FileService fileService;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Mock
    private QuestionBankRepository questionBankRepository;

    @Mock
    private AssessmentRepository assessmentRepository;

    @InjectMocks
    private McqQuestionServiceImpl questionService;

    private McqQuestion sampleQuestion;

    @BeforeEach
    void setUp() {
        sampleQuestion = McqQuestion.builder()
                .id("mcq-101")
                .title("Java Volatile Visibility")
                .slug("java-volatile-visibility")
                .content("What is the effect of volatile keyword in Java?")
                .type(McqType.SINGLE_CHOICE)
                .difficulty(McqDifficulty.MEDIUM)
                .status(McqStatus.PUBLISHED)
                .topics(List.of("Java", "Concurrency"))
                .tags(List.of("CoreJava"))
                .options(List.of(
                        new McqOption("opt_1", "Ensures atomicity and visibility", "Incorrect"),
                        new McqOption("opt_2", "Ensures visibility across threads", "Correct"),
                        new McqOption("opt_3", "Prevents object garbage collection", "Incorrect")))
                .correctOptionIds(List.of("opt_2"))
                .explanation("Volatile creates a happens-before memory barrier ensuring visibility.")
                .defaultPoints(2.0)
                .negativePoints(0.5)
                .shuffleOptions(true)
                .authorId("admin-1")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("createQuestion successfully creates a single-choice MCQ with valid options")
    void createQuestion_SingleChoice_Success() {
        CreateMcqRequest request = new CreateMcqRequest(
                "Java Volatile Visibility",
                "What is the effect of volatile keyword in Java?",
                McqType.SINGLE_CHOICE,
                McqDifficulty.MEDIUM,
                McqStatus.PUBLISHED,
                List.of("Java", "Concurrency"),
                List.of("CoreJava"),
                List.of(
                        new McqOptionRequest("opt_1", "Ensures atomicity and visibility", "Incorrect"),
                        new McqOptionRequest("opt_2", "Ensures visibility across threads", "Correct")),
                List.of("opt_2"),
                "Volatile creates a happens-before memory barrier.",
                2.0,
                0.5,
                true,
                null);

        when(questionRepository.findBySlug(any())).thenReturn(Optional.empty());
        when(questionRepository.save(any(McqQuestion.class))).thenAnswer(invocation -> {
            McqQuestion q = invocation.getArgument(0);
            q.setId("mcq-generated-id");
            return q;
        });

        McqQuestionDetailResponse response = questionService.createQuestion(request, "admin-1");

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("mcq-generated-id");
        assertThat(response.title()).isEqualTo("Java Volatile Visibility");
        assertThat(response.type()).isEqualTo(McqType.SINGLE_CHOICE);
        assertThat(response.correctOptionIds()).containsExactly("opt_2");
        assertThat(response.options()).hasSize(2);

        verify(topicService).incrementQuestionCounts(any());
    }

    @Test
    @DisplayName("createQuestion throws BadRequestException when options list has less than 2 options")
    void createQuestion_LessThanTwoOptions_ThrowsBadRequest() {
        CreateMcqRequest request = new CreateMcqRequest(
                "Invalid Question",
                "Question stem",
                McqType.SINGLE_CHOICE,
                McqDifficulty.EASY,
                McqStatus.DRAFT,
                List.of("Java"),
                null,
                List.of(new McqOptionRequest("opt_1", "Only one option", null)),
                List.of("opt_1"),
                null,
                1.0,
                0.0,
                true,
                null);

        assertThatThrownBy(() -> questionService.createQuestion(request, "admin-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least two options");
    }

    @Test
    @DisplayName("createQuestion throws BadRequestException when single-choice has multiple correct options")
    void createQuestion_SingleChoiceMultipleCorrect_ThrowsBadRequest() {
        CreateMcqRequest request = new CreateMcqRequest(
                "Invalid Single Choice",
                "Question stem",
                McqType.SINGLE_CHOICE,
                McqDifficulty.EASY,
                McqStatus.DRAFT,
                List.of("Java"),
                null,
                List.of(
                        new McqOptionRequest("opt_1", "Option 1", null),
                        new McqOptionRequest("opt_2", "Option 2", null)),
                List.of("opt_1", "opt_2"),
                null,
                1.0,
                0.0,
                true,
                null);

        assertThatThrownBy(() -> questionService.createQuestion(request, "admin-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Single choice question must have exactly one correct option");
    }

    @Test
    @DisplayName("createQuestion throws BadRequestException when correct option ID not found in options")
    void createQuestion_CorrectOptionNotFound_ThrowsBadRequest() {
        CreateMcqRequest request = new CreateMcqRequest(
                "Invalid Options",
                "Question stem",
                McqType.SINGLE_CHOICE,
                McqDifficulty.EASY,
                McqStatus.DRAFT,
                List.of("Java"),
                null,
                List.of(
                        new McqOptionRequest("opt_1", "Option 1", null),
                        new McqOptionRequest("opt_2", "Option 2", null)),
                List.of("opt_non_existent"),
                null,
                1.0,
                0.0,
                true,
                null);

        assertThatThrownBy(() -> questionService.createQuestion(request, "admin-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not match any provided option");
    }

    @Test
    @DisplayName("createQuestion successfully creates a MULTIPLE_CHOICE question with 2 correct answers")
    void createQuestion_MultipleChoice_Success() {
        CreateMcqRequest request = new CreateMcqRequest(
                "Java Immutable Collections",
                "Which methods create unmodifiable collections in modern Java?",
                McqType.MULTIPLE_CHOICE,
                McqDifficulty.EASY,
                McqStatus.PUBLISHED,
                List.of("Java"),
                null,
                List.of(
                        new McqOptionRequest("opt_1", "List.of()", "Creates unmodifiable list"),
                        new McqOptionRequest("opt_2", "new ArrayList<>()", "Creates mutable list"),
                        new McqOptionRequest("opt_3", "Set.of()", "Creates unmodifiable set")),
                List.of("opt_1", "opt_3"),
                "Factory methods List.of() and Set.of() return unmodifiable instances.",
                2.0,
                0.0,
                true,
                null);

        when(questionRepository.findBySlug(any())).thenReturn(Optional.empty());
        when(questionRepository.save(any(McqQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        McqQuestionDetailResponse response = questionService.createQuestion(request, "admin-1");

        assertThat(response).isNotNull();
        assertThat(response.type()).isEqualTo(McqType.MULTIPLE_CHOICE);
        assertThat(response.correctOptionIds()).containsExactlyInAnyOrder("opt_1", "opt_3");
    }

    @Test
    @DisplayName("getCandidateQuestionById strips answers and explanations")
    void getCandidateQuestionById_StripsSensitiveData() {
        when(questionRepository.findById("mcq-101")).thenReturn(Optional.of(sampleQuestion));

        CandidateMcqResponse candidateView = questionService.getCandidateQuestionById("mcq-101", false);

        assertThat(candidateView).isNotNull();
        assertThat(candidateView.id()).isEqualTo("mcq-101");
        assertThat(candidateView.title()).isEqualTo("Java Volatile Visibility");
        assertThat(candidateView.options()).hasSize(3);
        // Candidate option has text but no explanation
        assertThat(candidateView.options().get(0).text()).isEqualTo("Ensures atomicity and visibility");
    }

    @Test
    @DisplayName("updateQuestion updates details and adjusts topics")
    void updateQuestion_Success() {
        when(questionRepository.findById("mcq-101")).thenReturn(Optional.of(sampleQuestion));
        when(questionRepository.save(any(McqQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateMcqRequest updateRequest = new UpdateMcqRequest(
                "Java Volatile Visibility Updated",
                "Updated question content",
                McqType.SINGLE_CHOICE,
                McqDifficulty.HARD,
                McqStatus.PUBLISHED,
                List.of("Java", "JVM"), // Removed Concurrency, Added JVM
                List.of("CoreJava"),
                List.of(
                        new McqOptionRequest("opt_1", "Updated Opt 1", null),
                        new McqOptionRequest("opt_2", "Updated Opt 2", null)),
                List.of("opt_2"),
                "Updated explanation",
                3.0,
                1.0,
                false,
                null);

        McqQuestionDetailResponse updated = questionService.updateQuestion("mcq-101", updateRequest);

        assertThat(updated).isNotNull();
        assertThat(updated.title()).isEqualTo("Java Volatile Visibility Updated");
        assertThat(updated.difficulty()).isEqualTo(McqDifficulty.HARD);
        assertThat(updated.defaultPoints()).isEqualTo(3.0);

        verify(topicService).decrementQuestionCounts(eq(List.of("Concurrency")));
        verify(topicService).incrementQuestionCounts(eq(List.of("JVM")));
    }

    @Test
    @DisplayName("deleteQuestion removes question and decrements topic counts")
    void deleteQuestion_Success() {
        when(questionRepository.findById("mcq-101")).thenReturn(Optional.of(sampleQuestion));

        questionService.deleteQuestion("mcq-101");

        verify(questionRepository).delete(sampleQuestion);
        verify(topicService).decrementQuestionCounts(sampleQuestion.getTopics());
    }

    @Test
    @DisplayName("deleteQuestion should throw ConflictException when MCQ question is in use")
    void deleteQuestion_ThrowsConflictException_WhenInUse() {
        when(questionRepository.findById("mcq-101")).thenReturn(Optional.of(sampleQuestion));
        when(questionBankRepository.findAllByItemsQuestionId("mcq-101")).thenReturn(List.of(
                QuestionBank.builder().id("bank-1").title("Java Core Bank").build()
        ));
        when(assessmentRepository.findAllByItemsQuestionId("mcq-101")).thenReturn(List.of(
                Assessment.builder().id("asm-1").title("Senior Java Interview").build()
        ));

        assertThatThrownBy(() -> questionService.deleteQuestion("mcq-101"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot delete MCQ question 'Java Volatile Visibility'")
                .hasMessageContaining("Java Core Bank")
                .hasMessageContaining("Senior Java Interview");

        verify(questionRepository, org.mockito.Mockito.never()).delete(sampleQuestion);
    }

    @Test
    @DisplayName("getStats calculates aggregation counts properly")
    void getStats_Success() {
        when(questionRepository.count()).thenReturn(10L);
        when(questionRepository.countByStatus(McqStatus.PUBLISHED)).thenReturn(8L);
        when(questionRepository.countByStatus(McqStatus.DRAFT)).thenReturn(2L);
        when(questionRepository.countByType(McqType.SINGLE_CHOICE)).thenReturn(6L);
        when(questionRepository.countByType(McqType.MULTIPLE_CHOICE)).thenReturn(3L);
        when(questionRepository.countByType(McqType.BOOLEAN)).thenReturn(1L);
        when(questionRepository.countByDifficulty(McqDifficulty.EASY)).thenReturn(4L);
        when(questionRepository.countByDifficulty(McqDifficulty.MEDIUM)).thenReturn(4L);
        when(questionRepository.countByDifficulty(McqDifficulty.HARD)).thenReturn(2L);
        when(topicRepository.count()).thenReturn(5L);

        McqStatsResponse stats = questionService.getStats();

        assertThat(stats.totalQuestions()).isEqualTo(10L);
        assertThat(stats.publishedQuestions()).isEqualTo(8L);
        assertThat(stats.draftQuestions()).isEqualTo(2L);
        assertThat(stats.singleChoiceCount()).isEqualTo(6L);
        assertThat(stats.multipleChoiceCount()).isEqualTo(3L);
        assertThat(stats.booleanCount()).isEqualTo(1L);
        assertThat(stats.totalTopics()).isEqualTo(5L);
    }

    @Test
    @DisplayName("updateStatus should reject publishing when question has no content")
    void updateStatus_WhenNoContent_ThrowsBadRequest() {
        McqQuestion emptyContent = McqQuestion.builder()
                .id("mcq-no-content")
                .title("No Content")
                .status(McqStatus.DRAFT)
                .content("")
                .options(sampleQuestion.getOptions())
                .correctOptionIds(sampleQuestion.getCorrectOptionIds())
                .build();

        when(questionRepository.findById("mcq-no-content")).thenReturn(Optional.of(emptyContent));

        assertThatThrownBy(() -> questionService.updateStatus("mcq-no-content", McqStatus.PUBLISHED))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot publish MCQ question without question content.");
    }

    @Test
    @DisplayName("updateStatus, publish, unpublish should execute valid status transitions")
    void updateStatus_ValidTransitions_Success() {
        McqQuestion draft = McqQuestion.builder()
                .id("mcq-valid")
                .title("Valid MCQ")
                .slug("valid-mcq")
                .status(McqStatus.DRAFT)
                .content("Valid content")
                .options(sampleQuestion.getOptions())
                .correctOptionIds(sampleQuestion.getCorrectOptionIds())
                .build();

        when(questionRepository.findById("mcq-valid")).thenReturn(Optional.of(draft));
        when(questionRepository.save(any(McqQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 1. Publish
        McqQuestionDetailResponse pub = questionService.publish("mcq-valid");
        assertThat(pub.status()).isEqualTo(McqStatus.PUBLISHED);

        // 2. Unpublish
        McqQuestionDetailResponse unpub = questionService.unpublish("mcq-valid");
        assertThat(unpub.status()).isEqualTo(McqStatus.DRAFT);
    }
}
