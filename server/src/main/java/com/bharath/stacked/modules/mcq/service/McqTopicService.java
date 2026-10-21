package com.bharath.stacked.modules.mcq.service;

import com.bharath.stacked.modules.mcq.dto.request.CreateMcqTopicRequest;
import com.bharath.stacked.modules.mcq.dto.request.UpdateMcqTopicRequest;
import com.bharath.stacked.modules.mcq.dto.response.McqTopicResponse;
import org.jspecify.annotations.NonNull;

import java.util.List;

public interface McqTopicService {

    @NonNull
    List<McqTopicResponse> listTopics();

    @NonNull
    McqTopicResponse getTopicById(@NonNull String id);

    @NonNull
    McqTopicResponse createTopic(@NonNull CreateMcqTopicRequest request);

    @NonNull
    McqTopicResponse updateTopic(@NonNull String id, @NonNull UpdateMcqTopicRequest request);

    void deleteTopic(@NonNull String id);

    void incrementQuestionCounts(@NonNull List<String> topicNames);

    void decrementQuestionCounts(@NonNull List<String> topicNames);
}
