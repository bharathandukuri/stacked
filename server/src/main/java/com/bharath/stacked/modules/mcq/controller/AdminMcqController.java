package com.bharath.stacked.modules.mcq.controller;

import com.bharath.stacked.common.api.ApiResponse;
import com.bharath.stacked.common.api.PageResponse;
import com.bharath.stacked.modules.mcq.dto.request.CreateMcqRequest;
import com.bharath.stacked.modules.mcq.dto.request.CreateMcqTopicRequest;
import com.bharath.stacked.modules.mcq.dto.request.McqFilterRequest;
import com.bharath.stacked.modules.mcq.dto.request.UpdateMcqRequest;
import com.bharath.stacked.modules.mcq.dto.request.UpdateMcqStatusRequest;
import com.bharath.stacked.modules.mcq.dto.request.UpdateMcqTopicRequest;
import com.bharath.stacked.modules.mcq.dto.response.McqQuestionDetailResponse;
import com.bharath.stacked.modules.mcq.dto.response.McqQuestionSummaryResponse;
import com.bharath.stacked.modules.mcq.dto.response.McqStatsResponse;
import com.bharath.stacked.modules.mcq.dto.response.McqTopicResponse;
import com.bharath.stacked.modules.mcq.service.McqQuestionService;
import com.bharath.stacked.modules.mcq.service.McqTopicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/mcq")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMcqController {

    private final McqQuestionService questionService;
    private final McqTopicService topicService;

    // --- Questions Endpoints ---

    @GetMapping("/questions")
    @NonNull
    public ResponseEntity<ApiResponse<PageResponse<McqQuestionSummaryResponse>>> listQuestions(
            @ModelAttribute McqFilterRequest filter) {
        PageResponse<McqQuestionSummaryResponse> response = questionService.listQuestions(filter);
        return ApiResponse.success(response, "MCQ questions retrieved successfully");
    }

    @GetMapping("/questions/{idOrSlug}")
    @NonNull
    public ResponseEntity<ApiResponse<McqQuestionDetailResponse>> getQuestion(
            @PathVariable @NonNull String idOrSlug) {
        McqQuestionDetailResponse response = questionService.getQuestionByIdOrSlug(idOrSlug);
        return ApiResponse.success(response, "MCQ question retrieved successfully");
    }

    @PostMapping("/questions")
    @NonNull
    public ResponseEntity<ApiResponse<McqQuestionDetailResponse>> createQuestion(
            @Valid @RequestBody @NonNull CreateMcqRequest request,
            @Nullable Authentication authentication) {
        String authorId = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName()
                : null;
        McqQuestionDetailResponse response = questionService.createQuestion(request, authorId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "MCQ question created successfully").getBody());
    }

    @PutMapping("/questions/{idOrSlug}")
    @NonNull
    public ResponseEntity<ApiResponse<McqQuestionDetailResponse>> updateQuestion(
            @PathVariable @NonNull String idOrSlug,
            @Valid @RequestBody @NonNull UpdateMcqRequest request) {
        McqQuestionDetailResponse response = questionService.updateQuestion(idOrSlug, request);
        return ApiResponse.success(response, "MCQ question updated successfully");
    }

    @DeleteMapping("/questions/{idOrSlug}")
    @NonNull
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            @PathVariable @NonNull String idOrSlug) {
        questionService.deleteQuestion(idOrSlug);
        return ApiResponse.success(null, "MCQ question deleted successfully");
    }

    @PatchMapping("/questions/{idOrSlug}/status")
    @NonNull
    public ResponseEntity<ApiResponse<McqQuestionDetailResponse>> updateQuestionStatus(
            @PathVariable @NonNull String idOrSlug,
            @Valid @RequestBody @NonNull UpdateMcqStatusRequest request) {
        McqQuestionDetailResponse response = questionService.updateStatus(idOrSlug, request.status());
        return ApiResponse.success(response, "MCQ question status updated successfully");
    }

    @PatchMapping("/questions/{idOrSlug}/publish")
    @NonNull
    public ResponseEntity<ApiResponse<McqQuestionDetailResponse>> publishQuestion(
            @PathVariable @NonNull String idOrSlug) {
        McqQuestionDetailResponse response = questionService.publish(idOrSlug);
        return ApiResponse.success(response, "MCQ question published successfully");
    }

    @PatchMapping("/questions/{idOrSlug}/unpublish")
    @NonNull
    public ResponseEntity<ApiResponse<McqQuestionDetailResponse>> unpublishQuestion(
            @PathVariable @NonNull String idOrSlug) {
        McqQuestionDetailResponse response = questionService.unpublish(idOrSlug);
        return ApiResponse.success(response, "MCQ question unpublished successfully");
    }

    @PatchMapping("/questions/{idOrSlug}/archive")
    @NonNull
    public ResponseEntity<ApiResponse<McqQuestionDetailResponse>> archiveQuestion(
            @PathVariable @NonNull String idOrSlug) {
        McqQuestionDetailResponse response = questionService.archive(idOrSlug);
        return ApiResponse.success(response, "MCQ question archived successfully");
    }

    @GetMapping("/questions/check-slug")
    @NonNull
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkSlug(
            @RequestParam @NonNull String slug,
            @RequestParam(required = false) @Nullable String excludeId) {
        boolean available = questionService.isSlugAvailable(slug, excludeId);
        return ApiResponse.success(Map.of("available", available), "Slug availability checked");
    }

    @GetMapping("/stats")
    @NonNull
    public ResponseEntity<ApiResponse<McqStatsResponse>> getStats() {
        McqStatsResponse stats = questionService.getStats();
        return ApiResponse.success(stats, "MCQ stats retrieved successfully");
    }

    // --- Topics Endpoints ---

    @GetMapping("/topics")
    @NonNull
    public ResponseEntity<ApiResponse<List<McqTopicResponse>>> listTopics() {
        List<McqTopicResponse> topics = topicService.listTopics();
        return ApiResponse.success(topics, "MCQ topics retrieved successfully");
    }

    @GetMapping("/topics/{id}")
    @NonNull
    public ResponseEntity<ApiResponse<McqTopicResponse>> getTopic(
            @PathVariable @NonNull String id) {
        McqTopicResponse topic = topicService.getTopicById(id);
        return ApiResponse.success(topic, "MCQ topic retrieved successfully");
    }

    @PostMapping("/topics")
    @NonNull
    public ResponseEntity<ApiResponse<McqTopicResponse>> createTopic(
            @Valid @RequestBody @NonNull CreateMcqTopicRequest request) {
        McqTopicResponse topic = topicService.createTopic(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(topic, "MCQ topic created successfully").getBody());
    }

    @PutMapping("/topics/{id}")
    @NonNull
    public ResponseEntity<ApiResponse<McqTopicResponse>> updateTopic(
            @PathVariable @NonNull String id,
            @Valid @RequestBody @NonNull UpdateMcqTopicRequest request) {
        McqTopicResponse topic = topicService.updateTopic(id, request);
        return ApiResponse.success(topic, "MCQ topic updated successfully");
    }

    @DeleteMapping("/topics/{id}")
    @NonNull
    public ResponseEntity<ApiResponse<Void>> deleteTopic(
            @PathVariable @NonNull String id) {
        topicService.deleteTopic(id);
        return ApiResponse.success(null, "MCQ topic deleted successfully");
    }
}

