package com.desafio.obrigacoes.dto;

import com.desafio.obrigacoes.model.Documento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record DocumentoResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "guia.pdf") String nomeArquivo,
        @Schema(example = "application/pdf") String contentType,
        @Schema(example = "245760") Long tamanhoBytes,
        @Schema(example = "2026-06-18T10:00:00") LocalDateTime enviadoEm) {

    public static DocumentoResponse de(Documento d) {
        return new DocumentoResponse(d.getId(), d.getNomeArquivo(), d.getContentType(),
                d.getTamanhoBytes(), d.getEnviadoEm());
    }
}
