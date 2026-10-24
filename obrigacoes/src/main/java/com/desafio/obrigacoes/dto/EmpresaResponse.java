package com.desafio.obrigacoes.dto;

import com.desafio.obrigacoes.model.Empresa;
import com.desafio.obrigacoes.model.RegimeTributario;

public record EmpresaResponse(Long id, String nome, String cnpj, RegimeTributario regimeTributario, boolean ativa) {

    public static EmpresaResponse de(Empresa e) {
        return new EmpresaResponse(e.getId(), e.getNome(), e.getCnpj(), e.getRegimeTributario(), e.isAtiva());
    }
}
