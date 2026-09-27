package com.bharath.stacked.modules.judge.languages.impl;

import com.bharath.stacked.modules.judge.languages.InterpretedLanguageDetails;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class PythonLanguageDetails extends InterpretedLanguageDetails {

    @Builder.Default
    private String pythonBinary = "python3";

    @Override
    public String getRunCommand(String fileName) {
        String bin = pythonBinary != null ? pythonBinary : "python3";
        return bin + " " + fileName;
    }
}