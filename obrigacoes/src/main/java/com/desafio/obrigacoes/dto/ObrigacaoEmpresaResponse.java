package com.desafio.obrigacoes.dto;

import com.desafio.obrigacoes.model.EmpresaObrigacao;
import com.desafio.obrigacoes.model.TipoObrigacao;

import java.time.LocalDateTime;

public record ObrigacaoEmpresaResponse(Long id, TipoObrigacao tipo, String descricao, LocalDateTime criadoEm) {

    public static ObrigacaoEmpresaResponse de(EmpresaObrigacao v) {
        return new ObrigacaoEmpresaResponse(v.getId(), v.getTipo(), v.getTipo().getDescricao(), v.getCriadoEm());
    }
}
