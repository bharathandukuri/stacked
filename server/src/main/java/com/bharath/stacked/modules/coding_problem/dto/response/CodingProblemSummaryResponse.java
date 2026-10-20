package com.bharath.stacked.modules.coding_problem.dto.response;

import com.bharath.stacked.modules.coding_problem.enums.CodingProblemDifficulty;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemStatus;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import com.bharath.stacked.modules.coding_problem.model.CodingProblem;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class CodingProblemSummaryResponse {

    private String id;
    private String title;
    private String slug;
    private CodingProblemStatus status;
    private CodingProblemType problemType;
    private CodingProblemDifficulty difficulty;
    private List<String> supportedLanguageIds;
    private List<String> topics;
    private int sampleTestCaseCount;
    private int hiddenTestCaseCount;
    private Instant createdAt;
    private Instant updatedAt;

    public CodingProblemSummaryResponse() {
        this.supportedLanguageIds = Collections.emptyList();
        this.topics = Collections.emptyList();
    }

    public CodingProblemSummaryResponse(String id, String title, String slug, CodingProblemStatus status,
                                        CodingProblemType problemType, CodingProblemDifficulty difficulty,
                                        List<String> supportedLanguageIds, List<String> topics,
                                        int sampleTestCaseCount, int hiddenTestCaseCount,
                                        Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.slug = slug;
        this.status = status;
        this.problemType = problemType;
        this.difficulty = difficulty;
        this.supportedLanguageIds = supportedLanguageIds != null ? supportedLanguageIds : Collections.emptyList();
        this.topics = topics != null ? topics : Collections.emptyList();
        this.sampleTestCaseCount = sampleTestCaseCount;
        this.hiddenTestCaseCount = hiddenTestCaseCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String title;
        private String slug;
        private CodingProblemStatus status;
        private CodingProblemType problemType;
        private CodingProblemDifficulty difficulty;
        private List<String> supportedLanguageIds = Collections.emptyList();
        private List<String> topics = Collections.emptyList();
        private int sampleTestCaseCount;
        private int hiddenTestCaseCount;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder slug(String slug) {
            this.slug = slug;
            return this;
        }

        public Builder status(CodingProblemStatus status) {
            this.status = status;
            return this;
        }

        public Builder problemType(CodingProblemType problemType) {
            this.problemType = problemType;
            return this;
        }

        public Builder difficulty(CodingProblemDifficulty difficulty) {
            this.difficulty = difficulty;
            return this;
        }

        public Builder supportedLanguageIds(List<String> supportedLanguageIds) {
            this.supportedLanguageIds = supportedLanguageIds != null ? supportedLanguageIds : Collections.emptyList();
            return this;
        }

        public Builder topics(List<String> topics) {
            this.topics = topics != null ? topics : Collections.emptyList();
            return this;
        }

        public Builder sampleTestCaseCount(int sampleTestCaseCount) {
            this.sampleTestCaseCount = sampleTestCaseCount;
            return this;
        }

        public Builder hiddenTestCaseCount(int hiddenTestCaseCount) {
            this.hiddenTestCaseCount = hiddenTestCaseCount;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public CodingProblemSummaryResponse build() {
            return new CodingProblemSummaryResponse(id, title, slug, status, problemType, difficulty,
                    supportedLanguageIds, topics, sampleTestCaseCount, hiddenTestCaseCount, createdAt, updatedAt);
        }
    }

    public static CodingProblemSummaryResponse fromEntity(CodingProblem problem) {
        if (problem == null) {
            return null;
        }
        return builder()
                .id(problem.getId())
                .title(problem.getTitle())
                .slug(problem.getSlug())
                .status(problem.getStatus())
                .problemType(problem.getProblemType())
                .difficulty(problem.getDifficulty())
                .supportedLanguageIds(problem.getSupportedLanguageIds() != null ? problem.getSupportedLanguageIds() : Collections.emptyList())
                .topics(problem.getTopics() != null ? problem.getTopics() : Collections.emptyList())
                .sampleTestCaseCount(problem.getSampleTestCases() != null ? problem.getSampleTestCases().size() : 0)
                .hiddenTestCaseCount(problem.getHiddenTestCases() != null ? problem.getHiddenTestCases().size() : 0)
                .createdAt(problem.getCreatedAt())
                .updatedAt(problem.getUpdatedAt())
                .build();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public CodingProblemStatus getStatus() {
        return status;
    }

    public void setStatus(CodingProblemStatus status) {
        this.status = status;
    }

    public CodingProblemType getProblemType() {
        return problemType;
    }

    public void setProblemType(CodingProblemType problemType) {
        this.problemType = problemType;
    }

    public CodingProblemDifficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(CodingProblemDifficulty difficulty) {
        this.difficulty = difficulty;
    }

    public List<String> getSupportedLanguageIds() {
        return supportedLanguageIds;
    }

    public void setSupportedLanguageIds(List<String> supportedLanguageIds) {
        this.supportedLanguageIds = supportedLanguageIds;
    }

    public List<String> getTopics() {
        return topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics;
    }

    public int getSampleTestCaseCount() {
        return sampleTestCaseCount;
    }

    public void setSampleTestCaseCount(int sampleTestCaseCount) {
        this.sampleTestCaseCount = sampleTestCaseCount;
    }

    public int getHiddenTestCaseCount() {
        return hiddenTestCaseCount;
    }

    public void setHiddenTestCaseCount(int hiddenTestCaseCount) {
        this.hiddenTestCaseCount = hiddenTestCaseCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
