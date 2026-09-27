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
public class JavaLanguageDetails extends CompiledLanguageDetails {

    @Override
    public String getCompileCommand(String fileName) {
        return "javac " + fileName;
    }

    @Override
    public String getRunCommand(String fileName) {
        int slashIndex = fileName.lastIndexOf('/');
        int dotIndex = fileName.lastIndexOf('.');
        String className = fileName.substring(
                slashIndex >= 0 ? slashIndex + 1 : 0,
                dotIndex >= 0 ? dotIndex : fileName.length());

        return "java " + className;
    }
}