package com.desafio.obrigacoes.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.desafio.obrigacoes.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

class HealthOpenApiControllerIT extends BaseIntegrationTest {

    @Test
    void healthEstaExpostoSemDetalhes() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void openApiApresentaTituloEEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("API do Gerenciador de Obrigações"))
                .andExpect(jsonPath("$.paths['/entregas/resumo']").exists())
                .andExpect(jsonPath("$.paths['/documentos/{id}'].delete").exists());
    }

    @Test
    void outrosEndpointsActuatorNaoSaoExpostos() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isNotFound());
    }
}
