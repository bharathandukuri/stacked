package com.bharath.stacked.modules.mcq.model;

import com.bharath.stacked.modules.mcq.enums.McqDifficulty;
import com.bharath.stacked.modules.mcq.enums.McqStatus;
import com.bharath.stacked.modules.mcq.enums.McqType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "mcq_questions")
@CompoundIndex(name = "status_topics_difficulty_idx", def = "{'status': 1, 'topics': 1, 'difficulty': 1}")
public class McqQuestion {

    @Id
    private String id;

    private String title;

    @Indexed(unique = true)
    private String slug;

    private String content;

    @Builder.Default
    private McqType type = McqType.SINGLE_CHOICE;

    @Builder.Default
    private McqDifficulty difficulty = McqDifficulty.MEDIUM;

    @Builder.Default
    private McqStatus status = McqStatus.DRAFT;

    @Builder.Default
    private List<String> topics = new ArrayList<>();

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Builder.Default
    private List<McqOption> options = new ArrayList<>();

    @Builder.Default
    private List<String> correctOptionIds = new ArrayList<>();

    private String explanation;

    @Builder.Default
    private double defaultPoints = 1.0;

    @Builder.Default
    private double negativePoints = 0.0;

    @Builder.Default
    private boolean shuffleOptions = true;

    @Builder.Default
    private List<String> imageFileIds = new ArrayList<>();

    private String authorId;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}

