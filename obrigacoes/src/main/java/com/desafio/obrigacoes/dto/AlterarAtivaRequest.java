package com.desafio.obrigacoes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AlterarAtivaRequest(@Schema(example = "false") Boolean ativa) {
}
