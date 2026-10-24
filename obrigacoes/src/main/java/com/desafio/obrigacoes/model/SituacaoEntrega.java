package com.desafio.obrigacoes.model;

public enum SituacaoEntrega {

    ABERTA,
    ATRASADA,
    CONCLUIDA,
    CANCELADA;

    public boolean podeIrPara(SituacaoEntrega destino) {
        return switch (this) {
            case ABERTA -> destino == ATRASADA || destino == CONCLUIDA || destino == CANCELADA;
            case ATRASADA -> destino == CONCLUIDA || destino == CANCELADA;
            case CONCLUIDA, CANCELADA -> false;
        };
    }

    public boolean isFinal() {
        return this == CONCLUIDA || this == CANCELADA;
    }
}
