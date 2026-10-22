package com.bharath.stacked.modules.assessment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionSnapshot {
    private String id;
    private String content;
    private int order;
}

