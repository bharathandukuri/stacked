package com.bharath.stacked.modules.judge.languages.impl;

import com.bharath.stacked.modules.judge.languages.DatabaseLanguageDetails;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
public class SQLiteLanguageDetails extends DatabaseLanguageDetails {

    @Override
    public String getRunCommand(String fileName) {
        return "sqlite3 test.db < " + fileName;
    }
}
