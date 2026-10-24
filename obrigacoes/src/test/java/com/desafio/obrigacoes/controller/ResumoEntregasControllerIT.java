package com.desafio.obrigacoes.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.desafio.obrigacoes.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

class ResumoEntregasControllerIT extends BaseIntegrationTest {

    private int competenciaSequencia;

    @Test
    void resumoUsaHojeDoClockContaSemCarregarListaEFiltraPorEmpresa() throws Exception {
        long empresa = criarEmpresa("11222333000181");
        long outraEmpresa = criarEmpresa("12345678000195");
        criarEntrega(empresa, "2026-06-30", "ABERTA");
        criarEntrega(empresa, "2026-07-01", "ABERTA");
        criarEntrega(empresa, "2026-07-08", "ABERTA");
        criarEntrega(empresa, "2026-07-09", "ABERTA");
        criarEntrega(empresa, "2026-06-30", "ATRASADA");
        criarEntrega(empresa, "2026-06-30", "CONCLUIDA");
        criarEntrega(empresa, "2026-06-30", "CANCELADA");
        criarEntrega(outraEmpresa, "2026-07-02", "ABERTA");

        mockMvc.perform(get("/entregas/resumo").param("empresaId", String.valueOf(empresa)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(7))
                .andExpect(jsonPath("$.abertas").value(4))
                .andExpect(jsonPath("$.atrasadas").value(1))
                .andExpect(jsonPath("$.concluidas").value(1))
                .andExpect(jsonPath("$.canceladas").value(1))
                .andExpect(jsonPath("$.venceEmAte7Dias").value(2))
                .andExpect(jsonPath("$.vencidasSemBaixa").value(1));

        mockMvc.perform(get("/entregas/resumo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(8));
    }

    private long criarEmpresa(String cnpj) {
        jdbc.update("INSERT INTO empresa (nome, cnpj, regime_tributario) VALUES ('Resumo', ?, 'SIMPLES')", cnpj);
        return jdbc.queryForObject("SELECT id FROM empresa WHERE cnpj = ?", Long.class, cnpj);
    }

    private void criarEntrega(long empresaId, String vencimento, String situacao) {
        jdbc.update("""
                INSERT INTO entrega (empresa_id, tipo, competencia, data_vencimento, situacao, versao, criado_em)
                VALUES (?, 'DAS', ?, ?, ?, 0, NOW())
                """, empresaId, "2026-%02d".formatted(++competenciaSequencia), vencimento, situacao);
    }
}
