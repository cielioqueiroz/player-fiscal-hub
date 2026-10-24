package com.desafio.obrigacoes.dto;

import com.desafio.obrigacoes.model.Documento;

import java.time.LocalDateTime;

public record DocumentoResponse(
        Long id,
        String nomeArquivo,
        String contentType,
        Long tamanhoBytes,
        LocalDateTime enviadoEm) {

    public static DocumentoResponse de(Documento d) {
        return new DocumentoResponse(d.getId(), d.getNomeArquivo(), d.getContentType(),
                d.getTamanhoBytes(), d.getEnviadoEm());
    }
}
