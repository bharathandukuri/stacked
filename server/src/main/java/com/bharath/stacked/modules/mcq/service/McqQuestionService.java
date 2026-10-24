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
    McqQuestionDetailResponse getQuestionById(@NonNull String id);

    default @NonNull McqQuestionDetailResponse getQuestionByIdOrSlug(@NonNull String idOrSlug) {
        return getQuestionById(idOrSlug);
    }

    @NonNull
    CandidateMcqResponse getCandidateQuestionById(@NonNull String id, boolean shuffle);

    @NonNull
    McqQuestionDetailResponse createQuestion(@NonNull CreateMcqRequest request, @Nullable String authorId);

    @NonNull
    McqQuestionDetailResponse updateQuestion(@NonNull String id, @NonNull UpdateMcqRequest request);

    void deleteQuestion(@NonNull String id);

    @NonNull
    McqQuestionDetailResponse updateStatus(@NonNull String id, @NonNull McqStatus status);

    @NonNull
    McqQuestionDetailResponse publish(@NonNull String id);

    @NonNull
    McqQuestionDetailResponse unpublish(@NonNull String id);

    @NonNull
    McqStatsResponse getStats();

    boolean isInUse(@NonNull String id);
}
