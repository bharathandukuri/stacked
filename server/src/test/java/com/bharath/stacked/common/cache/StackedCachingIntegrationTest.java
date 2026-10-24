package com.bharath.stacked.common.cache;

import com.bharath.stacked.AbstractIntegrationTest;
import com.bharath.stacked.common.api.PageResponse;
import com.bharath.stacked.modules.analytics.dto.PlatformOverviewResponse;
import com.bharath.stacked.modules.analytics.service.AdminAnalyticsService;
import com.bharath.stacked.modules.coding_problem.dto.request.CodingProblemFilterRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.CreateCodingProblemRequest;
import com.bharath.stacked.modules.coding_problem.dto.response.CodingProblemSummaryResponse;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemDifficulty;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemStatus;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import com.bharath.stacked.modules.coding_problem.model.CodingProblem;
import com.bharath.stacked.modules.coding_problem.repository.CodingProblemRepository;
import com.bharath.stacked.modules.coding_problem.service.CodingProblemService;
import com.bharath.stacked.modules.mcq.dto.response.McqStatsResponse;
import com.bharath.stacked.modules.mcq.service.McqQuestionService;
import com.bharath.stacked.modules.question_bank.dto.CreateQuestionBankRequest;
import com.bharath.stacked.modules.question_bank.dto.QuestionBankStatsResponse;
import com.bharath.stacked.modules.question_bank.dto.QuestionBankSummaryResponse;
import com.bharath.stacked.modules.question_bank.service.QuestionBankService;
import com.bharath.stacked.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Stacked Caching & Invalidation Integration Tests")
class StackedCachingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AdminAnalyticsService analyticsService;

    @Autowired
    private CodingProblemService codingProblemService;

    @Autowired
    private CodingProblemRepository codingProblemRepository;

    @Autowired
    private McqQuestionService mcqQuestionService;

    @Autowired
    private QuestionBankService questionBankService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        var factory = stringRedisTemplate.getConnectionFactory();
        if (factory != null) {
            try (var connection = factory.getConnection()) {
                connection.serverCommands().flushDb();
            }
        }
    }

    @Test
    @DisplayName("Platform overview is cached in Redis on first request and served on subsequent requests")
    void testPlatformOverviewCaching() {
        String cacheKey = CacheKeys.analyticsOverview();
        assertThat(redisService.hasKey(cacheKey)).isFalse();

        PlatformOverviewResponse response1 = analyticsService.getPlatformOverview();
        assertThat(response1).isNotNull();
        assertThat(redisService.hasKey(cacheKey)).isTrue();

        PlatformOverviewResponse cached = redisService.get(cacheKey, PlatformOverviewResponse.class);
        assertThat(cached).isNotNull();
        assertThat(cached.getTotalCodingProblems()).isEqualTo(response1.getTotalCodingProblems());

        PlatformOverviewResponse response2 = analyticsService.getPlatformOverview();
        assertThat(response2.getTotalCodingProblems()).isEqualTo(cached.getTotalCodingProblems());
    }

    @Test
    @DisplayName("Coding problem default catalog page is cached, non-default search bypasses default cache, and mutation invalidates")
    void testCodingProblemCachingAndInvalidation() {
        CodingProblem problem = new CodingProblem();
        problem.setTitle("Cache Coding Problem");
        problem.setSlug("cache-coding-problem-" + System.currentTimeMillis());
        problem.setProblemType(CodingProblemType.GENERIC);
        problem.setDifficulty(CodingProblemDifficulty.EASY);
        problem.setStatus(CodingProblemStatus.PUBLISHED);
        problem = codingProblemRepository.save(problem);

        String defaultPageKey = CacheKeys.codingProblemDefaultPage();
        assertThat(redisService.hasKey(defaultPageKey)).isFalse();

        // 1. Request default page -> should populate cache
        CodingProblemFilterRequest defaultFilter = new CodingProblemFilterRequest();
        PageResponse<CodingProblemSummaryResponse> page1 = codingProblemService.findAll(defaultFilter);
        assertThat(page1).isNotNull();
        assertThat(redisService.hasKey(defaultPageKey)).isTrue();

        // 2. Request non-default filtered request (search) -> should NOT hit default cache or pollute it
        CodingProblemFilterRequest searchFilter = CodingProblemFilterRequest.builder()
                .search("nonexistent-problem-xyz")
                .build();
        PageResponse<CodingProblemSummaryResponse> searchPage = codingProblemService.findAll(searchFilter);
        assertThat(searchPage.getItems()).isEmpty();
        // Default cache still holds the default page
        assertThat(redisService.hasKey(defaultPageKey)).isTrue();

        // 3. Mutate: update problem status -> must invalidate default page and analytics overview
        codingProblemService.updateStatus(problem.getId(), CodingProblemStatus.DRAFT);

        // Cache keys must be evicted immediately
        assertThat(redisService.hasKey(defaultPageKey)).isFalse();
        assertThat(redisService.hasKey(CacheKeys.analyticsOverview())).isFalse();
    }

    @Test
    @DisplayName("Question bank statistics and distinct categories are cached and invalidated on creation")
    void testQuestionBankCachingAndInvalidation() {
        String statsKey = CacheKeys.questionBankStats();
        String categoriesKey = CacheKeys.questionBankCategories();
        assertThat(redisService.hasKey(statsKey)).isFalse();
        assertThat(redisService.hasKey(categoriesKey)).isFalse();

        QuestionBankStatsResponse stats = questionBankService.getStats();
        assertThat(stats).isNotNull();
        assertThat(redisService.hasKey(statsKey)).isTrue();

        List<String> categories = questionBankService.getDistinctCategories();
        assertThat(categories).isNotNull();
        assertThat(redisService.hasKey(categoriesKey)).isTrue();

        // Mutate: create a question bank -> evicts stats and categories
        CreateQuestionBankRequest request = CreateQuestionBankRequest.builder()
                .title("Cache Integration Question Bank")
                .category("Database Design")
                .items(List.of())
                .build();
        QuestionBankSummaryResponse created = questionBankService.createQuestionBank(request, "admin");
        assertThat(created).isNotNull();

        assertThat(redisService.hasKey(statsKey)).isFalse();
        assertThat(redisService.hasKey(categoriesKey)).isFalse();
    }

    @Test
    @DisplayName("MCQ statistics are cached in Redis")
    void testMcqStatsCaching() {
        String statsKey = CacheKeys.mcqStats();
        assertThat(redisService.hasKey(statsKey)).isFalse();

        McqStatsResponse stats1 = mcqQuestionService.getStats();
        assertThat(stats1).isNotNull();
        assertThat(redisService.hasKey(statsKey)).isTrue();

        McqStatsResponse cached = redisService.get(statsKey, McqStatsResponse.class);
        assertThat(cached).isNotNull();
        assertThat(cached.totalQuestions()).isEqualTo(stats1.totalQuestions());
    }
}
