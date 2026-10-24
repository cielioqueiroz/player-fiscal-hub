package com.desafio.obrigacoes;

import static org.assertj.core.api.Assertions.assertThat;

import com.desafio.obrigacoes.config.RelogioFixoTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest(properties = "obrigacoes.atualizacao-atrasadas.habilitada=false")
@ActiveProfiles("demo")
@Import({DemoDataSeederIT.ContainerConfig.class, RelogioFixoTestConfig.class})
class DemoDataSeederIT {

    @Autowired JdbcTemplate jdbc;

    @Test
    void preencheBancoVazioComRegimesVinculosEEntregasDeTodasAsSituacoes() {
        assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT regime_tributario) FROM empresa", Integer.class))
                .isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM empresa_obrigacao", Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForList("SELECT DISTINCT situacao FROM entrega", String.class))
                .containsExactlyInAnyOrder("ABERTA", "ATRASADA", "CONCLUIDA", "CANCELADA");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ContainerConfig {

        @Bean
        @ServiceConnection
        MySQLContainer bancoTeste() {
            return new MySQLContainer("mysql:8.4");
        }
    }
}
