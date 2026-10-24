package com.desafio.obrigacoes.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class ClockConfigTest {

    @Test
    void relogioUsaFusoDeSaoPaulo() {
        assertThat(new ClockConfig().clock().getZone()).isEqualTo(ZoneId.of("America/Sao_Paulo"));
    }
}
