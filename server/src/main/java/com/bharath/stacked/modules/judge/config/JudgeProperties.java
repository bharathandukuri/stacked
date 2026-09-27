package com.bharath.stacked.modules.judge.config;

import com.bharath.stacked.modules.judge.enums.SandboxDriverType;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.Map;

@ConfigurationProperties(prefix = "judge.sandbox")
public record JudgeProperties(
        @DefaultValue("DOCKER") @NonNull SandboxDriverType driver,
        @DefaultValue("5000") long defaultTimeoutMs,
        @DefaultValue("10000") long compileTimeoutMs,
        @DefaultValue("65536") int maxOutputSizeBytes,
        @DefaultValue("/tmp/stacked/workspace") @NonNull String workspaceDir,
        @DefaultValue DockerProperties docker,
        @DefaultValue FirecrackerProperties firecracker,
        Map<String, LanguageOverrideProperties> languages) {

    public JudgeProperties {
        if (driver == null)
            driver = SandboxDriverType.DOCKER;
        if (workspaceDir == null || workspaceDir.isBlank())
            workspaceDir = "/tmp/stacked/workspace";
        if (docker == null)
            docker = new DockerProperties(null, null, null, true, 128, 1.0, 256);
        if (firecracker == null)
            firecracker = new FirecrackerProperties(null, null, null, null, null, 1, 256);
        if (languages == null)
            languages = Map.of();
    }

    public record DockerProperties(
            @DefaultValue("tcp://localhost:2375") @NonNull String host,
            @DefaultValue("docker") @NonNull String cliPath,
            @DefaultValue("none") @NonNull String network,
            @DefaultValue("true") boolean readOnly,
            @DefaultValue("128") int pidsLimit,
            @DefaultValue("1.0") double cpus,
            @DefaultValue("256") int memoryLimitMb) {

        public DockerProperties {
            if (host == null || host.isBlank())
                host = "tcp://localhost:2375";
            if (cliPath == null || cliPath.isBlank())
                cliPath = "docker";
            if (network == null || network.isBlank())
                network = "none";
        }
    }

    public record FirecrackerProperties(
            @DefaultValue("http://localhost:8085") @NonNull String serviceUrl,
            @DefaultValue("/usr/bin/firecracker") @NonNull String binaryPath,
            @DefaultValue("/tmp/firecracker") @NonNull String socketDir,
            @DefaultValue("/var/lib/firecracker/vmlinux") @NonNull String kernelPath,
            @DefaultValue("/var/lib/firecracker/rootfs") @NonNull String rootfsDir,
            @DefaultValue("1") int vcpuCount,
            @DefaultValue("256") int memSizeMib) {

        public FirecrackerProperties {
            if (serviceUrl == null || serviceUrl.isBlank())
                serviceUrl = "http://localhost:8085";
            if (binaryPath == null || binaryPath.isBlank())
                binaryPath = "/usr/bin/firecracker";
            if (socketDir == null || socketDir.isBlank())
                socketDir = "/tmp/firecracker";
            if (kernelPath == null || kernelPath.isBlank())
                kernelPath = "/var/lib/firecracker/vmlinux";
            if (rootfsDir == null || rootfsDir.isBlank())
                rootfsDir = "/var/lib/firecracker/rootfs";
        }
    }

    public record LanguageOverrideProperties(
            String dockerImage,
            String firecrackerRootfs,
            String compileCommand,
            String runCommand,
            Long timeoutMs,
            Integer memoryLimitMb) {
    }
}
