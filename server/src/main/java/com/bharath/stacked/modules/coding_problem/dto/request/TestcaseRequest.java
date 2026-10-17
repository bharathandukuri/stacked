package com.bharath.stacked.modules.coding_problem.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TestcaseRequest {
    private String stdin;
    private String dbInit;
}
