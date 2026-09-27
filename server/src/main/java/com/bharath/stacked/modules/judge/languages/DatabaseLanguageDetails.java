package com.bharath.stacked.modules.judge.languages;

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
public abstract class DatabaseLanguageDetails extends LanguageDetails {
    private String initFile;
    private String solutionFile;
    private String validationFile;

    public abstract String getRunCommand(String fileName);
}
