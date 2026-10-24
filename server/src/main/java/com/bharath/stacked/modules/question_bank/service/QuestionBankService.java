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

    QuestionBankDetailResponse getQuestionBank(String id);

    QuestionBankSummaryResponse updateQuestionBank(String id, UpdateQuestionBankRequest request);

    void deleteQuestionBank(String id);

    QuestionBankSummaryResponse updateStatus(String id, QuestionBankStatus status);

    QuestionBankSummaryResponse publish(String id);

    QuestionBankSummaryResponse unpublish(String id);

    QuestionBankSummaryResponse duplicateQuestionBank(String id, String authorId);

    List<String> getDistinctCategories();

    QuestionBankStatsResponse getStats();

    void removeQuestionFromAllBanks(String questionId);

    boolean isInUse(String id);
}
