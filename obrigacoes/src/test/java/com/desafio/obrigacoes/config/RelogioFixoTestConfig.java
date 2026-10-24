package com.desafio.obrigacoes.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class RelogioFixoTestConfig {

    @Bean
    @Primary
    Clock relogioFixo() {
        return Clock.fixed(Instant.parse("2026-07-01T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    }
}
