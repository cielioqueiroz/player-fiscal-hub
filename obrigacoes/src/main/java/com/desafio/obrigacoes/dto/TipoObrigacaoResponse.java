package com.desafio.obrigacoes.dto;

import com.desafio.obrigacoes.model.Periodicidade;
import com.desafio.obrigacoes.model.RegimeTributario;
import com.desafio.obrigacoes.model.TipoObrigacao;

import java.util.Set;

public record TipoObrigacaoResponse(
        TipoObrigacao tipo,
        String descricao,
        Periodicidade periodicidade,
        Set<RegimeTributario> regimesAceitos) {

    public static TipoObrigacaoResponse de(TipoObrigacao tipo) {
        return new TipoObrigacaoResponse(tipo, tipo.getDescricao(), tipo.getPeriodicidade(), tipo.getRegimesAceitos());
    }
}
