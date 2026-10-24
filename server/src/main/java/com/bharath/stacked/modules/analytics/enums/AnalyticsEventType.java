package com.bharath.stacked.modules.analytics.enums;

public enum AnalyticsEventType {
    // Content Lifecycle - Coding Problems
    CODING_PROBLEM_CREATED,
    CODING_PROBLEM_UPDATED,
    CODING_PROBLEM_PUBLISHED,
    CODING_PROBLEM_UNPUBLISHED,
    CODING_PROBLEM_DELETED,

    // Content Lifecycle - MCQs
    MCQ_CREATED,
    MCQ_UPDATED,
    MCQ_PUBLISHED,
    MCQ_UNPUBLISHED,
    MCQ_DELETED,

    // Content Lifecycle - Question Banks
    QUESTION_BANK_CREATED,
    QUESTION_BANK_UPDATED,
    QUESTION_BANK_PUBLISHED,
    QUESTION_BANK_UNPUBLISHED,
    QUESTION_BANK_DELETED,

    // Assessment Lifecycle
    ASSESSMENT_CREATED,
    ASSESSMENT_ACTIVATED,
    ASSESSMENT_COMPLETED,
    ASSESSMENT_DELETED,
    CANDIDATE_INVITED,
    CANDIDATE_ENROLLED,

    // Candidate Participation & Exam Flow
    ATTEMPT_STARTED,
    ANSWER_SAVED,
    ATTEMPT_SUBMITTED,
    ATTEMPT_EXPIRED,

    // Code Execution & Submission Judging
    CODE_SUBMISSION_EVALUATED,
    SAMPLE_RUN_EXECUTED
}
