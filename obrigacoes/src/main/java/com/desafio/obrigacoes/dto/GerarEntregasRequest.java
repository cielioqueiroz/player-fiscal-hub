package com.desafio.obrigacoes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record GerarEntregasRequest(@Schema(example = "2026-09") String mesReferencia) {
}
