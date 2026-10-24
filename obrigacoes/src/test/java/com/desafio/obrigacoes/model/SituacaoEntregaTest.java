package com.desafio.obrigacoes.model;

import static com.desafio.obrigacoes.model.SituacaoEntrega.ABERTA;
import static com.desafio.obrigacoes.model.SituacaoEntrega.ATRASADA;
import static com.desafio.obrigacoes.model.SituacaoEntrega.CANCELADA;
import static com.desafio.obrigacoes.model.SituacaoEntrega.CONCLUIDA;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SituacaoEntregaTest {

    @ParameterizedTest
    @CsvSource({
            "ABERTA,    ABERTA,    false",
            "ABERTA,    ATRASADA,  true",
            "ABERTA,    CONCLUIDA, true",
            "ABERTA,    CANCELADA, true",
            "ATRASADA,  ABERTA,    false",
            "ATRASADA,  ATRASADA,  false",
            "ATRASADA,  CONCLUIDA, true",
            "ATRASADA,  CANCELADA, true",
            "CONCLUIDA, ABERTA,    false",
            "CONCLUIDA, ATRASADA,  false",
            "CONCLUIDA, CONCLUIDA, false",
            "CONCLUIDA, CANCELADA, false",
            "CANCELADA, ABERTA,    false",
            "CANCELADA, ATRASADA,  false",
            "CANCELADA, CONCLUIDA, false",
            "CANCELADA, CANCELADA, false"
    })
    void podeIrPara(SituacaoEntrega origem, SituacaoEntrega destino, boolean esperado) {
        assertThat(origem.podeIrPara(destino)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @CsvSource({
            "ABERTA,    false",
            "ATRASADA,  false",
            "CONCLUIDA, true",
            "CANCELADA, true"
    })
    void identificaSituacoesFinais(SituacaoEntrega situacao, boolean esperado) {
        assertThat(situacao.isFinal()).isEqualTo(esperado);
    }

    @Test
    void situacoesFinaisNaoTemSaida() {
        for (SituacaoEntrega origem : SituacaoEntrega.values()) {
            if (!origem.isFinal()) {
                continue;
            }
            for (SituacaoEntrega destino : SituacaoEntrega.values()) {
                assertThat(origem.podeIrPara(destino)).as("%s -> %s", origem, destino).isFalse();
            }
        }
    }

    @Test
    void naoTransicionaParaNulo() {
        assertThat(ABERTA.podeIrPara(null)).isFalse();
    }

    @Test
    void declaraSituacoesNaOrdemDoFluxo() {
        assertThat(SituacaoEntrega.values()).containsExactly(ABERTA, ATRASADA, CONCLUIDA, CANCELADA);
    }
}
