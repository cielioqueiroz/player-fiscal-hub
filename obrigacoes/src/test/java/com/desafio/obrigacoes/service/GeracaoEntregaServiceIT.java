package com.desafio.obrigacoes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.desafio.obrigacoes.BaseIntegrationTest;
import com.desafio.obrigacoes.dto.ResumoGeracaoResponse;
import com.desafio.obrigacoes.model.RegimeTributario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class GeracaoEntregaServiceIT extends BaseIntegrationTest {

    private static final YearMonth MAIO = YearMonth.of(2026, 5);

    @Autowired GeracaoEntregaService geracaoService;

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

    private int contarEntregas() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM entrega", Integer.class);
    }

    @Test
    void gerarMaioCriaQuatro() {
        assertThat(geracaoService.gerar(MAIO)).isEqualTo(new ResumoGeracaoResponse("2026-05", 4, 0));
    }

    @Test
    void empresaInativaNaoGeraNovasEntregas() {
        jdbc.update("UPDATE empresa SET ativa = FALSE WHERE id = ?", empresaA);

        assertThat(geracaoService.gerar(MAIO)).isEqualTo(new ResumoGeracaoResponse("2026-05", 2, 0));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM entrega WHERE empresa_id = ?", Integer.class, empresaA))
                .isZero();
    }

    @Test
    void gerarMaioGravaCompetenciaVencimentoESituacao() {
        geracaoService.gerar(MAIO);

        List<Map<String, Object>> linhas = jdbc.queryForList("""
                SELECT empresa_id, tipo, competencia, data_vencimento, situacao, versao, criado_em
                FROM entrega ORDER BY empresa_id, tipo""");

        assertThat(linhas).hasSize(4);
        assertLinha(linhas.get(0), empresaA, "DAS", "2026-05", "2026-06-20");
        assertLinha(linhas.get(1), empresaA, "ECD", "2025", "2026-05-31");
        assertLinha(linhas.get(2), empresaB, "DCTF", "2026-05", "2026-07-15");
        assertLinha(linhas.get(3), empresaB, "ECD", "2025", "2026-05-31");
    }

    private void assertLinha(Map<String, Object> linha, long empresaId, String tipo, String competencia, String vencimento) {
        assertThat(((Number) linha.get("empresa_id")).longValue()).isEqualTo(empresaId);
        assertThat(linha.get("tipo")).isEqualTo(tipo);
        assertThat(linha.get("competencia")).isEqualTo(competencia);
        assertThat(linha.get("data_vencimento").toString()).isEqualTo(vencimento);
        assertThat(linha.get("situacao")).isEqualTo("ABERTA");
        assertThat(((Number) linha.get("versao")).longValue()).isZero();
        assertThat(linha.get("criado_em")).isNotNull();
    }

    @Test
    void gerarDuasVezesNaoDuplica() {
        geracaoService.gerar(MAIO);

        assertThat(geracaoService.gerar(MAIO)).isEqualTo(new ResumoGeracaoResponse("2026-05", 0, 4));
        assertThat(contarEntregas()).isEqualTo(4);
    }

    @Test
    void gerarJunhoReaproveitaEcdDe2025() {
        geracaoService.gerar(MAIO);

        assertThat(geracaoService.gerar(YearMonth.of(2026, 6))).isEqualTo(new ResumoGeracaoResponse("2026-06", 2, 2));
        assertThat(contarEntregas()).isEqualTo(6);
    }

    @Test
    void novaEmpresaSoGeraOQueFalta() {
        geracaoService.gerar(MAIO);
        long empresaC = criarEmpresa("12345678000195", RegimeTributario.SIMPLES);
        vincular(empresaC, "DAS");

        assertThat(geracaoService.gerar(MAIO)).isEqualTo(new ResumoGeracaoResponse("2026-05", 1, 4));
    }

    @Test
    void entregaExistenteNaoEhAlterada() {
        geracaoService.gerar(MAIO);
        jdbc.update("UPDATE entrega SET situacao = 'CANCELADA' WHERE empresa_id = ? AND tipo = 'DAS'", empresaA);

        assertThat(geracaoService.gerar(MAIO)).isEqualTo(new ResumoGeracaoResponse("2026-05", 0, 4));

        Map<String, Object> linha = jdbc.queryForMap(
                "SELECT situacao, data_vencimento FROM entrega WHERE empresa_id = ? AND tipo = 'DAS'", empresaA);
        assertThat(linha.get("situacao")).isEqualTo("CANCELADA");
        assertThat(linha.get("data_vencimento").toString()).isEqualTo("2026-06-20");
    }

    @Test
    void semVinculosDevolveZeros() {
        jdbc.update("DELETE FROM empresa_obrigacao");

        assertThat(geracaoService.gerar(MAIO)).isEqualTo(new ResumoGeracaoResponse("2026-05", 0, 0));
        assertThat(contarEntregas()).isZero();
    }

    @Test
    void constraintDoBancoRecusaDuplicataSemIgnore() {
        geracaoService.gerar(MAIO);

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO entrega (empresa_id, tipo, competencia, data_vencimento, situacao, versao, criado_em) "
                        + "VALUES (?, 'DAS', '2026-05', '2026-06-20', 'ABERTA', 0, NOW())", empresaA))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @RepeatedTest(5)
    void geracoesSimultaneasNaoDuplicam() throws Exception {
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch largada = new CountDownLatch(1);
            List<Future<ResumoGeracaoResponse>> resultados = new ArrayList<>();

            for (int i = 0; i < threads; i++) {
                resultados.add(pool.submit(() -> {
                    largada.await();
                    return geracaoService.gerar(YearMonth.of(2026, 5));
                }));
            }

            largada.countDown();

            int totalCriadas = 0;
            int totalJaExistentes = 0;
            for (Future<ResumoGeracaoResponse> r : resultados) {
                ResumoGeracaoResponse resumo = r.get(30, TimeUnit.SECONDS);
                totalCriadas += resumo.criadas();
                totalJaExistentes += resumo.jaExistentes();
            }

            assertThat(totalCriadas).isEqualTo(4);
            assertThat(totalCriadas + totalJaExistentes).isEqualTo(threads * 4);
            assertThat(contarEntregas()).isEqualTo(4);
        } finally {
            pool.shutdown();
        }
    }
}
