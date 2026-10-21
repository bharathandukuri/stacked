package com.bharath.stacked.modules.mcq.service;

import com.bharath.stacked.common.api.PageResponse;
import com.bharath.stacked.modules.mcq.dto.request.CreateMcqRequest;
import com.bharath.stacked.modules.mcq.dto.request.McqFilterRequest;
import com.bharath.stacked.modules.mcq.dto.request.UpdateMcqRequest;
import com.bharath.stacked.modules.mcq.dto.response.CandidateMcqResponse;
import com.bharath.stacked.modules.mcq.dto.response.McqQuestionDetailResponse;
import com.bharath.stacked.modules.mcq.dto.response.McqQuestionSummaryResponse;
import com.bharath.stacked.modules.mcq.dto.response.McqStatsResponse;
import com.bharath.stacked.modules.mcq.enums.McqStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface McqQuestionService {

    @NonNull
    PageResponse<McqQuestionSummaryResponse> listQuestions(@Nullable McqFilterRequest filter);

    @NonNull
    McqQuestionDetailResponse getQuestionByIdOrSlug(@NonNull String idOrSlug);

    @NonNull
    CandidateMcqResponse getCandidateQuestionById(@NonNull String id, boolean shuffle);

    @NonNull
    McqQuestionDetailResponse createQuestion(@NonNull CreateMcqRequest request, @Nullable String authorId);

    @NonNull
    McqQuestionDetailResponse updateQuestion(@NonNull String idOrSlug, @NonNull UpdateMcqRequest request);

    void deleteQuestion(@NonNull String idOrSlug);

    @NonNull
    McqQuestionDetailResponse updateStatus(@NonNull String idOrSlug, @NonNull McqStatus status);

    @NonNull
    McqQuestionDetailResponse publish(@NonNull String idOrSlug);

    @NonNull
    McqQuestionDetailResponse unpublish(@NonNull String idOrSlug);

    @NonNull
    McqQuestionDetailResponse archive(@NonNull String idOrSlug);

    @NonNull
    McqStatsResponse getStats();

    boolean isSlugAvailable(@NonNull String slug, @Nullable String excludeId);
}
