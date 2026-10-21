package com.bharath.stacked.modules.mcq;

import com.bharath.stacked.AbstractIntegrationTest;
import com.bharath.stacked.common.api.PageResponse;
import com.bharath.stacked.modules.mcq.dto.request.CreateMcqRequest;
import com.bharath.stacked.modules.mcq.dto.request.CreateMcqTopicRequest;
import com.bharath.stacked.modules.mcq.dto.request.McqFilterRequest;
import com.bharath.stacked.modules.mcq.dto.request.McqOptionRequest;
import com.bharath.stacked.modules.mcq.dto.request.UpdateMcqRequest;
import com.bharath.stacked.modules.mcq.dto.response.McqQuestionDetailResponse;
import com.bharath.stacked.modules.mcq.dto.response.McqQuestionSummaryResponse;
import com.bharath.stacked.modules.mcq.dto.response.McqTopicResponse;
import com.bharath.stacked.modules.mcq.enums.McqDifficulty;
import com.bharath.stacked.modules.mcq.enums.McqStatus;
import com.bharath.stacked.modules.mcq.enums.McqType;
import com.bharath.stacked.modules.mcq.service.McqQuestionService;
import com.bharath.stacked.modules.mcq.service.McqTopicService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class McqIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private McqQuestionService questionService;

    @Autowired
    private McqTopicService topicService;

    @Test
    @DisplayName("Complete MCQ lifecycle: topic creation, question creation, topic count sync, filtering, updating, deletion")
    void mcqFullLifecycleIntegrationTest() {
        // 1. Create a topic
        String topicName = "Java Core Concurrency";
        McqTopicResponse topic = topicService.createTopic(new CreateMcqTopicRequest(topicName, "Threads and JMM"));
        assertThat(topic.id()).isNotNull();
        assertThat(topic.questionCount()).isEqualTo(0);

        // 2. Create an MCQ question referencing the topic
        CreateMcqRequest createReq = new CreateMcqRequest(
                "Java Virtual Threads Lifecycle",
                "How are Java 21+ Virtual Threads scheduled?",
                McqType.SINGLE_CHOICE,
                McqDifficulty.MEDIUM,
                McqStatus.PUBLISHED,
                List.of(topicName),
                List.of("Java21", "Concurrency"),
                List.of(
                        new McqOptionRequest("opt_1", "Directly 1:1 on OS threads", "Incorrect"),
                        new McqOptionRequest("opt_2", "M:N multiplexed onto carrier ForkJoinPool threads", "Correct"),
                        new McqOptionRequest("opt_3", "Executed via standard green thread bytecode emulation", "Incorrect")
                ),
                List.of("opt_2"),
                "Virtual threads are user-mode threads scheduled by the JVM onto carrier platform threads.",
                2.0,
                0.0,
                true,
                null
        );

        McqQuestionDetailResponse createdQuestion = questionService.createQuestion(createReq, "admin-test");
        assertThat(createdQuestion.id()).isNotNull();
        assertThat(createdQuestion.slug()).contains("java-virtual-threads-lifecycle");
        assertThat(createdQuestion.correctOptionIds()).containsExactly("opt_2");

        // Verify topic question count incremented to 1
        McqTopicResponse topicAfterCreate = topicService.getTopicById(topic.id());
        assertThat(topicAfterCreate.questionCount()).isEqualTo(1);

        // 3. Filter questions
        McqFilterRequest filter = McqFilterRequest.builder()
                .search("Virtual Threads")
                .topic(topicName)
                .type(McqType.SINGLE_CHOICE)
                .difficulty(McqDifficulty.MEDIUM)
                .page(0)
                .size(10)
                .build();

        PageResponse<McqQuestionSummaryResponse> pageResult = questionService.listQuestions(filter);
        assertThat(pageResult.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(pageResult.getItems()).anyMatch(q -> q.id().equals(createdQuestion.id()));

        // 4. Update question to add a new topic "Performance"
        String newTopicName = "JVM Performance";
        McqTopicResponse newTopic = topicService.createTopic(new CreateMcqTopicRequest(newTopicName, "Tuning"));

        UpdateMcqRequest updateReq = new UpdateMcqRequest(
                "Java Virtual Threads Lifecycle Updated",
                "How are Java 21+ Virtual Threads scheduled in modern JVMs?",
                McqType.SINGLE_CHOICE,
                McqDifficulty.HARD,
                McqStatus.PUBLISHED,
                List.of(newTopicName), // Changed topic from topicName to newTopicName
                List.of("Java21"),
                List.of(
                        new McqOptionRequest("opt_1", "Directly 1:1 on OS threads", null),
                        new McqOptionRequest("opt_2", "M:N multiplexed onto carrier threads", null)
                ),
                List.of("opt_2"),
                "Explanation updated",
                3.0,
                0.5,
                false,
                null
        );

        McqQuestionDetailResponse updatedQuestion = questionService.updateQuestion(createdQuestion.id(), updateReq);
        assertThat(updatedQuestion.difficulty()).isEqualTo(McqDifficulty.HARD);
        assertThat(updatedQuestion.topics()).containsExactly(newTopicName);

        // Verify old topic decremented and new topic incremented
        assertThat(topicService.getTopicById(topic.id()).questionCount()).isEqualTo(0);
        assertThat(topicService.getTopicById(newTopic.id()).questionCount()).isEqualTo(1);

        // 5. Delete question
        questionService.deleteQuestion(createdQuestion.id());
        assertThat(topicService.getTopicById(newTopic.id()).questionCount()).isEqualTo(0);

        // Clean up created topics
        topicService.deleteTopic(topic.id());
        topicService.deleteTopic(newTopic.id());
    }
}
