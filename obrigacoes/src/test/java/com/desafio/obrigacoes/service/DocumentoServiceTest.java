package com.desafio.obrigacoes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.desafio.obrigacoes.dto.DocumentoResponse;
import com.desafio.obrigacoes.model.Documento;
import com.desafio.obrigacoes.model.Empresa;
import com.desafio.obrigacoes.model.Entrega;
import com.desafio.obrigacoes.model.TipoObrigacao;
import com.desafio.obrigacoes.repository.DocumentoRepository;
import com.desafio.obrigacoes.repository.EntregaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DocumentoServiceTest {

    private static final Clock RELOGIO_COM_FRACAO = Clock.fixed(Instant.parse("2026-07-01T12:00:00.987654Z"), ZoneOffset.UTC);

    private DocumentoService serviceComEntregaAberta() {
        EntregaRepository entregas = mock(EntregaRepository.class);
        Entrega entrega = new Entrega(new Empresa(), TipoObrigacao.DAS, "2026-05",
                LocalDate.of(2026, 6, 20), LocalDateTime.of(2026, 5, 1, 0, 0));
        when(entregas.buscarParaAlteracao(1L)).thenReturn(Optional.of(entrega));
        DocumentoRepository documentos = mock(DocumentoRepository.class);
        when(documentos.save(any(Documento.class))).thenAnswer(i -> i.getArgument(0));
        return new DocumentoService(documentos, entregas, RELOGIO_COM_FRACAO);
    }

    @Test
    void enviadoEmVemSemFracaoDeSegundo() {
        DocumentoResponse resposta = serviceComEntregaAberta()
                .anexar(1L, new MockMultipartFile("arquivo", "guia.pdf", "application/pdf", new byte[] {1}));

        assertThat(resposta.enviadoEm()).isEqualTo(LocalDateTime.of(2026, 7, 1, 12, 0, 0));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "guia.pdf                  | guia.pdf",
            "C:\\fakepath\\guia.pdf    | guia.pdf",
            "/home/ana/docs/guia.pdf   | guia.pdf",
            "C:\\pasta\\               | documento",
            "'  recibo dctf.pdf  '     | recibo dctf.pdf"})
    void limpaCaminhoEEspacos(String original, String esperado) {
        assertThat(DocumentoService.limparNome(original)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void nomeAusenteViraDocumento(String original) {
        assertThat(DocumentoService.limparNome(original)).isEqualTo("documento");
    }

    @Test
    void caracteresDeControleSaoRemovidos() {
        assertThat(DocumentoService.limparNome("gu\u0000ia\n.pdf")).isEqualTo("guia.pdf");
    }

    @Test
    void controlesC1EDeFormatacaoSaoRemovidos() {
        assertThat(DocumentoService.limparNome("gu‮ia​.pdf")).isEqualTo("guia.pdf");
        assertThat(DocumentoService.limparNome("﻿guia\u0085.pdf")).isEqualTo("guia.pdf");
    }

    @Test
    void nomeLongoPreservaAExtensao() {
        String limpo = DocumentoService.limparNome("a".repeat(300) + ".pdf");

        assertThat(limpo).hasSize(255).endsWith(".pdf");
    }

    @Test
    void corteNaoPartePardeSurrogate() {
        String emoji = "😀";

        String limpo = DocumentoService.limparNome(emoji.repeat(300) + ".pdf");

        assertThat(limpo.codePointCount(0, limpo.length())).isEqualTo(255);
        assertThat(limpo).endsWith(".pdf");
        assertThat(limpo.codePoints().noneMatch(cp -> cp >= 0xD800 && cp <= 0xDFFF)).isTrue();
    }

    @Test
    void nomeLongoCortadoEm255() {
        String limpo = DocumentoService.limparNome("a".repeat(300) + ".pdf");

        assertThat(limpo).hasSize(255).startsWith("aaa");
    }
}
