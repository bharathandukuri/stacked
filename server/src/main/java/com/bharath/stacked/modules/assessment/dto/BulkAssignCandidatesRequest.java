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
public class BulkAssignCandidatesRequest {

    private String emailsRaw;

    @Builder.Default
    private List<String> emails = new ArrayList<>();
}

