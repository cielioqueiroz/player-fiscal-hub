package com.desafio.obrigacoes.dto;

import com.desafio.obrigacoes.model.SituacaoEntrega;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.media.Schema;

public record AlterarSituacaoRequest(
        @Schema(example = "CONCLUIDA") SituacaoEntrega situacao,
        @Schema(example = "2026-09-18") LocalDate dataConclusao,
        @Schema(example = "Obrigação encerrada") String motivoCancelamento) {
}
