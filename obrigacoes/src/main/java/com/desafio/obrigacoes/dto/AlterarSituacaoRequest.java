package com.desafio.obrigacoes.dto;

import com.desafio.obrigacoes.model.SituacaoEntrega;

import java.time.LocalDate;

public record AlterarSituacaoRequest(
        SituacaoEntrega situacao,
        LocalDate dataConclusao,
        String motivoCancelamento) {
}
