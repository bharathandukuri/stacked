package com.bharath.stacked.modules.judge.sandbox;

import com.bharath.stacked.modules.judge.config.JudgeProperties;
import com.bharath.stacked.modules.judge.enums.SandboxDriverType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Registry and selector for pluggable sandbox drivers (Docker, Firecracker).
 * Resolves the active driver configured via judge.sandbox.driver.
 */
@Component
public class SandboxDriverRegistry {

    private static final Logger log = LoggerFactory.getLogger(SandboxDriverRegistry.class);

    private final JudgeProperties judgeProperties;
    private final Map<SandboxDriverType, SandboxDriver> driverMap = new EnumMap<>(SandboxDriverType.class);

    public SandboxDriverRegistry(JudgeProperties judgeProperties, List<SandboxDriver> drivers) {
        this.judgeProperties = judgeProperties;
        for (SandboxDriver driver : drivers) {
            driverMap.put(driver.getType(), driver);
            log.info("Registered sandbox driver: {} (available: {})", driver.getType(), driver.isAvailable());
        }
    }

    public SandboxDriver getActiveDriver() {
        SandboxDriverType configuredType = judgeProperties.driver();
        SandboxDriver driver = driverMap.get(configuredType);
        if (driver == null) {
            log.warn("Configured sandbox driver {} not found. Falling back to DOCKER.", configuredType);
            driver = driverMap.get(SandboxDriverType.DOCKER);
        }
        return driver;
    }

    public SandboxDriver getDriver(SandboxDriverType type) {
        return driverMap.get(type);
    }
}
