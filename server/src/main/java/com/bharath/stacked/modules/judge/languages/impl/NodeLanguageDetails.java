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
public class NodeLanguageDetails extends InterpretedLanguageDetails {

    @Builder.Default
    private String nodeBinary = "node";

    @Override
    public String getRunCommand(String fileName) {
        String bin = nodeBinary != null ? nodeBinary : "node";
        return bin + " " + fileName;
    }
}
