package com.bharath.stacked.modules.execution.language;

import com.bharath.stacked.modules.language.LanguageType;
import com.bharath.stacked.modules.language.exception.LanguageNotFoundException;
import com.bharath.stacked.modules.language.exception.UnsupportedLanguageTypeException;
import com.bharath.stacked.modules.language.impl.DefaultCompiledLanguage;
import com.bharath.stacked.modules.language.impl.DefaultDatabaseLanguage;
import com.bharath.stacked.modules.language.impl.DefaultInterpretedLanguage;
import com.bharath.stacked.modules.execution.registry.DockerImageRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Language Domain Models and Exceptions Unit Tests")
class LanguageModelsAndExceptionsTest {

    @Test
    @DisplayName("LanguageType enum contains COMPILED, INTERPRETED, DATABASE")
    void languageTypeEnum() {
        assertThat(LanguageType.values()).containsExactlyInAnyOrder(
                LanguageType.COMPILED,
                LanguageType.INTERPRETED,
                LanguageType.DATABASE
        );

        assertThat(LanguageType.valueOf("COMPILED")).isEqualTo(LanguageType.COMPILED);
        assertThat(LanguageType.valueOf("INTERPRETED")).isEqualTo(LanguageType.INTERPRETED);
        assertThat(LanguageType.valueOf("DATABASE")).isEqualTo(LanguageType.DATABASE);
    }

    @Test
    @DisplayName("DefaultCompiledLanguage record constructor, accessors, and command generation")
    void defaultCompiledLanguage() {
        DefaultCompiledLanguage lang = new DefaultCompiledLanguage(
                "lang-c",
                "C Custom",
                ".c",
                DockerImageRegistry.C_17,
                files -> List.of("gcc", files.get(0)),
                files -> List.of("./a.out")
        );

        assertThat(lang.id()).isEqualTo("lang-c");
        assertThat(lang.name()).isEqualTo("C Custom");
        assertThat(lang.type()).isEqualTo(LanguageType.COMPILED);
        assertThat(lang.fileExtension()).isEqualTo(".c");
        assertThat(lang.dockerImage()).isEqualTo(DockerImageRegistry.C_17);
        assertThat(lang.dockerImageDetails()).isEqualTo(DockerImageRegistry.C_17.dockerImage());
        assertThat(lang.dockerImageReference()).isEqualTo("execution/c:17");

        assertThat(lang.compile("main.c")).containsExactly("gcc", "main.c");
        assertThat(lang.compile(List.of("main.c"))).containsExactly("gcc", "main.c");
        assertThat(lang.compile("main.c", "util.c")).containsExactly("gcc", "main.c");
        assertThat(lang.run("main.c")).containsExactly("./a.out");
        assertThat(lang.run()).containsExactly("./a.out");

        assertThatThrownBy(() -> new DefaultCompiledLanguage(null, "name", ".c", DockerImageRegistry.C_17, f -> f, f -> f))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("DefaultInterpretedLanguage record constructor, accessors, and command generation")
    void defaultInterpretedLanguage() {
        DefaultInterpretedLanguage lang = new DefaultInterpretedLanguage(
                "lang-py",
                "Python Custom",
                ".py",
                DockerImageRegistry.PYTHON_3_12,
                files -> List.of("python3", files.get(0))
        );

        assertThat(lang.id()).isEqualTo("lang-py");
        assertThat(lang.name()).isEqualTo("Python Custom");
        assertThat(lang.type()).isEqualTo(LanguageType.INTERPRETED);
        assertThat(lang.fileExtension()).isEqualTo(".py");
        assertThat(lang.dockerImage()).isEqualTo(DockerImageRegistry.PYTHON_3_12);

        assertThat(lang.run("script.py")).containsExactly("python3", "script.py");
        assertThat(lang.run(List.of("script.py"))).containsExactly("python3", "script.py");

        assertThatThrownBy(() -> new DefaultInterpretedLanguage(null, "name", ".py", DockerImageRegistry.PYTHON_3_12, f -> f))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("DefaultDatabaseLanguage record constructor, accessors, and command generation")
    void defaultDatabaseLanguage() {
        DefaultDatabaseLanguage lang = new DefaultDatabaseLanguage(
                "lang-db",
                "SQL Custom",
                ".sql",
                DockerImageRegistry.MYSQL_8_0,
                files -> List.of("mysql", "-e", "source " + files.get(0))
        );

        assertThat(lang.id()).isEqualTo("lang-db");
        assertThat(lang.name()).isEqualTo("SQL Custom");
        assertThat(lang.type()).isEqualTo(LanguageType.DATABASE);
        assertThat(lang.fileExtension()).isEqualTo(".sql");
        assertThat(lang.dockerImage()).isEqualTo(DockerImageRegistry.MYSQL_8_0);

        assertThat(lang.run("query.sql")).containsExactly("mysql", "-e", "source query.sql");
        assertThat(lang.run(List.of("query.sql"))).containsExactly("mysql", "-e", "source query.sql");

        assertThatThrownBy(() -> new DefaultDatabaseLanguage(null, "name", ".sql", DockerImageRegistry.MYSQL_8_0, f -> f))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("LanguageNotFoundException message and status code")
    void languageNotFoundException() {
        LanguageNotFoundException ex1 = new LanguageNotFoundException("cobol-85");
        assertThat(ex1.getMessage()).contains("cobol-85");

        LanguageNotFoundException ex2 = new LanguageNotFoundException("Failed to locate language", new RuntimeException());
        assertThat(ex2.getMessage()).isEqualTo("Failed to locate language");
        assertThat(ex2.getCause()).isNotNull();
    }

    @Test
    @DisplayName("UnsupportedLanguageTypeException message and type details")
    void unsupportedLanguageTypeException() {
        UnsupportedLanguageTypeException ex1 = new UnsupportedLanguageTypeException(
                "python-3.12",
                LanguageType.COMPILED,
                LanguageType.INTERPRETED
        );
        assertThat(ex1.getMessage()).contains("python-3.12").contains("INTERPRETED").contains("COMPILED");

        UnsupportedLanguageTypeException ex2 = new UnsupportedLanguageTypeException("Custom type mismatch");
        assertThat(ex2.getMessage()).isEqualTo("Custom type mismatch");
    }
}
