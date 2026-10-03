package com.bharath.stacked.modules.execution.language;

import com.bharath.stacked.modules.language.*;
import com.bharath.stacked.modules.language.exception.LanguageNotFoundException;
import com.bharath.stacked.modules.language.factory.LanguageFactory;
import com.bharath.stacked.modules.language.factory.impl.LanguageFactoryImpl;
import com.bharath.stacked.modules.execution.registry.DockerImageRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LanguageFactory Unit Tests")
class LanguageFactoryTest {

    private LanguageFactory factory;

    @BeforeEach
    void setUp() {
        factory = new LanguageFactoryImpl();
    }

    @Test
    @DisplayName("supportedLanguageIds returns all 7 standard built-in language IDs")
    void supportedLanguageIds() {
        assertThat(factory.supportedLanguageIds()).containsExactly(
                "java-21",
                "c-17",
                "cpp-23",
                "python-3.12",
                "javascript-node-20",
                "mysql-8.0",
                "postgresql-16"
        );
    }

    @Test
    @DisplayName("create('java-21') produces valid CompiledLanguage with javac and java commands")
    void createJava21() {
        Language language = factory.create("java-21");

        assertThat(language).isInstanceOf(CompiledLanguage.class);
        CompiledLanguage compiled = (CompiledLanguage) language;

        assertThat(compiled.id()).isEqualTo("java-21");
        assertThat(compiled.name()).isEqualTo("Java (OpenJDK 21)");
        assertThat(compiled.type()).isEqualTo(LanguageType.COMPILED);
        assertThat(compiled.fileExtension()).isEqualTo(".java");
        assertThat(compiled.dockerImage()).isEqualTo(DockerImageRegistry.JAVA_21);

        // Compile commands
        assertThat(compiled.compile("Solution.java"))
                .containsExactly("javac", "Solution.java");
        assertThat(compiled.compile(List.of("Solution.java", "Helper.java")))
                .containsExactly("javac", "Solution.java", "Helper.java");
        assertThat(compiled.compile(List.of()))
                .containsExactly("javac", "Solution.java");

        // Run commands
        assertThat(compiled.run("Solution.java"))
                .containsExactly("java", "-cp", ".", "Solution");
        assertThat(compiled.run("Main.java"))
                .containsExactly("java", "-cp", ".", "Main");
        assertThat(compiled.run(List.of()))
                .containsExactly("java", "-cp", ".", "Solution");
    }

    @Test
    @DisplayName("create('c-17') produces valid CompiledLanguage with gcc and a.out commands")
    void createC17() {
        Language language = factory.create("c-17");

        assertThat(language).isInstanceOf(CompiledLanguage.class);
        CompiledLanguage compiled = (CompiledLanguage) language;

        assertThat(compiled.id()).isEqualTo("c-17");
        assertThat(compiled.name()).isEqualTo("C (GCC 13.2)");
        assertThat(compiled.type()).isEqualTo(LanguageType.COMPILED);
        assertThat(compiled.fileExtension()).isEqualTo(".c");
        assertThat(compiled.dockerImage()).isEqualTo(DockerImageRegistry.C_17);

        assertThat(compiled.compile("solution.c"))
                .containsExactly("gcc", "-std=c17", "-O2", "-Wall", "solution.c", "-o", "a.out", "-lm");
        assertThat(compiled.compile(List.of("solution.c", "extra.c")))
                .containsExactly("gcc", "-std=c17", "-O2", "-Wall", "solution.c", "extra.c", "-o", "a.out", "-lm");

        assertThat(compiled.run("solution.c")).containsExactly("./a.out");
        assertThat(compiled.run()).containsExactly("./a.out");
    }

    @Test
    @DisplayName("create('cpp-23') produces valid CompiledLanguage with g++ and a.out commands")
    void createCpp23() {
        Language language = factory.create("cpp-23");

        assertThat(language).isInstanceOf(CompiledLanguage.class);
        CompiledLanguage compiled = (CompiledLanguage) language;

        assertThat(compiled.id()).isEqualTo("cpp-23");
        assertThat(compiled.name()).isEqualTo("C++ (GCC 13.2)");
        assertThat(compiled.type()).isEqualTo(LanguageType.COMPILED);
        assertThat(compiled.fileExtension()).isEqualTo(".cpp");
        assertThat(compiled.dockerImage()).isEqualTo(DockerImageRegistry.CPP_23);

        assertThat(compiled.compile("solution.cpp"))
                .containsExactly("g++", "-std=c++23", "-O2", "-Wall", "solution.cpp", "-o", "a.out");
        assertThat(compiled.run("solution.cpp")).containsExactly("./a.out");
    }

    @Test
    @DisplayName("create('python-3.12') produces valid InterpretedLanguage with python3 command")
    void createPython312() {
        Language language = factory.create("python-3.12");

        assertThat(language).isInstanceOf(InterpretedLanguage.class);
        InterpretedLanguage interpreted = (InterpretedLanguage) language;

        assertThat(interpreted.id()).isEqualTo("python-3.12");
        assertThat(interpreted.name()).isEqualTo("Python (CPython 3.12)");
        assertThat(interpreted.type()).isEqualTo(LanguageType.INTERPRETED);
        assertThat(interpreted.fileExtension()).isEqualTo(".py");
        assertThat(interpreted.dockerImage()).isEqualTo(DockerImageRegistry.PYTHON_3_12);

        assertThat(interpreted.run("solution.py")).containsExactly("python3", "solution.py");
        assertThat(interpreted.run("main.py")).containsExactly("python3", "main.py");
        assertThat(interpreted.run()).containsExactly("python3", "solution.py");
    }

