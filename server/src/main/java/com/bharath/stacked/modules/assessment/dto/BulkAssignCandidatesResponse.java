package com.bharath.stacked.modules.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkAssignCandidatesResponse {

    private int totalProcessed;

    private int successfullyAssigned;

    private int duplicatesSkipped;

    @Builder.Default
    private List<String> invalidEmails = new ArrayList<>();

    @Builder.Default
    private List<String> assignedEmails = new ArrayList<>();
}

