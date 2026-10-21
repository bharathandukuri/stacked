package com.bharath.stacked.modules.question_bank.listener;

import com.bharath.stacked.common.event.QuestionArchivedEvent;
import com.bharath.stacked.common.event.QuestionDeletedEvent;
import com.bharath.stacked.modules.question_bank.service.QuestionBankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionBankEventListener {

    private final QuestionBankService questionBankService;

    @EventListener
    public void onQuestionArchived(QuestionArchivedEvent event) {
        log.info("Handling QuestionArchivedEvent for question ID: {}", event.questionId());
        questionBankService.removeQuestionFromAllBanks(event.questionId());
    }

    @EventListener
    public void onQuestionDeleted(QuestionDeletedEvent event) {
        log.info("Handling QuestionDeletedEvent for question ID: {}", event.questionId());
        questionBankService.removeQuestionFromAllBanks(event.questionId());
    }
}

