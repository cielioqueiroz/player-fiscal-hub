package com.desafio.obrigacoes.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.desafio.obrigacoes.BaseIntegrationTest;
import com.desafio.obrigacoes.dto.AlterarSituacaoRequest;
import com.desafio.obrigacoes.model.RegimeTributario;
import com.desafio.obrigacoes.model.SituacaoEntrega;
import com.desafio.obrigacoes.repository.EntregaRepository;
import com.desafio.obrigacoes.service.EntregaService;
import com.desafio.obrigacoes.service.GeracaoEntregaService;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class DocumentoControllerIT extends BaseIntegrationTest {

    private static final int DEZ_MB = 10 * 1024 * 1024;

    @Autowired GeracaoEntregaService geracaoService;
    @Autowired EntregaService entregaService;

    private long entrega;

    @BeforeEach
    void criarMassa() {
        long empresa = criarEmpresa("11222333000181", RegimeTributario.SIMPLES);
        vincular(empresa, "DAS");
        geracaoService.gerar(YearMonth.of(2026, 5));

        entrega = idDaEntrega(empresa, "DAS");
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

    private MockMultipartFile pdf(String nome, byte[] bytes) {
        return new MockMultipartFile("arquivo", nome, "application/pdf", bytes);
    }

    private ResultActions enviar(Object entregaId, MockMultipartFile arquivo) throws Exception {
        return mockMvc.perform(multipart("/entregas/{id}/documentos", entregaId).file(arquivo));
    }

    private int documentosGravados() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM documento WHERE entrega_id = ?", Integer.class, entrega);
    }

    private void concluir() {
        entregaService.alterarSituacao(entrega,
                new AlterarSituacaoRequest(SituacaoEntrega.CONCLUIDA, LocalDate.of(2026, 6, 18), null));
    }

    @Test
    void pdfEmEntregaAbertaDevolve201() throws Exception {
        enviar(entrega, pdf("guia.pdf", new byte[] {1, 2, 3}))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nomeArquivo").value("guia.pdf"))
                .andExpect(jsonPath("$.contentType").value("application/pdf"))
                .andExpect(jsonPath("$.tamanhoBytes").value(3))
                .andExpect(jsonPath("$.enviadoEm", startsWith("2026-07-01T")));

        assertThat(documentosGravados()).isEqualTo(1);
    }

    @Test
    void nomeComCaminhoESalvoSemPastas() throws Exception {
        enviar(entrega, pdf("C:\\fakepath\\guia.pdf", new byte[] {1}))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomeArquivo").value("guia.pdf"));
    }

    @Test
    void arquivoVazioDevolve400() throws Exception {
        enviar(entrega, pdf("guia.pdf", new byte[0]))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Arquivo é obrigatório"));
    }

    @Test
    void semCampoArquivoDevolve400() throws Exception {
        mockMvc.perform(multipart("/entregas/{id}/documentos", entrega))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Arquivo é obrigatório (campo 'arquivo')"));
    }

    @Test
    void tipoNaoPermitidoDevolve400() throws Exception {
        enviar(entrega, new MockMultipartFile("arquivo", "setup.exe", "application/x-msdownload", new byte[] {1}))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Tipo de arquivo não permitido: application/x-msdownload"));

        assertThat(documentosGravados()).isZero();
    }

    @Test
    void tipoComParametroEAceitoEGravadoNormalizado() throws Exception {
        enviar(entrega, new MockMultipartFile("arquivo", "nfe.xml", "text/xml; charset=utf-8", new byte[] {1}))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType").value("text/xml"));
    }

    @Test
    void tipoEmMaiusculasEAceitoEGravadoNormalizado() throws Exception {
        enviar(entrega, new MockMultipartFile("arquivo", "guia.pdf", "Application/PDF", new byte[] {1}))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType").value("application/pdf"));
    }

    @Test
    void tipoQueNaoEMediaTypeDevolve400() throws Exception {
        enviar(entrega, new MockMultipartFile("arquivo", "guia.pdf", "isso-nao-e-tipo", new byte[] {1}))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Tipo de arquivo não permitido: isso-nao-e-tipo"));
    }

    @Test
    void arquivoSemTipoDevolve400() throws Exception {
        enviar(entrega, new MockMultipartFile("arquivo", "guia.pdf", null, new byte[] {1}))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Tipo de arquivo não permitido: null"));
    }

    @Test
    void arquivoAcimaDe10MbDevolve400() throws Exception {
        enviar(entrega, pdf("grande.pdf", new byte[DEZ_MB + 1]))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O arquivo excede o limite de 10 MB"));
    }

    @Test
    void arquivoDeExatamente10MbEAceito() throws Exception {
        enviar(entrega, pdf("limite.pdf", new byte[DEZ_MB]))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tamanhoBytes").value(DEZ_MB));
    }

    @Test
    void entregaConcluidaDevolve422() throws Exception {
        concluir();

        enviar(entrega, pdf("guia.pdf", new byte[] {1}))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.erro").value("Não é possível anexar documentos a uma entrega CONCLUIDA"));

        assertThat(documentosGravados()).isZero();
    }

    @Test
    void entregaCanceladaDevolve422() throws Exception {
        entregaService.alterarSituacao(entrega, new AlterarSituacaoRequest(SituacaoEntrega.CANCELADA, null, "Encerrou"));

        enviar(entrega, pdf("guia.pdf", new byte[] {1}))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.erro").value("Não é possível anexar documentos a uma entrega CANCELADA"));

        assertThat(documentosGravados()).isZero();
    }

    @Test
    void entregaConcluidaComArquivoVazioDevolve422() throws Exception {
        concluir();

        enviar(entrega, pdf("guia.pdf", new byte[0]))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void entregaAtrasadaAceita201() throws Exception {
        entregaService.alterarSituacao(entrega, new AlterarSituacaoRequest(SituacaoEntrega.ATRASADA, null, null));

        enviar(entrega, pdf("guia.pdf", new byte[] {1}))
                .andExpect(status().isCreated());
    }

    @Test
    void uploadEmEntregaInexistenteDevolve404() throws Exception {
        enviar(999999, pdf("guia.pdf", new byte[] {1}))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Entrega 999999 não encontrada"));
    }

    @Test
    void requisicaoQueNaoEMultipartDevolve415() throws Exception {
        mockMvc.perform(post("/entregas/{id}/documentos", entrega)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void documentoEnviadoApareceNaLista() throws Exception {
        enviar(entrega, pdf("guia.pdf", new byte[] {1, 2, 3})).andExpect(status().isCreated());

        mockMvc.perform(get("/entregas/{id}/documentos", entrega))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nomeArquivo").value("guia.pdf"))
                .andExpect(jsonPath("$[0].tamanhoBytes").value(3));
    }

    @Test
    void doisArquivosNaOrdemDeEnvio() throws Exception {
        enviar(entrega, pdf("guia.pdf", new byte[] {1})).andExpect(status().isCreated());
        enviar(entrega, pdf("recibo.pdf", new byte[] {2})).andExpect(status().isCreated());

        mockMvc.perform(get("/entregas/{id}/documentos", entrega))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nomeArquivo").value("guia.pdf"))
                .andExpect(jsonPath("$[1].nomeArquivo").value("recibo.pdf"));
    }

    @Test
    void listagemOrdenaPorEnviadoEmAntesDoId() throws Exception {
        inserirDocumento("recente.pdf", LocalDateTime.of(2026, 7, 1, 12, 0, 0));
        inserirDocumento("antigo.pdf", LocalDateTime.of(2026, 6, 1, 12, 0, 0));

        mockMvc.perform(get("/entregas/{id}/documentos", entrega))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nomeArquivo").value("antigo.pdf"))
                .andExpect(jsonPath("$[1].nomeArquivo").value("recente.pdf"));
    }

    private void inserirDocumento(String nome, LocalDateTime enviadoEm) {
        jdbc.update("""
                INSERT INTO documento (entrega_id, nome_arquivo, content_type, tamanho_bytes, conteudo, enviado_em)
                VALUES (?, ?, 'application/pdf', 1, ?, ?)
                """, entrega, nome, new byte[] {1}, enviadoEm);
    }

    @Test
    void listagemNaoTrazConteudo() throws Exception {
        enviar(entrega, pdf("guia.pdf", new byte[] {1})).andExpect(status().isCreated());

        mockMvc.perform(get("/entregas/{id}/documentos", entrega))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].conteudo").doesNotExist());
    }

    @Test
    void entregaSemDocumentosDevolveListaVazia() throws Exception {
        mockMvc.perform(get("/entregas/{id}/documentos", entrega))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listaDeEntregaInexistenteDevolve404() throws Exception {
        mockMvc.perform(get("/entregas/{id}/documentos", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Entrega 999999 não encontrada"));
    }

    private long idDoDocumento(MockMultipartFile arquivo) throws Exception {
        String json = enviar(entrega, arquivo).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private static ContentDisposition disposicao(MvcResult resultado) {
        return ContentDisposition.parse(resultado.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION));
    }

    @Test
    void downloadDevolveBytesIdenticos() throws Exception {
        byte[] bytes = "%PDF-1.4 teste".getBytes(StandardCharsets.UTF_8);
        long id = idDoDocumento(pdf("guia.pdf", bytes));

        MvcResult resultado = mockMvc.perform(get("/documentos/{id}/download", id))
                .andExpect(status().isOk())
                .andExpect(content().bytes(bytes))
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andReturn();

        assertThat(disposicao(resultado).isAttachment()).isTrue();
        assertThat(disposicao(resultado).getFilename()).isEqualTo("guia.pdf");
    }

    @Test
    void nomeComAcentoChegaCorretoNoDownload() throws Exception {
        long id = idDoDocumento(pdf("guia_março.pdf", new byte[] {1}));

        MvcResult resultado = mockMvc.perform(get("/documentos/{id}/download", id))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(disposicao(resultado).getFilename()).isEqualTo("guia_março.pdf");
    }

    @Test
    void downloadInexistenteDevolve404() throws Exception {
        mockMvc.perform(get("/documentos/{id}/download", 999999))
                .andExpect(status().isNotFound());
    }

    @Autowired PlatformTransactionManager txManager;
    @Autowired EntregaRepository entregaRepository;

    @Test
    void buscarParaAlteracaoBloqueiaALinha() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Throwable causaDoBloqueio = new TransactionTemplate(txManager).execute(status -> {
                entregaRepository.buscarParaAlteracao(entrega).orElseThrow();
                try {
                    pool.submit(() -> jdbc.queryForObject(
                                    "SELECT id FROM entrega WHERE id = ? FOR UPDATE NOWAIT", Long.class, entrega))
                            .get(30, TimeUnit.SECONDS);
                    return null;
                } catch (ExecutionException e) {
                    return e.getCause();
                } catch (InterruptedException | TimeoutException e) {
                    throw new IllegalStateException(e);
                }
            });

            assertThat(causaDoBloqueio).isInstanceOf(UncategorizedSQLException.class);
            assertThat(((UncategorizedSQLException) causaDoBloqueio).getSQLException().getErrorCode()).isEqualTo(3572);
        } finally {
            pool.shutdown();
        }

        Long reencontrado = jdbc.queryForObject(
                "SELECT id FROM entrega WHERE id = ? FOR UPDATE NOWAIT", Long.class, entrega);
        assertThat(reencontrado).isEqualTo(entrega);
    }
}
