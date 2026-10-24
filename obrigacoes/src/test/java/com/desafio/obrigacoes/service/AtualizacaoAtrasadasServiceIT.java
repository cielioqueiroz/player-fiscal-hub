package com.desafio.obrigacoes.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.desafio.obrigacoes.BaseIntegrationTest;
import com.desafio.obrigacoes.model.RegimeTributario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AtualizacaoAtrasadasServiceIT extends BaseIntegrationTest {

    @Autowired AtualizacaoAtrasadasService service;
    private int competenciaSequencia;

    @Test
    void atualizaSomenteAbertasComVencimentoAnteriorAHojeEPermiteRepetir() {
        long empresaId = criarEmpresa("11222333000181");
        criarEntrega(empresaId, "2026-06-30", "ABERTA");
        criarEntrega(empresaId, "2026-07-01", "ABERTA");
        criarEntrega(empresaId, "2026-06-30", "CONCLUIDA");
        criarEntrega(empresaId, "2026-06-30", "CANCELADA");

        assertThat(service.atualizarAtrasadas()).isEqualTo(1);
        assertThat(service.atualizarAtrasadas()).isZero();
        assertThat(jdbc.queryForList("SELECT situacao FROM entrega ORDER BY id", String.class))
                .containsExactly("ATRASADA", "ABERTA", "CONCLUIDA", "CANCELADA");
    }

    private long criarEmpresa(String cnpj) {
        jdbc.update("INSERT INTO empresa (nome, cnpj, regime_tributario) VALUES ('Resumo', ?, ?)",
                cnpj, RegimeTributario.SIMPLES.name());
        return jdbc.queryForObject("SELECT id FROM empresa WHERE cnpj = ?", Long.class, cnpj);
    }

    private void criarEntrega(long empresaId, String vencimento, String situacao) {
        jdbc.update("""
                INSERT INTO entrega (empresa_id, tipo, competencia, data_vencimento, situacao, versao, criado_em)
                VALUES (?, 'DAS', ?, ?, ?, 0, NOW())
                """, empresaId, "2026-%02d".formatted(++competenciaSequencia), vencimento, situacao);
    }
}
