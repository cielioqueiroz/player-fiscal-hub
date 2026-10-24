package com.desafio.obrigacoes.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.desafio.obrigacoes.BaseIntegrationTest;
import com.desafio.obrigacoes.dto.AlterarSituacaoRequest;
import com.desafio.obrigacoes.dto.EntregaResponse;
import com.desafio.obrigacoes.exception.RegraNegocioException;
import com.desafio.obrigacoes.model.RegimeTributario;
import com.desafio.obrigacoes.model.SituacaoEntrega;
import com.desafio.obrigacoes.repository.EntregaRepository;
import com.desafio.obrigacoes.service.EntregaService;
import com.desafio.obrigacoes.service.GeracaoEntregaService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

class AlterarSituacaoIT extends BaseIntegrationTest {

    @Autowired GeracaoEntregaService geracaoService;
    @Autowired EntregaService entregaService;
    @MockitoSpyBean EntregaRepository entregaRepository;

    private long das;
    private long dctf;

    @BeforeEach
    void criarMassa() {
        long empresaA = criarEmpresa("11222333000181", RegimeTributario.SIMPLES);
        long empresaB = criarEmpresa("11444777000161", RegimeTributario.LUCRO_REAL);
        vincular(empresaA, "DAS");
        vincular(empresaA, "ECD");
        vincular(empresaB, "DCTF");
        vincular(empresaB, "ECD");
        geracaoService.gerar(YearMonth.of(2026, 5));

        das = idDaEntrega(empresaA, "DAS");
        dctf = idDaEntrega(empresaB, "DCTF");
    }

    private long criarEmpresa(String cnpj, RegimeTributario regime) {
        jdbc.update("INSERT INTO empresa (nome, cnpj, regime_tributario) VALUES ('Teste', ?, ?)", cnpj, regime.name());
        return jdbc.queryForObject("SELECT id FROM empresa WHERE cnpj = ?", Long.class, cnpj);
    }

    private void vincular(long empresaId, String tipo) {
        jdbc.update("INSERT INTO empresa_obrigacao (empresa_id, tipo, criado_em) VALUES (?, ?, NOW())", empresaId, tipo);
    }

    private long idDaEntrega(long empresaId, String tipo) {
        return jdbc.queryForObject("SELECT id FROM entrega WHERE empresa_id = ? AND tipo = ?", Long.class, empresaId, tipo);
    }

    private ResultActions patch(Object id, String json) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.patch("/entregas/{id}/situacao", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private static final String CONCLUIR = """
            {"situacao":"CONCLUIDA","dataConclusao":"2026-06-18"}""";
    private static final String CANCELAR = """
            {"situacao":"CANCELADA","motivoCancelamento":"Empresa encerrou atividades"}""";
    private static final String ATRASAR = """
            {"situacao":"ATRASADA"}""";

    @Test
    void concluirDas() throws Exception {
        patch(das, CONCLUIR)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("CONCLUIDA"))
                .andExpect(jsonPath("$.dataConclusao").value("2026-06-18"))
                .andExpect(jsonPath("$.vencida").value(false))
                .andExpect(jsonPath("$.transicoesPermitidas.length()").value(0));
    }

    @Test
    void concluirSemDataDevolve400() throws Exception {
        patch(das, """
                {"situacao":"CONCLUIDA"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("dataConclusao é obrigatória para concluir a entrega"));
    }

    @Test
    void concluirComDataFuturaDevolve400() throws Exception {
        patch(das, """
                {"situacao":"CONCLUIDA","dataConclusao":"2026-07-02"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("dataConclusao não pode estar no futuro"));
    }

    @Test
    void cancelarDas() throws Exception {
        patch(das, CANCELAR)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("CANCELADA"))
                .andExpect(jsonPath("$.motivoCancelamento").value("Empresa encerrou atividades"));
    }

    @Test
    void cancelarComMotivoEmBrancoDevolve400() throws Exception {
        patch(das, """
                {"situacao":"CANCELADA","motivoCancelamento":"   "}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("motivoCancelamento é obrigatório para cancelar a entrega"));
    }

    @Test
    void marcarDasAtrasada() throws Exception {
        patch(das, ATRASAR)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("ATRASADA"))
                .andExpect(jsonPath("$.transicoesPermitidas.length()").value(2))
                .andExpect(jsonPath("$.transicoesPermitidas[0]").value("CONCLUIDA"))
                .andExpect(jsonPath("$.transicoesPermitidas[1]").value("CANCELADA"));
    }

    @Test
    void marcarDctfAtrasadaDevolve422() throws Exception {
        patch(dctf, ATRASAR)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.erro").value("Só é possível marcar como ATRASADA após o vencimento (2026-07-15)"));
    }

    @Test
    void concluidaParaCanceladaDevolve422() throws Exception {
        patch(das, CONCLUIR).andExpect(status().isOk());

        patch(das, CANCELAR)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.erro").value("Não é possível alterar uma entrega CONCLUIDA"));
    }

    @Test
    void canceladaParaConcluidaDevolve422() throws Exception {
        patch(das, CANCELAR).andExpect(status().isOk());

        patch(das, CONCLUIR)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.erro").value("Não é possível alterar uma entrega CANCELADA"));
    }

    @Test
    void voltarParaAbertaDevolve422() throws Exception {
        patch(das, """
                {"situacao":"ABERTA"}""")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.erro").value("Transição de ABERTA para ABERTA não é permitida"));
    }

    @Test
    void semSituacaoDevolve400() throws Exception {
        patch(das, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("situacao é obrigatória"));
    }

    @Test
    void situacaoDesconhecidaDevolve400() throws Exception {
        patch(das, """
                {"situacao":"XYZ"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Corpo da requisição inválido ou com valor desconhecido"));
    }

    @Test
    void entregaInexistenteDevolve404() throws Exception {
        patch(999999, ATRASAR)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Entrega 999999 não encontrada"));
    }

    @Test
    void semCorpoDevolve400() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch("/entregas/{id}/situacao", das)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("situacao é obrigatória"));
    }

