package com.bharath.stacked.modules.mcq.controller;

import com.bharath.stacked.common.api.PageResponse;
import com.bharath.stacked.exception.GlobalExceptionHandler;
import com.bharath.stacked.modules.mcq.dto.request.CreateMcqRequest;
import com.bharath.stacked.modules.mcq.dto.request.CreateMcqTopicRequest;
import com.bharath.stacked.modules.mcq.dto.request.McqOptionRequest;
import com.bharath.stacked.modules.mcq.dto.request.UpdateMcqStatusRequest;
import com.bharath.stacked.modules.mcq.dto.response.McqOptionResponse;
import com.bharath.stacked.modules.mcq.dto.response.McqQuestionDetailResponse;
import com.bharath.stacked.modules.mcq.dto.response.McqQuestionSummaryResponse;
import com.bharath.stacked.modules.mcq.dto.response.McqStatsResponse;
import com.bharath.stacked.modules.mcq.dto.response.McqTopicResponse;
import com.bharath.stacked.modules.mcq.enums.McqDifficulty;
import com.bharath.stacked.modules.mcq.enums.McqStatus;
import com.bharath.stacked.modules.mcq.enums.McqType;
import com.bharath.stacked.modules.mcq.service.McqQuestionService;
import com.bharath.stacked.modules.mcq.service.McqTopicService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminMcqControllerTest {

    private MockMvc mockMvc;

    @Mock
    private McqQuestionService questionService;

    @Mock
    private McqTopicService topicService;

    @InjectMocks
    private AdminMcqController controller;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/admin/mcq/questions returns 200 with paginated questions")
    void listQuestions_Success() throws Exception {
        McqQuestionSummaryResponse summary = new McqQuestionSummaryResponse(
                "q-1",
                "Java Concurrency",
                "Snippet",
                McqType.SINGLE_CHOICE,
                McqDifficulty.MEDIUM,
                McqStatus.PUBLISHED,
                List.of("Java"),
                List.of(),
                3,
                1,
                Instant.now(),
                Instant.now());
        PageResponse<McqQuestionSummaryResponse> page = PageResponse.of(List.of(summary), 0, 20, 1);

        when(questionService.listQuestions(any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/mcq/questions")
                .param("search", "Java")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].id").value("q-1"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/admin/mcq/questions/{id} returns 200 with detail")
    void getQuestion_Success() throws Exception {
        McqQuestionDetailResponse detail = new McqQuestionDetailResponse(
                "q-1",
                "Java Concurrency",
                "Full question content",
                McqType.SINGLE_CHOICE,
                McqDifficulty.MEDIUM,
                McqStatus.PUBLISHED,
                List.of("Java"),
                List.of(),
                List.of(new McqOptionResponse("opt-1", "Text", null)),
                List.of("opt-1"),
                "Explanation",
                true,
                List.of(),
                "admin-1",
                Instant.now(),
                Instant.now());

        when(questionService.getQuestionById("q-1")).thenReturn(detail);

        mockMvc.perform(get("/api/admin/mcq/questions/q-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("q-1"))
                .andExpect(jsonPath("$.data.title").value("Java Concurrency"));
    }

    @Test
    @DisplayName("POST /api/admin/mcq/questions returns 201 Created on valid payload")
    void createQuestion_Success() throws Exception {
        CreateMcqRequest request = new CreateMcqRequest(
                "New Question",
                "Content stem",
                McqType.SINGLE_CHOICE,
                McqDifficulty.EASY,
                McqStatus.DRAFT,
                List.of("Java"),
                null,
                List.of(
                        new McqOptionRequest("opt_1", "Option A", null),
                        new McqOptionRequest("opt_2", "Option B", null)),
                List.of("opt_1"),
                "Explanation",
                true,
                null);

        McqQuestionDetailResponse created = new McqQuestionDetailResponse(
                "created-id",
                "New Question",
                "Content stem",
                McqType.SINGLE_CHOICE,
                McqDifficulty.EASY,
                McqStatus.DRAFT,
                List.of("Java"),
                List.of(),
                List.of(new McqOptionResponse("opt_1", "Option A", null)),
                List.of("opt_1"),
                "Explanation",
                true,
                List.of(),
                null,
                Instant.now(),
                Instant.now());

        when(questionService.createQuestion(any(), any())).thenReturn(created);

        mockMvc.perform(post("/api/admin/mcq/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("created-id"));
    }

    @Test
    @DisplayName("POST /api/admin/mcq/questions returns 422 Unprocessable Entity when validation fails")
    void createQuestion_ValidationFailure_Returns422() throws Exception {
        // Missing title and options
        String invalidJson = """
                {
                    "content": "Stem",
                    "type": "SINGLE_CHOICE",
                    "difficulty": "EASY"
                }
                """;

        mockMvc.perform(post("/api/admin/mcq/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("DELETE /api/admin/mcq/questions/{id} returns 200")
    void deleteQuestion_Success() throws Exception {
        mockMvc.perform(delete("/api/admin/mcq/questions/q-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(questionService).deleteQuestion("q-1");
    }

    @Test
    @DisplayName("GET /api/admin/mcq/stats returns 200 with metrics")
    void getStats_Success() throws Exception {
        McqStatsResponse stats = new McqStatsResponse(10, 8, 2, 6, 3, 1, 4, 4, 2, 5);
        when(questionService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/admin/mcq/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalQuestions").value(10))
                .andExpect(jsonPath("$.data.publishedQuestions").value(8));
    }

    @Test
    @DisplayName("POST /api/admin/mcq/topics creates topic and returns 201 Created")
    void createTopic_Success() throws Exception {
        CreateMcqTopicRequest request = new CreateMcqTopicRequest("Spring Boot", "Enterprise framework");
        McqTopicResponse topic = new McqTopicResponse("topic-1", "Spring Boot", "spring-boot", "Enterprise framework",
                0, Instant.now(), Instant.now());

        when(topicService.createTopic(any())).thenReturn(topic);

        mockMvc.perform(post("/api/admin/mcq/topics")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Spring Boot"));
    }

    @Test
    @DisplayName("PATCH /api/admin/mcq/questions/{id}/status updates status and returns 200")
    void updateQuestionStatus_Success() throws Exception {
        UpdateMcqStatusRequest request = new UpdateMcqStatusRequest(McqStatus.PUBLISHED);
        McqQuestionDetailResponse detail = new McqQuestionDetailResponse(
                "q-1", "Java Concurrency", "Content",
                McqType.SINGLE_CHOICE, McqDifficulty.MEDIUM, McqStatus.PUBLISHED,
                List.of("Java"), List.of(), List.of(), List.of(), "Exp", true, List.of(),
                "admin-1", Instant.now(), Instant.now()
        );
        when(questionService.updateStatus("q-1", McqStatus.PUBLISHED)).thenReturn(detail);

        mockMvc.perform(patch("/api/admin/mcq/questions/q-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        verify(questionService).updateStatus("q-1", McqStatus.PUBLISHED);
    }

    @Test
    @DisplayName("PATCH /api/admin/mcq/questions/{id}/publish publishes question and returns 200")
    void publishQuestion_Success() throws Exception {
        McqQuestionDetailResponse detail = new McqQuestionDetailResponse(
                "q-1", "Java Concurrency", "Content",
                McqType.SINGLE_CHOICE, McqDifficulty.MEDIUM, McqStatus.PUBLISHED,
                List.of("Java"), List.of(), List.of(), List.of(), "Exp", true, List.of(),
                "admin-1", Instant.now(), Instant.now()
        );
        when(questionService.publish("q-1")).thenReturn(detail);

        mockMvc.perform(patch("/api/admin/mcq/questions/q-1/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("q-1"));

        verify(questionService).publish("q-1");
    }

    @Test
    @DisplayName("PATCH /api/admin/mcq/questions/{id}/unpublish unpublishes question and returns 200")
    void unpublishQuestion_Success() throws Exception {
        McqQuestionDetailResponse detail = new McqQuestionDetailResponse(
                "q-1", "Java Concurrency", "Content",
                McqType.SINGLE_CHOICE, McqDifficulty.MEDIUM, McqStatus.DRAFT,
                List.of("Java"), List.of(), List.of(), List.of(), "Exp", true, List.of(),
                "admin-1", Instant.now(), Instant.now()
        );
        when(questionService.unpublish("q-1")).thenReturn(detail);

        mockMvc.perform(patch("/api/admin/mcq/questions/q-1/unpublish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        verify(questionService).unpublish("q-1");
    }

    @Test
    @DisplayName("GET /api/admin/mcq/questions/{id}/in-use returns whether question is in use")
    void isInUse_Success() throws Exception {
        when(questionService.isInUse("q-1")).thenReturn(true);

        mockMvc.perform(get("/api/admin/mcq/questions/q-1/in-use"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.inUse").value(true));

        verify(questionService).isInUse("q-1");
    }
}
