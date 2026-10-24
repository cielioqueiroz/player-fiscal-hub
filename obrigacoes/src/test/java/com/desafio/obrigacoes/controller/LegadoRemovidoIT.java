package com.desafio.obrigacoes.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.desafio.obrigacoes.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

class LegadoRemovidoIT extends BaseIntegrationTest {

    @Test
    void rotaDeObrigacoesNaoExisteMais() throws Exception {
        mockMvc.perform(get("/obrigacoes")).andExpect(status().isNotFound());
    }

    @Test
    void empresaFalsaNaoExisteMais() throws Exception {
        mockMvc.perform(get("/empresa")).andExpect(status().isNotFound());
    }

    @Test
    void raizContinuaRespondendo() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("funcionando")));
    }

    @Test
    void tabelaObrigacaoFoiRemovida() {
        Integer tabelas = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name = 'obrigacao'""", Integer.class);
        assertThat(tabelas).isZero();
    }
}
