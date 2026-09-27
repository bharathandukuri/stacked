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
public abstract class InterpretedLanguageDetails extends LanguageDetails {
    private String sourceFile;
    private String validationFile;

    public abstract String getRunCommand(String fileName);

    public String getRunCommand() {
        return getRunCommand(getSourceFile());
    }
}