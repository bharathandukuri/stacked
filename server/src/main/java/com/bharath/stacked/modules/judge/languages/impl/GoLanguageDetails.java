package com.bharath.stacked.modules.judge.languages.impl;

import com.bharath.stacked.modules.judge.languages.CompiledLanguageDetails;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class GoLanguageDetails extends CompiledLanguageDetails {

    @Override
    public String getCompileCommand(String fileName) {
        return "go build -o solution_bin " + fileName;
    }

    @Override
    public String getRunCommand(String fileName) {
        return "./solution_bin";
    }
}
