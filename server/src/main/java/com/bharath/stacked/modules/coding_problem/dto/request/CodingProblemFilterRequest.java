package com.bharath.stacked.modules.coding_problem.dto.request;

import com.bharath.stacked.modules.coding_problem.enums.CodingProblemDifficulty;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemStatus;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;

public class CodingProblemFilterRequest {

    private String search;
    private CodingProblemStatus status;
    private CodingProblemType problemType;
    private CodingProblemDifficulty difficulty;
    private int page = 0;
    private int size = 20;
    private String sortBy = "createdAt";
    private String sortDirection = "DESC";

    public CodingProblemFilterRequest() {
    }

    public CodingProblemFilterRequest(String search, CodingProblemStatus status, CodingProblemType problemType,
                                      CodingProblemDifficulty difficulty, int page, int size,
                                      String sortBy, String sortDirection) {
        this.search = search;
        this.status = status;
        this.problemType = problemType;
        this.difficulty = difficulty;
        this.page = page;
        this.size = size <= 0 ? 20 : size;
        this.sortBy = sortBy != null ? sortBy : "createdAt";
        this.sortDirection = sortDirection != null ? sortDirection : "DESC";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String search;
        private CodingProblemStatus status;
        private CodingProblemType problemType;
        private CodingProblemDifficulty difficulty;
        private int page = 0;
        private int size = 20;
        private String sortBy = "createdAt";
        private String sortDirection = "DESC";

        public Builder search(String search) {
            this.search = search;
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

        public Builder page(int page) {
            this.page = page;
            return this;
        }

        public Builder size(int size) {
            this.size = size;
            return this;
        }

        public Builder sortBy(String sortBy) {
            this.sortBy = sortBy;
            return this;
        }

        public Builder sortDirection(String sortDirection) {
            this.sortDirection = sortDirection;
            return this;
        }

        public CodingProblemFilterRequest build() {
            return new CodingProblemFilterRequest(search, status, problemType, difficulty, page, size, sortBy, sortDirection);
        }
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
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

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }
}
