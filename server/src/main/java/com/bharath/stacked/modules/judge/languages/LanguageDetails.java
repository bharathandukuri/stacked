package com.bharath.stacked.modules.judge.languages;

import com.bharath.stacked.modules.judge.enums.LanguageType;
import com.bharath.stacked.modules.judge.enums.ProblemCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class LanguageDetails {
    private String id;
    private String name;
    private ProblemCategory category;
    private LanguageType languageType;
    private String dockerImage;
    private String firecrackerRootfs;
    private long defaultTimeoutMs;
    private int defaultMemoryLimitMb;
}