    @Test
    void camposDeOutraTransicaoSaoIgnorados() throws Exception {
        patch(das, """
                {"situacao":"CONCLUIDA","dataConclusao":"2026-06-18","motivoCancelamento":"x"}""")
                .andExpect(status().isOk());
        patch(dctf, """
                {"situacao":"CANCELADA","motivoCancelamento":"Encerrou","dataConclusao":"2026-06-18"}""")
                .andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT motivo_cancelamento FROM entrega WHERE id = ?", String.class, das))
                .isNull();
        assertThat(jdbc.queryForObject("SELECT data_conclusao FROM entrega WHERE id = ?", LocalDate.class, dctf))
                .isNull();
    }

    @Test
    void dataMalFormatadaDevolve400() throws Exception {
        patch(das, """
                {"situacao":"CONCLUIDA","dataConclusao":"18/06/2026"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Corpo da requisição inválido ou com valor desconhecido"));
    }

    @Test
    void idNaoNumericoDevolve400() throws Exception {
        patch("abc", ATRASAR)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Parâmetro inválido: id"));
    }

    @Test
    void conflitoDeVersaoViaHttpDevolve409() throws Exception {
        Answer<?> real = mockingDetails(entregaRepository).getMockCreationSettings().getDefaultAnswer();
        doAnswer(invocacao -> {
            Object lida = real.answer(invocacao);
            jdbc.update("UPDATE entrega SET versao = versao + 1 WHERE id = ?", das);
            return lida;
        }).when(entregaRepository).findById(das);

        patch(das, CONCLUIR)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("A entrega foi alterada por outra pessoa. Recarregue e tente novamente."));

        assertThat(jdbc.queryForObject("SELECT situacao FROM entrega WHERE id = ?", String.class, das))
                .isEqualTo("ABERTA");
    }

    @RepeatedTest(5)
    void concluirECancelarAoMesmoTempo() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch largada = new CountDownLatch(1);
            List<Future<EntregaResponse>> resultados = List.of(
                    pool.submit(() -> {
                        largada.await();
                        return entregaService.alterarSituacao(das, new AlterarSituacaoRequest(
                                SituacaoEntrega.CONCLUIDA, LocalDate.of(2026, 6, 18), null));
                    }),
                    pool.submit(() -> {
                        largada.await();
                        return entregaService.alterarSituacao(das, new AlterarSituacaoRequest(
                                SituacaoEntrega.CANCELADA, null, "Encerrou"));
                    }));

            largada.countDown();

            int sucessos = 0;
            List<Throwable> falhas = new ArrayList<>();
            for (Future<EntregaResponse> r : resultados) {
                try {
                    r.get(30, TimeUnit.SECONDS);
                    sucessos++;
                } catch (ExecutionException e) {
                    falhas.add(e.getCause());
                }
            }

            assertThat(sucessos).isEqualTo(1);
            assertThat(falhas).singleElement()
                    .isInstanceOfAny(ObjectOptimisticLockingFailureException.class, RegraNegocioException.class);

            Map<String, Object> linha = jdbc.queryForMap(
                    "SELECT situacao, data_conclusao, motivo_cancelamento, versao FROM entrega WHERE id = ?", das);
            assertThat(((Number) linha.get("versao")).longValue()).isEqualTo(1);
            if ("CONCLUIDA".equals(linha.get("situacao"))) {
                assertThat(linha.get("data_conclusao")).hasToString("2026-06-18");
                assertThat(linha.get("motivo_cancelamento")).isNull();
            } else {
                assertThat(linha.get("situacao")).isEqualTo("CANCELADA");
                assertThat(linha.get("motivo_cancelamento")).isEqualTo("Encerrou");
                assertThat(linha.get("data_conclusao")).isNull();
            }
        } finally {
            pool.shutdown();
        }
    }

    @Test
    void detalheDaDasAbertaEstaVencida() throws Exception {
        mockMvc.perform(get("/entregas/{id}", das))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vencida").value(true))
                .andExpect(jsonPath("$.transicoesPermitidas.length()").value(3))
                .andExpect(jsonPath("$.transicoesPermitidas[0]").value("ATRASADA"))
                .andExpect(jsonPath("$.transicoesPermitidas[1]").value("CONCLUIDA"))
                .andExpect(jsonPath("$.transicoesPermitidas[2]").value("CANCELADA"));
    }

    @Test
    void detalheDaDctfAbertaNaoEstaVencida() throws Exception {
        mockMvc.perform(get("/entregas/{id}", dctf))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vencida").value(false))
                .andExpect(jsonPath("$.transicoesPermitidas.length()").value(2))
                .andExpect(jsonPath("$.transicoesPermitidas[0]").value("CONCLUIDA"))
                .andExpect(jsonPath("$.transicoesPermitidas[1]").value("CANCELADA"));
    }

    @Test
    void listagemTrazVencidaETransicoes() throws Exception {
        mockMvc.perform(get("/entregas").param("tipo", "DAS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vencida").value(true))
                .andExpect(jsonPath("$[0].transicoesPermitidas.length()").value(3))
                .andExpect(jsonPath("$[0].transicoesPermitidas[0]").value("ATRASADA"))
                .andExpect(jsonPath("$[0].transicoesPermitidas[1]").value("CONCLUIDA"))
                .andExpect(jsonPath("$[0].transicoesPermitidas[2]").value("CANCELADA"));
    }
}
