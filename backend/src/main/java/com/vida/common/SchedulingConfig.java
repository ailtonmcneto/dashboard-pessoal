package com.vida.common;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Tarefas periódicas (ex.: recorrências). Desligadas nos testes com {@code vida.jobs.enabled=false}. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "vida.jobs.enabled", havingValue = "true", matchIfMissing = true)
class SchedulingConfig {
}
