package com.desafio.obrigacoes.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.desafio.obrigacoes.BaseIntegrationTest;
import com.desafio.obrigacoes.model.RegimeTributario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class EntregaControllerIT extends BaseIntegrationTest {

    private static final String MES_MAIO = """
            {"mesReferencia":"2026-05"}""";

    private long empresaA;
    private long empresaB;

    @BeforeEach
    void criarMassa() {
        empresaA = criarEmpresa("11222333000181", RegimeTributario.SIMPLES);
        empresaB = criarEmpresa("11444777000161", RegimeTributario.LUCRO_REAL);
        vincular(empresaA, "DAS");
        vincular(empresaA, "ECD");
        vincular(empresaB, "DCTF");
        vincular(empresaB, "ECD");
    }

    private long criarEmpresa(String cnpj, RegimeTributario regime) {
        jdbc.update("INSERT INTO empresa (nome, cnpj, regime_tributario) VALUES ('Teste', ?, ?)", cnpj, regime.name());
        return jdbc.queryForObject("SELECT id FROM empresa WHERE cnpj = ?", Long.class, cnpj);
    }

    private void vincular(long empresaId, String tipo) {
        jdbc.update("INSERT INTO empresa_obrigacao (empresa_id, tipo, criado_em) VALUES (?, ?, NOW())", empresaId, tipo);
    }

    private ResultActions gerar(String json) throws Exception {
        return mockMvc.perform(post("/entregas/gerar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private int contarEntregas() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM entrega", Integer.class);
    }

    @Test
    void gerarDevolveResumo() throws Exception {
        gerar(MES_MAIO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mesReferencia").value("2026-05"))
                .andExpect(jsonPath("$.criadas").value(4))
                .andExpect(jsonPath("$.jaExistentes").value(0));
    }

    @Test
    void naoGeraEntregasParaEmpresaInativa() throws Exception {
        jdbc.update("UPDATE empresa SET ativa = FALSE WHERE id = ?", empresaB);

        gerar(MES_MAIO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.criadas").value(2));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM entrega WHERE empresa_id = ?", Integer.class, empresaB))
                .isZero();
    }

    @Test
    void mesInexistenteDevolve400() throws Exception {
        gerar("""
                {"mesReferencia":"2026-13"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("mesReferencia deve estar no formato AAAA-MM"));
    }

    @Test
    void mesPorExtensoDevolve400() throws Exception {
        gerar("""
                {"mesReferencia":"maio"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("mesReferencia deve estar no formato AAAA-MM"));
    }

    @Test
    void corpoSemMesDevolve400() throws Exception {
        gerar("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("mesReferencia é obrigatório (formato AAAA-MM)"));
    }

    @Test
    void semCorpoDevolve400() throws Exception {
        mockMvc.perform(post("/entregas/gerar").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("mesReferencia é obrigatório (formato AAAA-MM)"));
    }

    @Test
    void mesForaDoIntervaloDevolve400() throws Exception {
        for (String mes : new String[] {"1999-12", "3000-01"}) {
            gerar("""
                    {"mesReferencia":"%s"}""".formatted(mes))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro").value("mesReferencia deve ter ano entre 2000 e 2999"));
        }

        assertThat(contarEntregas()).isZero();
    }

    @Test
    void listaTodasOrdenadasPorVencimento() throws Exception {
        gerar(MES_MAIO);

        mockMvc.perform(get("/entregas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].tipo").value("ECD"))
                .andExpect(jsonPath("$[0].empresaId").value(empresaA))
                .andExpect(jsonPath("$[0].dataVencimento").value("2026-05-31"))
                .andExpect(jsonPath("$[1].tipo").value("ECD"))
                .andExpect(jsonPath("$[1].empresaId").value(empresaB))
                .andExpect(jsonPath("$[2].tipo").value("DAS"))
                .andExpect(jsonPath("$[3].tipo").value("DCTF"));
    }

    @Test
    void filtraPorEmpresa() throws Exception {
        gerar(MES_MAIO);

        mockMvc.perform(get("/entregas").param("empresaId", String.valueOf(empresaA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void filtraPorTipo() throws Exception {
        gerar(MES_MAIO);

        mockMvc.perform(get("/entregas").param("tipo", "ECD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void filtraPorCompetencia() throws Exception {
        gerar(MES_MAIO);

        mockMvc.perform(get("/entregas").param("competencia", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void competenciaEmBrancoNaoFiltra() throws Exception {
        gerar(MES_MAIO);

        mockMvc.perform(get("/entregas").param("competencia", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));

        mockMvc.perform(get("/entregas")
                        .param("empresaId", "")
                        .param("tipo", "")
                        .param("competencia", "")
                        .param("situacao", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void filtraPorSituacaoETipo() throws Exception {
        gerar(MES_MAIO);

        mockMvc.perform(get("/entregas").param("situacao", "ABERTA").param("tipo", "DAS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void situacaoInvalidaDevolve400() throws Exception {
        mockMvc.perform(get("/entregas").param("situacao", "XYZ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Parâmetro inválido: situacao"));
    }

    @Test
    void tipoInvalidoDevolve400() throws Exception {
        mockMvc.perform(get("/entregas").param("tipo", "XYZ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Parâmetro inválido: tipo"));
    }

    @Test
    void empresaInexistenteDevolveListaVazia() throws Exception {
        gerar(MES_MAIO);

        mockMvc.perform(get("/entregas").param("empresaId", "999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void empresaIdNaoNumericoDevolve400() throws Exception {
        mockMvc.perform(get("/entregas").param("empresaId", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Parâmetro inválido: empresaId"));
    }

    @Test
    void detalheTrazEmpresa() throws Exception {
        gerar(MES_MAIO);
        long id = jdbc.queryForObject(
                "SELECT id FROM entrega WHERE empresa_id = ? AND tipo = 'DAS'", Long.class, empresaA);

        mockMvc.perform(get("/entregas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empresaNome").value("Teste"))
                .andExpect(jsonPath("$.empresaCnpj").value("11222333000181"))
                .andExpect(jsonPath("$.situacao").value("ABERTA"))
                .andExpect(jsonPath("$.competencia").value("2026-05"))
                .andExpect(jsonPath("$.dataVencimento").value("2026-06-20"));
    }

    @Test
    void detalheInexistenteDevolve404() throws Exception {
        mockMvc.perform(get("/entregas/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Entrega 999999 não encontrada"));
    }
}
