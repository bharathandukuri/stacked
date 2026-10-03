package com.bharath.stacked.modules.execution.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DockerProperties.class)
public class DockerConfig {

    @Bean
    public DockerClient dockerClient(DockerProperties properties) {

        DefaultDockerClientConfig.Builder builder =
                DefaultDockerClientConfig.createDefaultConfigBuilder();

        if (properties.getHost() != null && !properties.getHost().isBlank()) {
            builder.withDockerHost(properties.getHost());
        }

        DefaultDockerClientConfig config = builder.build();

        DockerHttpClient httpClient =
                new ApacheDockerHttpClient.Builder()
                        .dockerHost(config.getDockerHost())
                        .sslConfig(config.getSSLConfig())
                        .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }
}