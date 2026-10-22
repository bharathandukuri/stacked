package com.bharath.stacked.modules.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinByCodeRequest {

    @NotBlank(message = "Invitation code is required")
    private String code;
}

