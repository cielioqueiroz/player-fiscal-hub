package com.desafio.obrigacoes.dto;

public record ResumoEntregasResponse(
        long total,
        long abertas,
        long atrasadas,
        long concluidas,
        long canceladas,
        long venceEmAte7Dias,
        long vencidasSemBaixa) {
}
