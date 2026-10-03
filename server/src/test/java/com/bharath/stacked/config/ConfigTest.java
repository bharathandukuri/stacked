package com.bharath.stacked.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("Application Configuration Unit Tests")
class ConfigTest {

    @Test
    @DisplayName("AppProperties record properties and getters")
    void appProperties() {
        AppProperties properties = new AppProperties(
                "http://app.stacked.com",
                "http://api.stacked.com",
                List.of("http://app.stacked.com", "http://admin.stacked.com")
        );

        assertThat(properties.frontendUrl()).isEqualTo("http://app.stacked.com");
        assertThat(properties.backendUrl()).isEqualTo("http://api.stacked.com");
        assertThat(properties.corsAllowedOrigins())
                .containsExactly("http://app.stacked.com", "http://admin.stacked.com");
    }

    @Test
    @DisplayName("DotenvConfig loadDotenv and dotenv bean")
    void dotenvConfig() {
        DotenvConfig dotenvConfig = new DotenvConfig();
        Dotenv dotenv = dotenvConfig.dotenv();

        assertThat(dotenv).isNotNull();
        // DotenvConfig.loadDotenv static invocation
        Dotenv staticDotenv = DotenvConfig.loadDotenv();
        assertThat(staticDotenv).isNotNull();
    }

    @Test
    @DisplayName("MongoConfig instantiable")
    void mongoConfig() {
        MongoConfig mongoConfig = new MongoConfig();
        assertThat(mongoConfig).isNotNull();
    }

    @Test
    @DisplayName("RedisConfig creates configured RedisTemplate and StringRedisTemplate")
    void redisConfig() {
        RedisConfig redisConfig = new RedisConfig();
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        ObjectMapper objectMapper = new ObjectMapper();

        RedisTemplate<String, Object> template = redisConfig.redisTemplate(connectionFactory, objectMapper);
        assertThat(template).isNotNull();
        assertThat(template.getConnectionFactory()).isSameAs(connectionFactory);
        assertThat(template.getKeySerializer()).isNotNull();
        assertThat(template.getValueSerializer()).isNotNull();

        StringRedisTemplate stringTemplate = redisConfig.stringRedisTemplate(connectionFactory);
        assertThat(stringTemplate).isNotNull();
        assertThat(stringTemplate.getConnectionFactory()).isSameAs(connectionFactory);
    }
}
