package com.bharath.stacked.modules.judge.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JudgeProperties.class)
public class JudgeConfig {
}
