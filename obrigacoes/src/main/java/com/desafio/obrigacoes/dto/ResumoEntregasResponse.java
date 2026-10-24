package com.desafio.obrigacoes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Contagens das entregas nos filtros solicitados")
public record ResumoEntregasResponse(
        @Schema(example = "15") long total,
        @Schema(example = "6") long abertas,
        @Schema(example = "2") long atrasadas,
        @Schema(example = "5") long concluidas,
        @Schema(example = "2") long canceladas,
        @Schema(example = "3") long venceEmAte7Dias,
        @Schema(example = "1") long vencidasSemBaixa) {
}
