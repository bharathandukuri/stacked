package com.bharath.stacked.modules.mcq.dto.request;

import com.bharath.stacked.modules.mcq.enums.McqDifficulty;
import com.bharath.stacked.modules.mcq.enums.McqStatus;
import com.bharath.stacked.modules.mcq.enums.McqType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McqFilterRequest {

    private String search;
    private String topic;
    private McqDifficulty difficulty;
    private McqType type;
    private McqStatus status;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;

    @Builder.Default
    private String sortBy = "createdAt";

    @Builder.Default
    private String sortDirection = "DESC";

    public boolean isDefault() {
        return (search == null || search.isBlank()) &&
                (topic == null || topic.isBlank()) &&
                difficulty == null &&
                type == null &&
                status == null &&
                page == 0 &&
                size == 20 &&
                (sortBy == null || sortBy.equalsIgnoreCase("createdAt")) &&
                (sortDirection == null || sortDirection.equalsIgnoreCase("DESC"));
    }
}

