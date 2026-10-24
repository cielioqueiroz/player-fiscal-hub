package com.desafio.obrigacoes.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.desafio.obrigacoes.BaseIntegrationTest;
import com.desafio.obrigacoes.model.RegimeTributario;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class EmpresaObrigacaoControllerIT extends BaseIntegrationTest {

    private static final long EMPRESA_INEXISTENTE = 999_999L;

    private long criarEmpresa(String cnpj, RegimeTributario regime) {
        jdbc.update("INSERT INTO empresa (nome, cnpj, regime_tributario) VALUES ('Teste', ?, ?)", cnpj, regime.name());
        return jdbc.queryForObject("SELECT id FROM empresa WHERE cnpj = ?", Long.class, cnpj);
    }

    private long empresaSimples() {
        return criarEmpresa("11222333000181", RegimeTributario.SIMPLES);
    }

    private ResultActions anexar(long empresaId, String json) throws Exception {
        return mockMvc.perform(post("/empresas/{id}/obrigacoes", empresaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions anexarTipo(long empresaId, String tipo) throws Exception {
        return anexar(empresaId, """
                {"tipo": "%s"}""".formatted(tipo));
    }

    @Test
    void catalogoListaAsTresObrigacoes() throws Exception {
        mockMvc.perform(get("/catalogo-obrigacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].tipo").value("DAS"))
                .andExpect(jsonPath("$[0].descricao").value("Documento de Arrecadação do Simples Nacional"))
                .andExpect(jsonPath("$[0].periodicidade").value("MENSAL"))
                .andExpect(jsonPath("$[0].regimesAceitos", contains("SIMPLES")))
                .andExpect(jsonPath("$[1].tipo").value("DCTF"))
                .andExpect(jsonPath("$[1].regimesAceitos", contains("LUCRO_PRESUMIDO", "LUCRO_REAL")))
                .andExpect(jsonPath("$[2].tipo").value("ECD"))
                .andExpect(jsonPath("$[2].periodicidade").value("ANUAL"))
                .andExpect(jsonPath("$[2].regimesAceitos", contains("SIMPLES", "LUCRO_PRESUMIDO", "LUCRO_REAL")));
    }

    @Test
    void simplesAnexaDas() throws Exception {
        long empresa = empresaSimples();

        anexarTipo(empresa, "DAS")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.tipo").value("DAS"))
                .andExpect(jsonPath("$.descricao").value("Documento de Arrecadação do Simples Nacional"))
                .andExpect(jsonPath("$.criadoEm").exists());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM empresa_obrigacao WHERE empresa_id = ?", Integer.class, empresa))
                .isEqualTo(1);
    }

    @Test
    void simplesAnexaEcd() throws Exception {
        anexarTipo(empresaSimples(), "ECD").andExpect(status().isCreated());
    }

    @Test
    void simplesNaoAnexaDctf() throws Exception {
        anexarTipo(empresaSimples(), "DCTF")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.erro").value("A obrigação DCTF não se aplica ao regime SIMPLES"));
    }

    @Test
    void lucroRealNaoAnexaDas() throws Exception {
        anexarTipo(criarEmpresa("11444777000161", RegimeTributario.LUCRO_REAL), "DAS")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.erro").value("A obrigação DAS não se aplica ao regime LUCRO_REAL"));
    }

    @Test
    void lucroPresumidoAnexaDctf() throws Exception {
        anexarTipo(criarEmpresa("12345678000195", RegimeTributario.LUCRO_PRESUMIDO), "DCTF")
                .andExpect(status().isCreated());
    }

    @Test
    void anexarDuasVezesDevolve409() throws Exception {
        long empresa = empresaSimples();
        anexarTipo(empresa, "DAS").andExpect(status().isCreated());

        anexarTipo(empresa, "DAS")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("A obrigação DAS já está anexada a esta empresa"));
    }

    @Test
    void anexarEmEmpresaInexistenteDevolve404() throws Exception {
        anexarTipo(EMPRESA_INEXISTENTE, "DAS")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Empresa 999999 não encontrada"));
    }

    @Test
    void anexarSemTipoDevolve400() throws Exception {
        anexar(empresaSimples(), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Tipo de obrigação é obrigatório"));
    }

    @Test
    void anexarSemTipoEmEmpresaInexistenteDevolve404() throws Exception {
        anexar(EMPRESA_INEXISTENTE, "{}").andExpect(status().isNotFound());
    }

    @Test
    void tipoDesconhecidoDevolve400() throws Exception {
        anexarTipo(empresaSimples(), "XYZ").andExpect(status().isBadRequest());
    }

    @Test
    void listaObrigacoesAnexadas() throws Exception {
        long empresa = empresaSimples();
        anexarTipo(empresa, "ECD").andExpect(status().isCreated());
        anexarTipo(empresa, "DAS").andExpect(status().isCreated());

        mockMvc.perform(get("/empresas/{id}/obrigacoes", empresa))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tipo").value("DAS"))
                .andExpect(jsonPath("$[1].tipo").value("ECD"));
    }

    @Test
    void listarDeEmpresaInexistenteDevolve404() throws Exception {
        mockMvc.perform(get("/empresas/{id}/obrigacoes", EMPRESA_INEXISTENTE))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeVinculo() throws Exception {
        long empresa = empresaSimples();
        anexarTipo(empresa, "DAS").andExpect(status().isCreated());
        anexarTipo(empresa, "ECD").andExpect(status().isCreated());

        mockMvc.perform(delete("/empresas/{id}/obrigacoes/DAS", empresa))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(get("/empresas/{id}/obrigacoes", empresa))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tipo").value("ECD"));
    }

    @Test
    void removerVinculoInexistenteDevolve404() throws Exception {
        mockMvc.perform(delete("/empresas/{id}/obrigacoes/DAS", empresaSimples()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("A obrigação DAS não está anexada a esta empresa"));
    }

    @Test
    void removerDeEmpresaInexistenteDevolve404() throws Exception {
        mockMvc.perform(delete("/empresas/{id}/obrigacoes/DAS", EMPRESA_INEXISTENTE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Empresa 999999 não encontrada"));
    }

    @Test
    void removerComTipoInvalidoNaUrlDevolve400() throws Exception {
        mockMvc.perform(delete("/empresas/{id}/obrigacoes/XYZ", empresaSimples()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Parâmetro inválido: tipo"));
    }
}
