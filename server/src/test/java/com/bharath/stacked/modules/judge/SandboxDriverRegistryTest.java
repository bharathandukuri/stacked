package com.bharath.stacked.modules.judge;

import com.bharath.stacked.modules.judge.config.JudgeProperties;
import com.bharath.stacked.modules.judge.enums.SandboxDriverType;
import com.bharath.stacked.modules.judge.sandbox.SandboxDriver;
import com.bharath.stacked.modules.judge.sandbox.SandboxDriverRegistry;
import com.bharath.stacked.modules.judge.sandbox.drivers.DockerSandboxDriver;
import com.bharath.stacked.modules.judge.sandbox.drivers.FirecrackerSandboxDriver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SandboxDriverRegistryTest {

    @Test
    @DisplayName("Should select Docker driver when configured")
    void shouldSelectDockerDriver() {
        JudgeProperties properties = new JudgeProperties(
                SandboxDriverType.DOCKER, 5000, 10000, 65536, "/tmp/test", null, null, null);

        DockerSandboxDriver dockerDriver = new DockerSandboxDriver(properties);
        FirecrackerSandboxDriver fcDriver = new FirecrackerSandboxDriver(properties);

        SandboxDriverRegistry registry = new SandboxDriverRegistry(properties, List.of(dockerDriver, fcDriver));

        SandboxDriver active = registry.getActiveDriver();
        assertThat(active).isInstanceOf(DockerSandboxDriver.class);
        assertThat(active.getType()).isEqualTo(SandboxDriverType.DOCKER);
    }

    @Test
    @DisplayName("Should select Firecracker driver when configured")
    void shouldSelectFirecrackerDriver() {
        JudgeProperties properties = new JudgeProperties(
                SandboxDriverType.FIRECRACKER, 5000, 10000, 65536, "/tmp/test", null, null, null);

        DockerSandboxDriver dockerDriver = new DockerSandboxDriver(properties);
        FirecrackerSandboxDriver fcDriver = new FirecrackerSandboxDriver(properties);

        SandboxDriverRegistry registry = new SandboxDriverRegistry(properties, List.of(dockerDriver, fcDriver));

        SandboxDriver active = registry.getActiveDriver();
        assertThat(active).isInstanceOf(FirecrackerSandboxDriver.class);
        assertThat(active.getType()).isEqualTo(SandboxDriverType.FIRECRACKER);
    }
}
