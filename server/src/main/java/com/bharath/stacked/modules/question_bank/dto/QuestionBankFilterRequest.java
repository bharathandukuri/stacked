package com.bharath.stacked.modules.question_bank.dto;

import com.bharath.stacked.modules.question_bank.enums.QuestionBankStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBankFilterRequest {
    private String search;
    private String category;
    private QuestionBankStatus status;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;

    public boolean isDefault() {
        return (search == null || search.isBlank()) &&
                (category == null || category.isBlank()) &&
                status == null &&
                (page == null || page == 0) &&
                (size == null || size == 20) &&
                (sortBy == null || sortBy.equalsIgnoreCase("createdAt")) &&
                (sortDirection == null || sortDirection.equalsIgnoreCase("DESC"));
    }
}

