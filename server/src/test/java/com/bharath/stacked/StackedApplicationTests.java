package com.bharath.stacked;

import com.bharath.stacked.initializer.AdminUserInitializer;
import com.bharath.stacked.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "spring.data.mongodb.auto-index-creation=false")
class StackedApplicationTests {

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private AdminUserInitializer adminUserInitializer;

    @Test
    void contextLoads() {
    }
}
