package com.bharath.stacked.modules.coding_problem.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomTestcaseRequest {
    private String id;
    private String stdin;
    private String dbInit;

    public CustomTestcaseRequest() {
    }

    public CustomTestcaseRequest(String id, String stdin, String dbInit) {
        this.id = id;
        this.stdin = stdin;
        this.dbInit = dbInit;
    }
}
