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
public class RustLanguageDetails extends CompiledLanguageDetails {

    @Override
    public String getCompileCommand(String fileName) {
        return "rustc -O " + fileName + " -o solution_bin";
    }

    @Override
    public String getRunCommand(String fileName) {
        return "./solution_bin";
    }
}
