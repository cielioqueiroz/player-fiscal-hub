package com.desafio.obrigacoes.dto;

import com.desafio.obrigacoes.model.Empresa;
import com.desafio.obrigacoes.model.RegimeTributario;
import io.swagger.v3.oas.annotations.media.Schema;

public record EmpresaResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Padaria Aurora") String nome,
        @Schema(example = "11222333000181") String cnpj,
        @Schema(example = "SIMPLES") RegimeTributario regimeTributario,
        @Schema(example = "true") boolean ativa) {

    public static EmpresaResponse de(Empresa e) {
        return new EmpresaResponse(e.getId(), e.getNome(), e.getCnpj(), e.getRegimeTributario(), e.isAtiva());
    }
}