    @Test
    @DisplayName("create('javascript-node-20') produces valid InterpretedLanguage with node command")
    void createJavaScriptNode20() {
        Language language = factory.create("javascript-node-20");

        assertThat(language).isInstanceOf(InterpretedLanguage.class);
        InterpretedLanguage interpreted = (InterpretedLanguage) language;

        assertThat(interpreted.id()).isEqualTo("javascript-node-20");
        assertThat(interpreted.name()).isEqualTo("JavaScript (Node.js 20)");
        assertThat(interpreted.type()).isEqualTo(LanguageType.INTERPRETED);
        assertThat(interpreted.fileExtension()).isEqualTo(".js");
        assertThat(interpreted.dockerImage()).isEqualTo(DockerImageRegistry.JAVASCRIPT_NODE_20);

        assertThat(interpreted.run("solution.js")).containsExactly("node", "solution.js");
        assertThat(interpreted.run()).containsExactly("node", "solution.js");
    }

    @Test
    @DisplayName("create('mysql-8.0') produces valid DatabaseLanguage with mysql command")
    void createMySql80() {
        Language language = factory.create("mysql-8.0");

        assertThat(language).isInstanceOf(DatabaseLanguage.class);
        DatabaseLanguage database = (DatabaseLanguage) language;

        assertThat(database.id()).isEqualTo("mysql-8.0");
        assertThat(database.name()).isEqualTo("MySQL (8.0)");
        assertThat(database.type()).isEqualTo(LanguageType.DATABASE);
        assertThat(database.fileExtension()).isEqualTo(".sql");
        assertThat(database.dockerImage()).isEqualTo(DockerImageRegistry.MYSQL_8_0);

        assertThat(database.run("query.sql"))
                .containsExactly("mysql", "-u", "root", "-pstacked_judge", "stacked_judge_db", "-e", "source query.sql");
        assertThat(database.run())
                .containsExactly("mysql", "-u", "root", "-pstacked_judge", "stacked_judge_db", "-e", "source solution.sql");
    }

    @Test
    @DisplayName("create('postgresql-16') produces valid DatabaseLanguage with psql command")
    void createPostgreSql16() {
        Language language = factory.create("postgresql-16");

        assertThat(language).isInstanceOf(DatabaseLanguage.class);
        DatabaseLanguage database = (DatabaseLanguage) language;

        assertThat(database.id()).isEqualTo("postgresql-16");
        assertThat(database.name()).isEqualTo("PostgreSQL (16)");
        assertThat(database.type()).isEqualTo(LanguageType.DATABASE);
        assertThat(database.fileExtension()).isEqualTo(".sql");
        assertThat(database.dockerImage()).isEqualTo(DockerImageRegistry.POSTGRES_16);

        assertThat(database.run("query.sql"))
                .containsExactly("psql", "-U", "postgres", "-d", "stacked_judge_db", "-f", "query.sql");
        assertThat(database.run())
                .containsExactly("psql", "-U", "postgres", "-d", "stacked_judge_db", "-f", "solution.sql");
    }

    @Test
    @DisplayName("create throws LanguageNotFoundException for null, blank, or unsupported language ID")
    void createExceptions() {
        assertThatThrownBy(() -> factory.create(null))
                .isInstanceOf(LanguageNotFoundException.class);

        assertThatThrownBy(() -> factory.create("   "))
                .isInstanceOf(LanguageNotFoundException.class);

        assertThatThrownBy(() -> factory.create("ruby-3.2"))
                .isInstanceOf(LanguageNotFoundException.class)
                .hasMessageContaining("ruby-3.2");
    }

    @Test
    @DisplayName("createCompiled, createInterpreted, createDatabase create custom language instances")
    void customLanguageCreation() {
        CompiledLanguage customCompiled = factory.createCompiled(
                "rust-1.76",
                "Rust (1.76)",
                ".rs",
                DockerImageRegistry.ISOLATE_1_0,
                files -> List.of("rustc", files.get(0)),
                files -> List.of("./solution")
        );
        assertThat(customCompiled.id()).isEqualTo("rust-1.76");
        assertThat(customCompiled.type()).isEqualTo(LanguageType.COMPILED);

        InterpretedLanguage customInterpreted = factory.createInterpreted(
                "ruby-3.2",
                "Ruby 3.2",
                ".rb",
                DockerImageRegistry.ISOLATE_1_0,
                files -> List.of("ruby", files.get(0))
        );
        assertThat(customInterpreted.id()).isEqualTo("ruby-3.2");
        assertThat(customInterpreted.type()).isEqualTo(LanguageType.INTERPRETED);

        DatabaseLanguage customDatabase = factory.createDatabase(
                "sqlite-3.45",
                "SQLite 3.45",
                ".sql",
                DockerImageRegistry.ISOLATE_1_0,
                files -> List.of("sqlite3", "test.db", ".read " + files.get(0))
        );
        assertThat(customDatabase.id()).isEqualTo("sqlite-3.45");
        assertThat(customDatabase.type()).isEqualTo(LanguageType.DATABASE);
    }
}
