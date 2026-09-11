package com.bharath.stacked.mongo;

import com.bharath.stacked.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MongoDB Live Integration Tests")
class MongoIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Document(collection = "integration_test_items")
    record TestItem(
            @Id String id,
            String name,
            String category,
            double price) {
    }

    @BeforeEach
    void cleanCollection() {
        mongoTemplate.dropCollection(TestItem.class);
    }

    @Test
    @DisplayName("Insert and findById retrieves persisted document from live MongoDB container")
    void testInsertAndFindById() {
        TestItem item = new TestItem("item-1", "MacBook Pro", "Electronics", 1999.99);

        TestItem saved = mongoTemplate.insert(item);
        assertNotNull(saved);
        assertEquals("item-1", saved.id());

        TestItem found = mongoTemplate.findById("item-1", TestItem.class);
        assertNotNull(found);
        assertEquals("MacBook Pro", found.name());
        assertEquals("Electronics", found.category());
        assertEquals(1999.99, found.price());
    }

    @Test
    @DisplayName("Query by criteria filters documents correctly in live MongoDB")
    void testQueryByCriteria() {
        mongoTemplate.insert(new TestItem("1", "Keyboard", "Peripherals", 120.00));
        mongoTemplate.insert(new TestItem("2", "Mouse", "Peripherals", 80.00));
        mongoTemplate.insert(new TestItem("3", "Monitor", "Displays", 400.00));

        Query query = new Query(Criteria.where("category").is("Peripherals"));
        List<TestItem> peripherals = mongoTemplate.find(query, TestItem.class);

        assertEquals(2, peripherals.size());
        assertTrue(peripherals.stream().anyMatch(i -> i.name().equals("Keyboard")));
        assertTrue(peripherals.stream().anyMatch(i -> i.name().equals("Mouse")));
    }

    @Test
    @DisplayName("UpdateFirst updates existing document in live MongoDB")
    void testUpdateDocument() {
        mongoTemplate.insert(new TestItem("update-me", "Old Product", "General", 10.00));

        Query query = new Query(Criteria.where("id").is("update-me"));
        Update update = new Update().set("name", "Updated Product").set("price", 25.50);

        mongoTemplate.updateFirst(query, update, TestItem.class);

        TestItem updated = mongoTemplate.findById("update-me", TestItem.class);
        assertNotNull(updated);
        assertEquals("Updated Product", updated.name());
        assertEquals(25.50, updated.price());
    }

    @Test
    @DisplayName("Remove deletes document from live MongoDB")
    void testDeleteDocument() {
        TestItem item = new TestItem("delete-me", "Disposable Item", "Temporary", 5.00);
        mongoTemplate.insert(item);

        assertNotNull(mongoTemplate.findById("delete-me", TestItem.class));

        mongoTemplate.remove(item);

        assertNull(mongoTemplate.findById("delete-me", TestItem.class));
    }
}
