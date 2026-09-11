package com.bharath.stacked.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.List;

@Configuration
public class DotenvConfig {

    private static final Logger log = LoggerFactory.getLogger(DotenvConfig.class);

    /**
     * Loads .env from candidate paths (current working directory, ./server, or
     * parent directory)
     * and populates System properties before Spring properties are resolved.
     */
    public static Dotenv loadDotenv() {
        List<String> candidatePaths = List.of(
                "./",
                "./server",
                "../",
                System.getProperty("user.dir", "."));

        Dotenv loadedDotenv = null;
        for (String path : candidatePaths) {
            File envFile = new File(path, ".env");
            if (envFile.exists() && envFile.isFile()) {
                try {
                    loadedDotenv = Dotenv.configure()
                            .directory(path)
                            .ignoreIfMalformed()
                            .ignoreIfMissing()
                            .load();
                    log.info("Loaded .env configuration successfully from: {}", envFile.getAbsolutePath());
                    break;
                } catch (Exception e) {
                    log.warn("Attempted to load .env from {}, encountered: {}", path, e.getMessage());
                }
            }
        }

        if (loadedDotenv == null) {
            log.info("No .env file found in candidate search paths. Falling back to environment variables.");
            loadedDotenv = Dotenv.configure().ignoreIfMissing().load();
        }

        // Expose .env entries into System properties so application.properties ${VAR}
        // resolutions succeed
        for (DotenvEntry entry : loadedDotenv.entries()) {
            if (System.getProperty(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        }

        return loadedDotenv;
    }

    @Bean
    public Dotenv dotenv() {
        return loadDotenv();
    }
}
