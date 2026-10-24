package com.desafio.obrigacoes.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.desafio.obrigacoes.BaseIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class EmpresaControllerIT extends BaseIntegrationTest {

    private ResultActions cadastrar(String json) throws Exception {
        return mockMvc.perform(post("/empresas").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions cadastrar(String nome, String cnpj, String regime) throws Exception {
        return cadastrar("""
                {"nome": "%s", "cnpj": "%s", "regimeTributario": "%s"}""".formatted(nome, cnpj, regime));
    }

    private long idDe(ResultActions resultado) throws Exception {
        return ((Number) JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Test
    void cadastraComCnpjMascaradoESalvaNormalizado() throws Exception {
        ResultActions resultado = cadastrar("Alfa", "11.222.333/0001-81", "SIMPLES")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nome").value("Alfa"))
                .andExpect(jsonPath("$.cnpj").value("11222333000181"))
                .andExpect(jsonPath("$.regimeTributario").value("SIMPLES"));

        assertThat(jdbc.queryForObject("SELECT cnpj FROM empresa WHERE id = ?", String.class, idDe(resultado)))
                .isEqualTo("11222333000181");
    }

    @Test
    void cnpjComDigitoErradoDevolve400() throws Exception {
        cadastrar("Alfa", "11222333000182", "SIMPLES")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("CNPJ inválido"));
    }

    @Test
    void semRegimeDevolve400() throws Exception {
        cadastrar("""
                {"nome": "Alfa", "cnpj": "11222333000181"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Regime tributário é obrigatório"));
    }

    @Test
    void regimeDesconhecidoDevolve400() throws Exception {
        cadastrar("Alfa", "11222333000181", "XYZ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void semNomeDevolve400() throws Exception {
        cadastrar("""
                {"cnpj": "11222333000181", "regimeTributario": "SIMPLES"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Nome é obrigatório"));
    }

    @Test
    void nomeEmBrancoDevolve400() throws Exception {
        cadastrar("   ", "11222333000181", "SIMPLES")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Nome é obrigatório"));
    }

    @Test
    void nomeLongoDemaisDevolve400() throws Exception {
        cadastrar("a".repeat(256), "11222333000181", "SIMPLES")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Nome deve ter no máximo 255 caracteres"));
    }

    @Test
    void cnpjRepetidoComESemMascaraDevolve409() throws Exception {
        cadastrar("Alfa", "11.222.333/0001-81", "SIMPLES").andExpect(status().isCreated());

        cadastrar("Beta", "11222333000181", "LUCRO_REAL")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("Já existe uma empresa com o CNPJ 11222333000181"));
    }

    @Test
    void cnpjAlfanumericoEmMinusculasConflitaComMaiusculas() throws Exception {
        cadastrar("Alfa", "12.abc.345/01de-35", "SIMPLES")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cnpj").value("12ABC34501DE35"));

        cadastrar("Beta", "12ABC34501DE35", "SIMPLES")
                .andExpect(status().isConflict());
    }

    @Test
    void listaEmpresasComoDto() throws Exception {
        cadastrar("Alfa", "11222333000181", "SIMPLES").andExpect(status().isCreated());
        cadastrar("Beta", "12345678000195", "LUCRO_REAL").andExpect(status().isCreated());

        mockMvc.perform(get("/empresas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].regimeTributario").exists());
    }

    @Test
    void buscaPorIdExistente() throws Exception {
        long id = idDe(cadastrar("Alfa", "11222333000181", "SIMPLES"));

        mockMvc.perform(get("/empresas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Alfa"));
    }

    @Test
    void buscaPorIdInexistenteDevolve404() throws Exception {
        mockMvc.perform(get("/empresas/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Empresa 999999 não encontrada"));
    }

    @Test
    void buscaPorIdNaoNumericoDevolve400() throws Exception {
        mockMvc.perform(get("/empresas/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Parâmetro inválido: id"));
    }

    @Test
    void rotaInexistenteDevolve404() throws Exception {
        mockMvc.perform(get("/nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void metodoNaoSuportadoDevolve405() throws Exception {
        mockMvc.perform(put("/empresas").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void preflightDoFlutterComPatchEhAceito() throws Exception {
        mockMvc.perform(options("/empresas")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void flutterAbertoPor127001TambemEhAceito() throws Exception {
        mockMvc.perform(options("/empresas")
                        .header("Origin", "http://127.0.0.1:3000")
                        .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:3000"));
    }

    @Test
    void origemForaDoLocalhostEhRecusada() throws Exception {
        mockMvc.perform(options("/empresas")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isForbidden());
    }
}
