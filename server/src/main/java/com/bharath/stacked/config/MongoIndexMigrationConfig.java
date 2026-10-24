package com.bharath.stacked.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MongoIndexMigrationConfig implements CommandLineRunner {

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) {
        dropSlugIndexIfExists("mcq_questions");
        dropSlugIndexIfExists("question_banks");
    }

    private void dropSlugIndexIfExists(String collectionName) {
        try {
            if (!mongoTemplate.collectionExists(collectionName)) {
                return;
            }
            List<IndexInfo> indexInfoList = mongoTemplate.indexOps(collectionName).getIndexInfo();
            for (IndexInfo info : indexInfoList) {
                if (info.getIndexFields().stream().anyMatch(field -> "slug".equals(field.getKey()))) {
                    log.info("Dropping obsolete index [{}] from collection [{}]", info.getName(), collectionName);
                    mongoTemplate.indexOps(collectionName).dropIndex(info.getName());
                }
            }
        } catch (Exception e) {
            log.warn("Could not inspect/drop slug index from [{}]: {}", collectionName, e.getMessage());
        }
    }
}

