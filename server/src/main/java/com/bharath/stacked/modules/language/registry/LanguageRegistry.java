package com.bharath.stacked.modules.language.registry;

import com.bharath.stacked.modules.language.CompiledLanguage;
import com.bharath.stacked.modules.language.DatabaseLanguage;
import com.bharath.stacked.modules.language.InterpretedLanguage;
import com.bharath.stacked.modules.language.Language;
import com.bharath.stacked.modules.language.LanguageType;

import java.util.List;

public interface LanguageRegistry {

    void register(Language language);

    void unregister(String id);

    Language get(String id);

    CompiledLanguage getCompiled(String id);

    InterpretedLanguage getInterpreted(String id);

    DatabaseLanguage getDatabase(String id);

    List<Language> getAll();

    List<Language> getAll(LanguageType type);

    boolean isSupported(String id);
}
