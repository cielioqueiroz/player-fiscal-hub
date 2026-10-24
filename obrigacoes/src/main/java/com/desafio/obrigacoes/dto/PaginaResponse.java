package com.desafio.obrigacoes.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record PaginaResponse<T>(List<T> conteudo, int pagina, int tamanho, long totalElementos, int totalPaginas) {

    public static <S, T> PaginaResponse<T> de(Page<S> page, Function<S, T> conversor) {
        return new PaginaResponse<>(page.getContent().stream().map(conversor).toList(), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
