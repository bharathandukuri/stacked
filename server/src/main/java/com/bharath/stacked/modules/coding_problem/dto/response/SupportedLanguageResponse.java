package com.bharath.stacked.modules.coding_problem.dto.response;

import com.bharath.stacked.modules.language.LanguageType;

public record SupportedLanguageResponse(
        String id,
        String name,
        LanguageType type,
        String fileExtension,
        String defaultFileName,
        long defaultTimeLimitMs,
        long defaultMemoryLimitKb
) {
}

