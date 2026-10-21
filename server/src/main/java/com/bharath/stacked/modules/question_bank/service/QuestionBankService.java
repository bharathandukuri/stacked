package com.bharath.stacked.modules.question_bank.service;

import com.bharath.stacked.common.api.PageResponse;
import com.bharath.stacked.modules.question_bank.dto.CreateQuestionBankRequest;
import com.bharath.stacked.modules.question_bank.dto.QuestionBankDetailResponse;
import com.bharath.stacked.modules.question_bank.dto.QuestionBankFilterRequest;
import com.bharath.stacked.modules.question_bank.dto.QuestionBankStatsResponse;
import com.bharath.stacked.modules.question_bank.dto.QuestionBankSummaryResponse;
import com.bharath.stacked.modules.question_bank.dto.UpdateQuestionBankRequest;
import com.bharath.stacked.modules.question_bank.enums.QuestionBankStatus;

import java.util.List;

public interface QuestionBankService {

    QuestionBankSummaryResponse createQuestionBank(CreateQuestionBankRequest request, String authorId);

    PageResponse<QuestionBankSummaryResponse> getQuestionBanks(QuestionBankFilterRequest filter);

    QuestionBankDetailResponse getQuestionBank(String idOrSlug);

    QuestionBankSummaryResponse updateQuestionBank(String idOrSlug, UpdateQuestionBankRequest request);

    void deleteQuestionBank(String idOrSlug);

    QuestionBankSummaryResponse updateStatus(String idOrSlug, QuestionBankStatus status);

    QuestionBankSummaryResponse publish(String idOrSlug);

    QuestionBankSummaryResponse unpublish(String idOrSlug);

    QuestionBankSummaryResponse archive(String idOrSlug);

    QuestionBankSummaryResponse duplicateQuestionBank(String idOrSlug, String authorId);

    List<String> getDistinctCategories();

    QuestionBankStatsResponse getStats();

    void removeQuestionFromAllBanks(String questionId);
}
