package com.bharath.stacked.modules.coding_problem.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PreviewTestcaseRequest {
    private String id;
    private String stdin;
    private String dbInit;
}

