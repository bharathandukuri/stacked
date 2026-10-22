package com.bharath.stacked;

import com.bharath.stacked.modules.security.initializer.AdminUserInitializer;
import com.bharath.stacked.modules.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.data.mongodb.auto-index-creation=false",
        "stacked.execution.docker.enabled=false",
        "jwt.secret=c3VwZXItc2VjcmV0LWtleS1mb3ItdGVzdGluZy1wdXJwb3Nlcy1vbmx5LTI1NmJpdHMh"
})
class StackedApplicationTests {

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private AdminUserInitializer adminUserInitializer;

    @Test
    void contextLoads() {
    }

    @Test
    void applicationInstantiation() {
        StackedApplication app = new StackedApplication();
        org.assertj.core.api.Assertions.assertThat(app).isNotNull();
    }
}
