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
public class CSharpLanguageDetails extends CompiledLanguageDetails {

    @Override
    public String getCompileCommand(String fileName) {
        return "dotnet build -c Release -o /tmp/bin " + fileName;
    }

    @Override
    public String getRunCommand(String fileName) {
        return "dotnet /tmp/bin/Solution.dll";
    }
}
