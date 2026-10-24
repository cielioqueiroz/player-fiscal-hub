package com.desafio.obrigacoes.dto;

import com.desafio.obrigacoes.model.Empresa;
import com.desafio.obrigacoes.model.Entrega;
import com.desafio.obrigacoes.model.SituacaoEntrega;
import com.desafio.obrigacoes.model.TipoObrigacao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record EntregaResponse(
        Long id,
        Long empresaId,
        String empresaNome,
        String empresaCnpj,
        TipoObrigacao tipo,
        String competencia,
        LocalDate dataVencimento,
        SituacaoEntrega situacao,
        LocalDate dataConclusao,
        String motivoCancelamento,
        LocalDateTime criadoEm,
        boolean vencida,
        List<SituacaoEntrega> transicoesPermitidas) {

    public static EntregaResponse de(Entrega e, LocalDate hoje) {
        Empresa empresa = e.getEmpresa();
        List<SituacaoEntrega> transicoes = Arrays.stream(SituacaoEntrega.values())
                .filter(e.getSituacao()::podeIrPara)
                .filter(s -> s != SituacaoEntrega.ATRASADA || hoje.isAfter(e.getDataVencimento()))
                .toList();
        return new EntregaResponse(
                e.getId(),
                empresa.getId(),
                empresa.getNome(),
                empresa.getCnpj(),
                e.getTipo(),
                e.getCompetencia(),
                e.getDataVencimento(),
                e.getSituacao(),
                e.getDataConclusao(),
                e.getMotivoCancelamento(),
                e.getCriadoEm(),
                e.isVencida(hoje),
                transicoes);
    }
}
