package com.desafio.obrigacoes.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.desafio.obrigacoes.exception.DadosInvalidosException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

class DocumentoConteudoTest {

    @Test
    void reconheceAssinaturasPdfPngEJpeg() {
        assertThatCode(() -> DocumentoService.validarConteudo("application/pdf", "%PDF-1.7".getBytes(StandardCharsets.UTF_8)))
                .doesNotThrowAnyException();
        assertThatCode(() -> DocumentoService.validarConteudo("image/png",
                new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}))
                .doesNotThrowAnyException();
        assertThatCode(() -> DocumentoService.validarConteudo("image/jpeg",
                new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}))
                .doesNotThrowAnyException();
    }

    @Test
    void aceitaXmlComEspacosETextoUtf8SemNulos() {
        assertThatCode(() -> DocumentoService.validarConteudo("text/xml", "  <?xml version=\"1.0\"?><a/>"
                .getBytes(StandardCharsets.UTF_8))).doesNotThrowAnyException();
        assertThatCode(() -> DocumentoService.validarConteudo("text/plain", "ação".getBytes(StandardCharsets.UTF_8)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejeitaAssinaturaIncompativelXmlMalformadoENulos() {
        assertThatThrownBy(() -> DocumentoService.validarConteudo("application/pdf", new byte[] {1}))
                .isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> DocumentoService.validarConteudo("text/xml", "conteudo".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> DocumentoService.validarConteudo("text/plain", new byte[] {(byte) 0xC3, 0x28}))
                .isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> DocumentoService.validarConteudo("text/plain", new byte[] {'a', 0, 'b'}))
                .isInstanceOf(DadosInvalidosException.class);
    }
}
