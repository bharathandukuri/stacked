package com.bharath.stacked.modules.execution.language;

import com.bharath.stacked.modules.language.*;
import com.bharath.stacked.modules.language.exception.LanguageNotFoundException;
import com.bharath.stacked.modules.language.exception.UnsupportedLanguageTypeException;
import com.bharath.stacked.modules.language.factory.LanguageFactory;
import com.bharath.stacked.modules.language.factory.impl.LanguageFactoryImpl;
import com.bharath.stacked.modules.language.registry.LanguageRegistry;
import com.bharath.stacked.modules.language.registry.impl.LanguageRegistryImpl;
import com.bharath.stacked.modules.execution.registry.DockerImageRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LanguageRegistry Unit Tests")
class LanguageRegistryTest {

    private LanguageRegistry registry;
    private LanguageFactory factory;

    @BeforeEach
    void setUp() {
        factory = new LanguageFactoryImpl();
        LanguageRegistryImpl registryImpl = new LanguageRegistryImpl(factory);
        registryImpl.init();
        registry = registryImpl;
    }

    @Test
    @DisplayName("init registers all 7 standard languages on startup")
    void initBootstrapsAllLanguages() {
        assertThat(registry.getAll()).hasSize(7);
        assertThat(registry.isSupported("java-21")).isTrue();
        assertThat(registry.isSupported("c-17")).isTrue();
        assertThat(registry.isSupported("cpp-23")).isTrue();
        assertThat(registry.isSupported("python-3.12")).isTrue();
        assertThat(registry.isSupported("javascript-node-20")).isTrue();
        assertThat(registry.isSupported("mysql-8.0")).isTrue();
        assertThat(registry.isSupported("postgresql-16")).isTrue();
    }

    @Test
    @DisplayName("get retrieves language by case-insensitive ID")
    void getSuccess() {
        Language lang1 = registry.get("java-21");
        Language lang2 = registry.get("JAVA-21");
        Language lang3 = registry.get("  java-21  ");

        assertThat(lang1).isNotNull();
        assertThat(lang1.id()).isEqualTo("java-21");
        assertThat(lang1).isSameAs(lang2);
        assertThat(lang1).isSameAs(lang3);
    }

    @Test
    @DisplayName("get throws LanguageNotFoundException when language is absent")
    void getNotFound() {
        assertThatThrownBy(() -> registry.get("unknown-lang"))
                .isInstanceOf(LanguageNotFoundException.class)
                .hasMessageContaining("unknown-lang");

        assertThatThrownBy(() -> registry.get(null))
                .isInstanceOf(LanguageNotFoundException.class);

        assertThatThrownBy(() -> registry.get("   "))
                .isInstanceOf(LanguageNotFoundException.class);
    }

    @Test
    @DisplayName("getCompiled returns CompiledLanguage or throws UnsupportedLanguageTypeException")
    void getCompiled() {
        CompiledLanguage java = registry.getCompiled("java-21");
        assertThat(java).isNotNull();
        assertThat(java.type()).isEqualTo(LanguageType.COMPILED);

        assertThatThrownBy(() -> registry.getCompiled("python-3.12"))
                .isInstanceOf(UnsupportedLanguageTypeException.class)
                .hasMessageContaining("python-3.12")
                .hasMessageContaining("COMPILED");
    }

    @Test
    @DisplayName("getInterpreted returns InterpretedLanguage or throws UnsupportedLanguageTypeException")
    void getInterpreted() {
        InterpretedLanguage python = registry.getInterpreted("python-3.12");
        assertThat(python).isNotNull();
        assertThat(python.type()).isEqualTo(LanguageType.INTERPRETED);

        assertThatThrownBy(() -> registry.getInterpreted("java-21"))
                .isInstanceOf(UnsupportedLanguageTypeException.class)
                .hasMessageContaining("java-21")
                .hasMessageContaining("INTERPRETED");
    }

    @Test
    @DisplayName("getDatabase returns DatabaseLanguage or throws UnsupportedLanguageTypeException")
    void getDatabase() {
        DatabaseLanguage mysql = registry.getDatabase("mysql-8.0");
        assertThat(mysql).isNotNull();
        assertThat(mysql.type()).isEqualTo(LanguageType.DATABASE);

        assertThatThrownBy(() -> registry.getDatabase("cpp-23"))
                .isInstanceOf(UnsupportedLanguageTypeException.class)
                .hasMessageContaining("cpp-23")
                .hasMessageContaining("DATABASE");
    }

    @Test
    @DisplayName("getAll(LanguageType) filters languages correctly by category")
    void getAllByType() {
        List<Language> compiled = registry.getAll(LanguageType.COMPILED);
        List<Language> interpreted = registry.getAll(LanguageType.INTERPRETED);
        List<Language> database = registry.getAll(LanguageType.DATABASE);

        assertThat(compiled).hasSize(3)
                .extracting(Language::id)
                .containsExactlyInAnyOrder("java-21", "c-17", "cpp-23");

        assertThat(interpreted).hasSize(2)
                .extracting(Language::id)
                .containsExactlyInAnyOrder("python-3.12", "javascript-node-20");

        assertThat(database).hasSize(2)
                .extracting(Language::id)
                .containsExactlyInAnyOrder("mysql-8.0", "postgresql-16");
    }

    @Test
    @DisplayName("register dynamically adds custom language and unregister removes it")
    void registerAndUnregister() {
        CompiledLanguage goLang = factory.createCompiled(
                "go-1.22",
                "Go (1.22)",
                ".go",
                DockerImageRegistry.ISOLATE_1_0,
                files -> List.of("go", "build", "-o", "main", files.get(0)),
                files -> List.of("./main")
        );

        assertThat(registry.isSupported("go-1.22")).isFalse();

        registry.register(goLang);
        assertThat(registry.isSupported("go-1.22")).isTrue();
        assertThat(registry.get("go-1.22")).isEqualTo(goLang);
        assertThat(registry.getCompiled("go-1.22")).isEqualTo(goLang);

        registry.unregister("go-1.22");
        assertThat(registry.isSupported("go-1.22")).isFalse();
        assertThatThrownBy(() -> registry.get("go-1.22"))
                .isInstanceOf(LanguageNotFoundException.class);
    }

    @Test
    @DisplayName("unregister with null or blank does nothing without error")
    void unregisterNullSafe() {
        registry.unregister(null);
        registry.unregister("   ");
        assertThat(registry.getAll()).hasSize(7);
    }
}
