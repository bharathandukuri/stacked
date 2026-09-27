package com.bharath.stacked.modules.judge.languages.impl;

import com.bharath.stacked.modules.judge.languages.CompiledLanguageDetails;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class CppLanguageDetails extends CompiledLanguageDetails {

    @Builder.Default
    private String compilerBinary = "g++";

    @Builder.Default
    private String cppStandard = "c++23";

    @Override
    public String getCompileCommand(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        String binaryName = dotIndex >= 0 ? fileName.substring(0, dotIndex) : fileName + "_bin";
        return compilerBinary + " -O3 -std=" + cppStandard + " " + fileName + " -o " + binaryName;
    }

    @Override
    public String getRunCommand(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        String binaryName = dotIndex >= 0 ? fileName.substring(0, dotIndex) : fileName + "_bin";
        return "./" + binaryName;
    }
}
