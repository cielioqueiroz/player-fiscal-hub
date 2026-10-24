package com.desafio.obrigacoes.dto;

import com.desafio.obrigacoes.model.RegimeTributario;
import io.swagger.v3.oas.annotations.media.Schema;

public record EmpresaRequest(
        @Schema(example = "Padaria Aurora") String nome,
        @Schema(example = "11222333000181") String cnpj,
        @Schema(example = "SIMPLES") RegimeTributario regimeTributario) {
}
