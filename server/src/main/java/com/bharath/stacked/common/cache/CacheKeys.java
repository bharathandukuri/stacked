package com.bharath.stacked.common.cache;

import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;

import java.util.Locale;

/**
 * Centralized, strongly typed Redis cache key definitions and builders for Stacked.
 *
 * <p>Follows standard colon-separated namespacing (e.g. {@code cache:<namespace>:<entity>[:<id>]})
 * ensuring zero key collisions, deterministic resolution, and discoverable key ownership.</p>
 */
public final class CacheKeys {

    private CacheKeys() {}

    public static final String ROOT_PREFIX = "cache:";

    // -------------------------------------------------------------------------
    // Analytics
    // -------------------------------------------------------------------------
    public static final String ANALYTICS_PREFIX = ROOT_PREFIX + "analytics:";
    public static final String ANALYTICS_OVERVIEW = ANALYTICS_PREFIX + "overview";
    public static final String ANALYTICS_CONTENT_DEFAULT = ANALYTICS_PREFIX + "content:default";
    public static final String ANALYTICS_ASSESSMENTS_DEFAULT = ANALYTICS_PREFIX + "assessments:default";
    public static final String ANALYTICS_SUBMISSIONS_DEFAULT = ANALYTICS_PREFIX + "submissions:default";

    public static String analyticsOverview() {
        return ANALYTICS_OVERVIEW;
    }

    public static String analyticsContentDefault() {
        return ANALYTICS_CONTENT_DEFAULT;
    }

    public static String analyticsAssessmentsDefault() {
        return ANALYTICS_ASSESSMENTS_DEFAULT;
    }

    public static String analyticsSubmissionsDefault() {
        return ANALYTICS_SUBMISSIONS_DEFAULT;
    }

    // -------------------------------------------------------------------------
    // Coding Problems
    // -------------------------------------------------------------------------
    public static final String CODING_PROBLEM_PREFIX = ROOT_PREFIX + "coding-problem:";
    public static final String CODING_PROBLEM_DEFAULT_PAGE = CODING_PROBLEM_PREFIX + "default-page";
    public static final String CODING_PROBLEM_DETAIL_PREFIX = CODING_PROBLEM_PREFIX + "detail:";
    public static final String CODING_PROBLEM_LANGUAGES_PREFIX = CODING_PROBLEM_PREFIX + "languages:";

    public static String codingProblemDefaultPage() {
        return CODING_PROBLEM_DEFAULT_PAGE;
    }

    public static String codingProblemDetail(String idOrSlug) {
        return CODING_PROBLEM_DETAIL_PREFIX + normalizeKey(idOrSlug);
    }

    public static String codingProblemSupportedLanguages(CodingProblemType problemType) {
        return CODING_PROBLEM_LANGUAGES_PREFIX + (problemType == null ? "all" : problemType.name().toLowerCase(Locale.ROOT));
    }

    // -------------------------------------------------------------------------
    // MCQ Questions & Topics
    // -------------------------------------------------------------------------
    public static final String MCQ_PREFIX = ROOT_PREFIX + "mcq:";
    public static final String MCQ_DEFAULT_PAGE = MCQ_PREFIX + "default-page";
    public static final String MCQ_DETAIL_PREFIX = MCQ_PREFIX + "detail:";
    public static final String MCQ_STATS = MCQ_PREFIX + "stats";
    public static final String MCQ_TOPICS = MCQ_PREFIX + "topics";

    public static String mcqDefaultPage() {
        return MCQ_DEFAULT_PAGE;
    }

    public static String mcqDetail(String id) {
        return MCQ_DETAIL_PREFIX + normalizeKey(id);
    }

    public static String mcqStats() {
        return MCQ_STATS;
    }

    public static String mcqTopics() {
        return MCQ_TOPICS;
    }

    // -------------------------------------------------------------------------
    // Question Banks
    // -------------------------------------------------------------------------
    public static final String QUESTION_BANK_PREFIX = ROOT_PREFIX + "question-bank:";
    public static final String QUESTION_BANK_DEFAULT_PAGE = QUESTION_BANK_PREFIX + "default-page";
    public static final String QUESTION_BANK_DETAIL_PREFIX = QUESTION_BANK_PREFIX + "detail:";
    public static final String QUESTION_BANK_CATEGORIES = QUESTION_BANK_PREFIX + "categories";
    public static final String QUESTION_BANK_STATS = QUESTION_BANK_PREFIX + "stats";

    public static String questionBankDefaultPage() {
        return QUESTION_BANK_DEFAULT_PAGE;
    }

    public static String questionBankDetail(String id) {
        return QUESTION_BANK_DETAIL_PREFIX + normalizeKey(id);
    }

    public static String questionBankCategories() {
        return QUESTION_BANK_CATEGORIES;
    }

    public static String questionBankStats() {
        return QUESTION_BANK_STATS;
    }

    // -------------------------------------------------------------------------
    // Assessments
    // -------------------------------------------------------------------------
    public static final String ASSESSMENT_PREFIX = ROOT_PREFIX + "assessment:";
    public static final String ASSESSMENT_DEFAULT_PAGE = ASSESSMENT_PREFIX + "default-page";
    public static final String ASSESSMENT_DETAIL_PREFIX = ASSESSMENT_PREFIX + "detail:";
    public static final String ASSESSMENT_METRICS_PREFIX = ASSESSMENT_PREFIX + "metrics:";
    public static final String ASSESSMENT_BANK_PREVIEW_PREFIX = ASSESSMENT_PREFIX + "bank-preview:";

    public static String assessmentDefaultPage() {
        return ASSESSMENT_DEFAULT_PAGE;
    }

    public static String assessmentDetail(String idOrSlug) {
        return ASSESSMENT_DETAIL_PREFIX + normalizeKey(idOrSlug);
    }

    public static String assessmentMetrics(String idOrSlug) {
        return ASSESSMENT_METRICS_PREFIX + normalizeKey(idOrSlug);
    }

    public static String assessmentBankPreview(String bankId) {
        return ASSESSMENT_BANK_PREVIEW_PREFIX + normalizeKey(bankId);
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------
    private static String normalizeKey(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Cache key component cannot be null or blank");
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}

